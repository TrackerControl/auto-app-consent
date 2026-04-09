package net.kollnig.consent.standards;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages IAB US Privacy String (CCPA) signals.
 *
 * The US Privacy String is a 4-character string stored in SharedPreferences
 * under the key "IABUSPrivacy_String". Many ad SDKs read this natively.
 *
 * Format: [Version][Notice][OptOut][LSPA]
 * - Version: 1 (current)
 * - Notice: Y = notice given, N = not given, - = N/A
 * - OptOut: Y = opted out of sale, N = not opted out, - = N/A
 * - LSPA:   Y = LSPA covered, N = not covered, - = N/A
 *
 * See: https://github.com/InteractiveAdvertisingBureau/USPrivacy
 */
public class UsPrivacyManager {

    private static final String IAB_US_PRIVACY_STRING = "IABUSPrivacy_String";
    private static final int VERSION = 1;

    private final Context context;

    public UsPrivacyManager(Context context) {
        this.context = context;
    }

    private SharedPreferences getDefaultPreferences() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Write the US Privacy (CCPA) string.
     *
     * @param ccpaApplies whether CCPA applies to this user
     * @param consent     whether the user has NOT opted out of sale (true = allow sale)
     */
    public void writeConsentSignal(boolean ccpaApplies, boolean consent) {
        String privacyString;
        if (!ccpaApplies) {
            // CCPA does not apply
            privacyString = VERSION + "---";
        } else {
            // Notice given, opt-out status based on consent, LSPA not covered
            char optOut = consent ? 'N' : 'Y';
            privacyString = VERSION + "Y" + optOut + "N";
        }

        getDefaultPreferences().edit()
                .putString(IAB_US_PRIVACY_STRING, privacyString)
                .apply();
    }

    /**
     * Clear the US Privacy string.
     */
    public void clearConsentSignal() {
        getDefaultPreferences().edit()
                .remove(IAB_US_PRIVACY_STRING)
                .apply();
    }

    /**
     * Read the current US Privacy string.
     */
    public String getPrivacyString() {
        return getDefaultPreferences().getString(IAB_US_PRIVACY_STRING, null);
    }

    /**
     * Check if user has opted out of sale under CCPA.
     */
    public boolean hasOptedOutOfSale() {
        String privacyString = getPrivacyString();
        return privacyString != null && privacyString.length() >= 3
                && privacyString.charAt(2) == 'Y';
    }
}
