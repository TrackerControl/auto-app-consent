package net.kollnig.consent.standards;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.lang.reflect.Constructor;

/**
 * Injects Sec-GPC: 1 into HttpURLConnection requests using Java's
 * built-in URLStreamHandlerFactory API.
 *
 * Coverage note: This only affects connections created via
 * URL.openConnection(). Most modern ad SDKs use OkHttp or Cronet
 * internally (which bypass this), so those are covered by the
 * build-time bytecode transform plugin instead.
 *
 * Limitation: URL.setURLStreamHandlerFactory() can only be called ONCE
 * per JVM. If another library has already set it, we log a warning.
 */
public class GpcUrlHandler {

    private static final String TAG = "GpcUrlHandler";
    private static boolean installed = false;
    private static URLStreamHandler defaultHttpHandler;
    private static URLStreamHandler defaultHttpsHandler;

    /**
     * Install the GPC-aware URLStreamHandlerFactory.
     * Safe to call multiple times — only installs once.
     */
    public static synchronized void install() {
        if (installed) return;

        // Resolve the platform's default handlers BEFORE setting our factory.
        // On Android, these are com.android.okhttp.HttpHandler and HttpsHandler.
        defaultHttpHandler = getDefaultHandler("http");
        defaultHttpsHandler = getDefaultHandler("https");

        if (defaultHttpHandler == null || defaultHttpsHandler == null) {
            Log.w(TAG, "Could not resolve default HTTP handlers, skipping GPC factory");
            return;
        }

        try {
            URL.setURLStreamHandlerFactory(new GpcStreamHandlerFactory());
            installed = true;
            Log.d(TAG, "GPC URLStreamHandlerFactory installed");
        } catch (Error e) {
            Log.w(TAG, "Could not install GPC URLStreamHandlerFactory "
                    + "(another factory already set): " + e.getMessage());
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    /**
     * Get the platform's default URLStreamHandler for a protocol by creating
     * a URL before our factory is installed. The handler is cached in the URL
     * class and we can extract it.
     */
    private static URLStreamHandler getDefaultHandler(String protocol) {
        try {
            // Creating a URL triggers handler resolution via the default mechanism.
            // We do this BEFORE setting our factory, so it returns the platform handler.
            URL testUrl = new URL(protocol + "://example.com");
            // Use reflection to read the handler field from the URL
            java.lang.reflect.Field handlerField = URL.class.getDeclaredField("handler");
            handlerField.setAccessible(true);
            return (URLStreamHandler) handlerField.get(testUrl);
        } catch (Exception e) {
            Log.w(TAG, "Could not get default " + protocol + " handler: " + e.getMessage());
            return null;
        }
    }

    private static class GpcStreamHandlerFactory implements URLStreamHandlerFactory {
        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("http".equals(protocol)) {
                return new GpcStreamHandler(defaultHttpHandler);
            }
            if ("https".equals(protocol)) {
                return new GpcStreamHandler(defaultHttpsHandler);
            }
            return null;
        }
    }

    /**
     * URLStreamHandler that delegates to the platform's default handler
     * and adds the GPC header to HttpURLConnection results.
     */
    private static class GpcStreamHandler extends URLStreamHandler {
        private final URLStreamHandler delegate;

        GpcStreamHandler(URLStreamHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            // Delegate to the platform's default handler
            try {
                java.lang.reflect.Method openConn = URLStreamHandler.class
                        .getDeclaredMethod("openConnection", URL.class);
                openConn.setAccessible(true);
                URLConnection conn = (URLConnection) openConn.invoke(delegate, url);
                return addGpcHeader(conn);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("GPC handler delegation failed", e);
            }
        }

        @Override
        protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
            try {
                java.lang.reflect.Method openConn = URLStreamHandler.class
                        .getDeclaredMethod("openConnection", URL.class, Proxy.class);
                openConn.setAccessible(true);
                URLConnection conn = (URLConnection) openConn.invoke(delegate, url, proxy);
                return addGpcHeader(conn);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("GPC handler delegation failed", e);
            }
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
