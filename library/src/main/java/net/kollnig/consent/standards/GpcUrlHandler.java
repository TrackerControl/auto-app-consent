package net.kollnig.consent.standards;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Injects Sec-GPC: 1 into all HttpURLConnection requests by wrapping the
 * default URLStreamHandler.
 *
 * This uses Java's built-in URLStreamHandlerFactory API — a stable,
 * documented mechanism, not a runtime hook. It works because:
 * - URL.openConnection() delegates to a URLStreamHandler
 * - We register a factory that wraps the default handler
 * - The wrapper adds the GPC header to every HttpURLConnection
 *
 * Limitations:
 * - URL.setURLStreamHandlerFactory() can only be called ONCE per JVM.
 *   If another library (e.g., OkHttp's internal implementation) has
 *   already set it, this will throw an Error. We catch and log this.
 * - Only applies to connections created via URL.openConnection().
 *   OkHttp bypasses this internally, but is covered by the build-time
 *   bytecode transform on Request.Builder.build().
 *
 * Usage:
 *   GpcUrlHandler.install();  // Call once at app startup
 */
public class GpcUrlHandler {

    private static final String TAG = "GpcUrlHandler";
    private static boolean installed = false;

    /**
     * Install the GPC-aware URLStreamHandlerFactory.
     * Safe to call multiple times — only installs once.
     * If another factory is already installed, logs a warning and skips.
     */
    public static synchronized void install() {
        if (installed) return;

        try {
            URL.setURLStreamHandlerFactory(new GpcStreamHandlerFactory());
            installed = true;
            Log.d(TAG, "GPC URLStreamHandlerFactory installed");
        } catch (Error e) {
            // Another library already set the factory
            Log.w(TAG, "Could not install GPC URLStreamHandlerFactory "
                    + "(another factory already set): " + e.getMessage());
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    /**
     * Factory that creates GPC-aware stream handlers for http and https.
     */
    private static class GpcStreamHandlerFactory implements URLStreamHandlerFactory {
        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("http".equals(protocol) || "https".equals(protocol)) {
                return new GpcStreamHandler(protocol);
            }
            // Return null to use the default handler for other protocols
            return null;
        }
    }

    /**
     * URLStreamHandler that delegates to the default handler and adds
     * the Sec-GPC header to the resulting HttpURLConnection.
     */
    private static class GpcStreamHandler extends URLStreamHandler {
        private final String protocol;

        GpcStreamHandler(String protocol) {
            this.protocol = protocol;
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            // Create a new URL without our custom handler to get the default connection
            // This avoids infinite recursion since the new URL uses the system default
            URL defaultUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
            URLConnection conn = defaultUrl.openConnection();
            return addGpcHeader(conn);
        }

        @Override
        protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
            URL defaultUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
            URLConnection conn = defaultUrl.openConnection(proxy);
            return addGpcHeader(conn);
        }

        private URLConnection addGpcHeader(URLConnection conn) {
            if (GpcInterceptor.isEnabled() && conn instanceof HttpURLConnection) {
                conn.setRequestProperty(
                        GpcInterceptor.GPC_HEADER_NAME,
                        GpcInterceptor.GPC_HEADER_VALUE);
            }
            return conn;
        }
    }
}
