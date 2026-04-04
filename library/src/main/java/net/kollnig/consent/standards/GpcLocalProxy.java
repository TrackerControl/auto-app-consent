package net.kollnig.consent.standards;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight local HTTP proxy that injects the Sec-GPC: 1 header into
 * outgoing HTTP requests.
 *
 * This is the recommended production approach for universal GPC header
 * injection. It works by:
 *
 * 1. Running a local HTTP proxy on 127.0.0.1 (ephemeral port)
 * 2. Setting the JVM-wide proxy via system properties so ALL HTTP clients
 *    (HttpURLConnection, OkHttp, Apache HttpClient, etc.) route through it
 * 3. The proxy injects "Sec-GPC: 1" into every outgoing request
 *
 * Advantages over YAHFA hooking:
 * - Works on ALL Android versions (no ART dependency)
 * - No method hooking = no crashes from JIT/inlining/OEM changes
 * - No Google Play Protect flags
 * - Catches all HTTP stacks (not just specific classes)
 *
 * Limitations:
 * - Only works for HTTP. For HTTPS (majority of traffic), the proxy uses
 *   CONNECT tunneling — it cannot inject headers into encrypted streams.
 *   For HTTPS, the TCF/US Privacy SharedPreferences approach is more
 *   effective since SDKs read consent signals locally before making requests.
 * - Apps that explicitly set their own proxy settings will bypass this.
 *
 * For full HTTPS header injection, consider integrating with a VPN-based
 * solution like TrackerControl or NetGuard, which can perform TLS
 * interception with a user-installed CA certificate.
 */
public class GpcLocalProxy {

    private static final String TAG = "GpcLocalProxy";
    private static final String GPC_HEADER = "Sec-GPC: 1\r\n";

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    private int localPort = -1;

    private String previousHttpHost;
    private String previousHttpPort;
    private String previousHttpsHost;
    private String previousHttpsPort;

    /**
     * Start the local GPC proxy and configure JVM-wide proxy settings.
     */
    public synchronized void start() throws IOException {
        if (running) return;

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        localPort = serverSocket.getLocalPort();

        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "GpcProxy");
            t.setDaemon(true);
            return t;
        });

        running = true;

        // Accept connections in background
        executor.submit(() -> {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    executor.submit(() -> handleConnection(client));
                } catch (IOException e) {
                    if (running) {
                        Log.w(TAG, "Accept error: " + e.getMessage());
                    }
                }
            }
        });

        // Save previous proxy settings and set ours
        previousHttpHost = System.getProperty("http.proxyHost");
        previousHttpPort = System.getProperty("http.proxyPort");
        previousHttpsHost = System.getProperty("https.proxyHost");
        previousHttpsPort = System.getProperty("https.proxyPort");

        // Only proxy HTTP — HTTPS uses CONNECT tunneling which can't inject headers
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", String.valueOf(localPort));

        Log.d(TAG, "GPC proxy started on port " + localPort);
    }

    /**
     * Stop the proxy and restore previous proxy settings.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;

        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }

        executor.shutdownNow();

        // Restore previous proxy settings
        restoreProperty("http.proxyHost", previousHttpHost);
        restoreProperty("http.proxyPort", previousHttpPort);
        restoreProperty("https.proxyHost", previousHttpsHost);
        restoreProperty("https.proxyPort", previousHttpsPort);

        Log.d(TAG, "GPC proxy stopped");
    }

    public boolean isRunning() {
        return running;
    }

    public int getLocalPort() {
        return localPort;
    }

    private void handleConnection(Socket client) {
        try (Socket clientSocket = client) {
            clientSocket.setSoTimeout(30_000);
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            // Read the request line and headers
            StringBuilder headerBuilder = new StringBuilder();
            int prev = 0, curr;
            boolean headersDone = false;
            while ((curr = in.read()) != -1) {
                headerBuilder.append((char) curr);
                // Detect \r\n\r\n (end of headers)
                if (curr == '\n' && prev == '\n') {
                    headersDone = true;
                    break;
                }
                if (curr == '\n' && headerBuilder.length() >= 2) {
                    char beforeCr = headerBuilder.charAt(headerBuilder.length() - 2);
                    if (beforeCr == '\r') {
                        // Check for \r\n\r\n
                        String built = headerBuilder.toString();
                        if (built.endsWith("\r\n\r\n")) {
                            headersDone = true;
                            break;
                        }
                    }
                }
                prev = curr;
            }

            if (!headersDone) {
                clientSocket.close();
                return;
            }

            String headers = headerBuilder.toString();
            String requestLine = headers.substring(0, headers.indexOf("\r\n"));

            // For CONNECT (HTTPS), just tunnel — we can't inject headers
            if (requestLine.startsWith("CONNECT")) {
                handleConnect(requestLine, clientSocket, in, out);
                return;
            }

            // For HTTP requests, inject GPC header and forward
            String modifiedHeaders = injectGpcHeader(headers);
            forwardHttpRequest(requestLine, modifiedHeaders, clientSocket, in, out);

        } catch (IOException e) {
            Log.w(TAG, "Connection handling error: " + e.getMessage());
        }
    }

    private String injectGpcHeader(String headers) {
        // Insert Sec-GPC header before the final \r\n\r\n
        int endOfHeaders = headers.lastIndexOf("\r\n\r\n");
        if (endOfHeaders >= 0) {
            return headers.substring(0, endOfHeaders + 2) + GPC_HEADER + "\r\n";
        }
        return headers;
    }

    private void forwardHttpRequest(String requestLine, String headers,
                                    Socket clientSocket, InputStream clientIn,
                                    OutputStream clientOut) throws IOException {
        // Parse host and port from request
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) return;

        String url = parts[1];
        String host;
        int port = 80;

        // Extract host from absolute URL (http://host:port/path)
        if (url.startsWith("http://")) {
            String afterScheme = url.substring(7);
            int slashIdx = afterScheme.indexOf('/');
            String hostPort = slashIdx >= 0 ? afterScheme.substring(0, slashIdx) : afterScheme;
            if (hostPort.contains(":")) {
                host = hostPort.substring(0, hostPort.indexOf(':'));
                port = Integer.parseInt(hostPort.substring(hostPort.indexOf(':') + 1));
            } else {
                host = hostPort;
            }
        } else {
            // Relative URL — extract from Host header
            host = extractHostHeader(headers);
            if (host == null) return;
            if (host.contains(":")) {
                port = Integer.parseInt(host.substring(host.indexOf(':') + 1));
                host = host.substring(0, host.indexOf(':'));
            }
        }

        try (Socket server = new Socket(host, port)) {
            server.setSoTimeout(30_000);
            OutputStream serverOut = server.getOutputStream();
            InputStream serverIn = server.getInputStream();

            // Send modified headers to server
            serverOut.write(headers.getBytes());
            serverOut.flush();

            // Bidirectional streaming
            Thread toServer = new Thread(() -> {
                try {
                    transfer(clientIn, serverOut);
                } catch (IOException ignored) {
                }
            });
            toServer.setDaemon(true);
            toServer.start();

            transfer(serverIn, clientOut);
        }
    }

    private void handleConnect(String requestLine, Socket clientSocket,
                               InputStream clientIn, OutputStream clientOut) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return;

        String hostPort = parts[1];
        String host;
        int port = 443;

        if (hostPort.contains(":")) {
            host = hostPort.substring(0, hostPort.indexOf(':'));
            port = Integer.parseInt(hostPort.substring(hostPort.indexOf(':') + 1));
        } else {
            host = hostPort;
        }

        try (Socket server = new Socket(host, port)) {
            server.setSoTimeout(30_000);

            // Send 200 Connection Established
            clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
            clientOut.flush();

            // Bidirectional tunnel
            Thread toServer = new Thread(() -> {
                try {
                    transfer(clientIn, server.getOutputStream());
                } catch (IOException ignored) {
                }
            });
            toServer.setDaemon(true);
            toServer.start();

            transfer(server.getInputStream(), clientOut);
        }
    }

    private String extractHostHeader(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("host:")) {
                return line.substring(5).trim();
            }
        }
        return null;
    }

    private void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
            out.flush();
        }
    }

    private void restoreProperty(String key, String previousValue) {
        if (previousValue != null) {
            System.setProperty(key, previousValue);
        } else {
            System.clearProperty(key);
        }
    }
}
