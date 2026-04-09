package net.kollnig.consent.standards;

import java.net.HttpURLConnection;

/**
 * Global Privacy Control (GPC) signal management.
 *
 * GPC is a web standard (Sec-GPC: 1 header + navigator.globalPrivacyControl)
 * that tells websites the user does not want their personal data sold or shared.
 * It is legally recognized under CCPA, GDPR, and the Colorado Privacy Act.
 *
 * IMPORTANT: GPC is a *browser/web* standard. It is designed for:
 * - WebViews loading web content (use GpcWebViewClient)
 * - The app's own HTTP requests to its backend (use applyTo())
 *
 * GPC is NOT designed for in-app SDK traffic. Ad/analytics SDKs use their own
 * consent mechanisms:
 * - IAB TCF v2.2 strings in SharedPreferences (see TcfConsentManager)
 * - IAB US Privacy strings in SharedPreferences (see UsPrivacyManager)
 * - SDK-specific consent APIs (what the Library classes handle)
 *
 * Injecting GPC headers into SDK HTTPS traffic is not feasible because:
 * - Most SDKs use HTTPS with certificate pinning
 * - No proxy/MITM approach can modify pinned HTTPS headers
 * - ART method hooking (YAHFA) is fragile and Android-version-limited
 *
 * See: https://globalprivacycontrol.github.io/gpc-spec/
 */
public class GpcInterceptor {

    public static final String GPC_HEADER_NAME = "Sec-GPC";
    public static final String GPC_HEADER_VALUE = "1";

    private static volatile boolean enabled = false;

    /**
     * Enable or disable GPC globally.
     * When enabled, GpcWebViewClient will inject GPC signals into WebViews,
     * and applyTo() will add the header to HttpURLConnections.
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
     * Apply the GPC header to an HttpURLConnection.
     * Use this for the app's own HTTP requests to its backend server.
     *
     * Example:
     *   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
     *   GpcInterceptor.applyTo(conn);
     *   conn.connect();
     */
    public static void applyTo(HttpURLConnection connection) {
        if (enabled) {
            connection.setRequestProperty(GPC_HEADER_NAME, GPC_HEADER_VALUE);
        }
    }
}
