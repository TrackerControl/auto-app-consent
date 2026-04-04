package net.kollnig.consent.standards;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Adds Global Privacy Control (GPC) headers to HTTP requests.
 *
 * GPC is a browser/HTTP signal (Sec-GPC: 1) that tells websites the user
 * does not want their personal data sold or shared. It is recognized under
 * CCPA, GDPR, and the Colorado Privacy Act.
 *
 * This class provides utilities to apply the GPC header to:
 * - HttpURLConnection (standard Android HTTP)
 * - Any request via the header name/value constants
 *
 * For OkHttp users, use GpcOkHttpInterceptor instead.
 *
 * See: https://globalprivacycontrol.github.io/gpc-spec/
 */
public class GpcInterceptor {

    public static final String GPC_HEADER_NAME = "Sec-GPC";
    public static final String GPC_HEADER_VALUE = "1";

    private static volatile boolean enabled = false;

    /**
     * Enable or disable GPC header injection globally.
     */
    public static void setEnabled(boolean gpcEnabled) {
        enabled = gpcEnabled;
    }

    /**
     * Returns whether GPC is currently enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Apply the GPC header to an HttpURLConnection if GPC is enabled.
     *
     * Call this before connecting:
     *   GpcInterceptor.applyTo(connection);
     *   connection.connect();
     */
    public static void applyTo(HttpURLConnection connection) {
        if (enabled) {
            connection.setRequestProperty(GPC_HEADER_NAME, GPC_HEADER_VALUE);
        }
    }
}
