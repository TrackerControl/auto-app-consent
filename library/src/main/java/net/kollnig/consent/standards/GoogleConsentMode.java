package net.kollnig.consent.standards;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Google Consent Mode v2 integration.
 *
 * Sets consent signals that Google Ads and Firebase Analytics read to decide
 * how to behave when consent is denied. Required for EU ad serving since
 * March 2024.
 *
 * Consent types:
 * - ANALYTICS_STORAGE: Enables storage for analytics (e.g. cookies, app identifiers)
 * - AD_STORAGE: Enables storage for advertising
 * - AD_USER_DATA: Consent to send user data to Google for advertising
 * - AD_PERSONALIZATION: Consent to use data for personalized advertising
 *
 * When denied, Google SDKs fall back to "cookieless pings" and modeled
 * conversions instead of refusing to work entirely.
 *
 * Uses reflection to call FirebaseAnalytics.setConsent() so there's no
 * compile-time dependency on Firebase.
 */
public class GoogleConsentMode {

    private static final String TAG = "GoogleConsentMode";

    // Consent type enum values (from com.google.firebase.analytics.FirebaseAnalytics.ConsentType)
    private static final String CONSENT_TYPE_CLASS =
            "com.google.firebase.analytics.FirebaseAnalytics$ConsentType";
    private static final String CONSENT_STATUS_CLASS =
            "com.google.firebase.analytics.FirebaseAnalytics$ConsentStatus";

    /**
     * Set Google Consent Mode v2 signals based on per-purpose consent.
     *
     * @param context           Android context
     * @param analyticsConsent  Whether analytics purpose was consented to
     * @param adsConsent        Whether advertising purpose was consented to
     */
    public static void setConsent(Context context,
                                  boolean analyticsConsent,
                                  boolean adsConsent) {
        try {
            // Load the enum classes
            Class<?> consentTypeClass = Class.forName(CONSENT_TYPE_CLASS);
            Class<?> consentStatusClass = Class.forName(CONSENT_STATUS_CLASS);

            // Get enum values
            Object analyticsStorage = getEnumValue(consentTypeClass, "ANALYTICS_STORAGE");
            Object adStorage = getEnumValue(consentTypeClass, "AD_STORAGE");
            Object adUserData = getEnumValue(consentTypeClass, "AD_USER_DATA");
            Object adPersonalization = getEnumValue(consentTypeClass, "AD_PERSONALIZATION");

            Object granted = getEnumValue(consentStatusClass, "GRANTED");
            Object denied = getEnumValue(consentStatusClass, "DENIED");

            if (analyticsStorage == null || adStorage == null ||
                    adUserData == null || adPersonalization == null ||
                    granted == null || denied == null) {
                Log.w(TAG, "Could not resolve Consent Mode enum values "
                        + "(Firebase Analytics may not support Consent Mode v2)");
                return;
            }

            // Build the consent map
            @SuppressWarnings({"unchecked", "rawtypes"})
            Map consentMap = new HashMap();
            consentMap.put(analyticsStorage, analyticsConsent ? granted : denied);
            consentMap.put(adStorage, adsConsent ? granted : denied);
            consentMap.put(adUserData, adsConsent ? granted : denied);
            consentMap.put(adPersonalization, adsConsent ? granted : denied);

            // Call FirebaseAnalytics.getInstance(context).setConsent(consentMap)
            Class<?> firebaseClass = Class.forName(
                    "com.google.firebase.analytics.FirebaseAnalytics");
            Method getInstance = firebaseClass.getMethod("getInstance", Context.class);
            Object analytics = getInstance.invoke(null, context);

            Method setConsent = firebaseClass.getMethod("setConsent", Map.class);
            setConsent.invoke(analytics, consentMap);

            Log.d(TAG, "Google Consent Mode v2 set: analytics="
                    + analyticsConsent + ", ads=" + adsConsent);

        } catch (ClassNotFoundException e) {
            // Firebase Analytics not present — that's fine
            Log.d(TAG, "Firebase Analytics not present, skipping Consent Mode v2");
        } catch (Exception e) {
            Log.w(TAG, "Failed to set Google Consent Mode v2: " + e.getMessage());
        }
    }

    /**
     * Check if Google Consent Mode v2 is available (Firebase Analytics present
     * and has the ConsentType enum).
     */
    public static boolean isAvailable() {
        try {
            Class.forName(CONSENT_TYPE_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object getEnumValue(Class<?> enumClass, String name) {
        try {
            Method valueOf = enumClass.getMethod("valueOf", String.class);
            return valueOf.invoke(null, name);
        } catch (Exception e) {
            return null;
        }
    }
}
