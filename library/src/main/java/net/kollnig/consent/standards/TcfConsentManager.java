package net.kollnig.consent.standards;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages IAB Transparency & Consent Framework (TCF) v2.2 consent signals.
 *
 * Most major ad SDKs (Google Ads, AppLovin, InMobi, ironSource, AdColony, Vungle, etc.)
 * natively read TCF consent from SharedPreferences using the IABTCF_ prefix keys.
 * By writing these keys, we can signal consent/denial without invasive method hooking.
 *
 * See: https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework
 */
public class TcfConsentManager {

    // IAB TCF v2.2 SharedPreferences keys
    private static final String IABTCF_CMP_SDK_ID = "IABTCF_CmpSdkID";
    private static final String IABTCF_CMP_SDK_VERSION = "IABTCF_CmpSdkVersion";
    private static final String IABTCF_POLICY_VERSION = "IABTCF_PolicyVersion";
    private static final String IABTCF_GDPR_APPLIES = "IABTCF_gdprApplies";
    private static final String IABTCF_PUBLISHER_CC = "IABTCF_PublisherCC";
    private static final String IABTCF_PURPOSE_ONE_TREATMENT = "IABTCF_PurposeOneTreatment";
    private static final String IABTCF_USE_NON_STANDARD_TEXTS = "IABTCF_UseNonStandardTexts";
    private static final String IABTCF_TC_STRING = "IABTCF_TCString";
    private static final String IABTCF_VENDOR_CONSENTS = "IABTCF_VendorConsents";
    private static final String IABTCF_VENDOR_LEGITIMATE_INTERESTS = "IABTCF_VendorLegitimateInterests";
    private static final String IABTCF_PURPOSE_CONSENTS = "IABTCF_PurposeConsents";
    private static final String IABTCF_PURPOSE_LEGITIMATE_INTERESTS = "IABTCF_PurposeLegitimateInterests";
    private static final String IABTCF_SPECIAL_FEATURES_OPT_INS = "IABTCF_SpecialFeaturesOptIns";
    private static final String IABTCF_PUBLISHER_CONSENT = "IABTCF_PublisherConsent";
    private static final String IABTCF_PUBLISHER_LEGITIMATE_INTERESTS = "IABTCF_PublisherLegitimateInterests";
    private static final String IABTCF_PUBLISHER_CUSTOM_PURPOSES_CONSENTS = "IABTCF_PublisherCustomPurposesConsents";
    private static final String IABTCF_PUBLISHER_CUSTOM_PURPOSES_LEGITIMATE_INTERESTS = "IABTCF_PublisherCustomPurposesLegitimateInterests";
    private static final String IABTCF_PUBLISHER_RESTRICTIONS = "IABTCF_PublisherRestrictions";

    // TCF v2.2 Purpose IDs (1-11)
    // 1: Store and/or access information on a device
    // 2: Select basic ads
    // 3: Create a personalised ads profile
    // 4: Select personalised ads
    // 5: Create a personalised content profile
    // 6: Select personalised content
    // 7: Measure ad performance
    // 8: Measure content performance
    // 9: Apply market research to generate audience insights
    // 10: Develop and improve products
    // 11: Use limited data to select content
    public static final int PURPOSE_COUNT = 11;

    // TCF v2.2 Special Feature IDs (1-2)
    // 1: Use precise geolocation data
    // 2: Actively scan device characteristics for identification
    public static final int SPECIAL_FEATURE_COUNT = 2;

    // CMP SDK ID — registered CMPs get an ID from IAB; 0 = not registered
    // Apps using this library should register with IAB to get a real CMP ID.
    private final int cmpSdkId;
    private final int cmpSdkVersion;
    private final String publisherCountryCode;
    private final String consentLanguage;

    private final Context context;

    public TcfConsentManager(Context context, int cmpSdkId, int cmpSdkVersion,
                             String publisherCountryCode) {
        this(context, cmpSdkId, cmpSdkVersion, publisherCountryCode, "EN");
    }

    public TcfConsentManager(Context context, int cmpSdkId, int cmpSdkVersion,
                             String publisherCountryCode, String consentLanguage) {
        this.context = context;
        this.cmpSdkId = cmpSdkId;
        this.cmpSdkVersion = cmpSdkVersion;
        this.publisherCountryCode = publisherCountryCode;
        this.consentLanguage = consentLanguage;
    }

    /**
     * Returns the default SharedPreferences used by the IAB TCF spec.
     * Per the spec, TCF keys MUST be stored in the app's default SharedPreferences.
     */
    private SharedPreferences getDefaultPreferences() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Write TCF v2.2 consent signals based on a global consent/deny decision.
     *
     * @param gdprApplies whether GDPR applies to this user
     * @param consent     whether the user has given consent
     */
    public void writeConsentSignals(boolean gdprApplies, boolean consent) {
        SharedPreferences.Editor editor = getDefaultPreferences().edit();

        // CMP identification
        editor.putInt(IABTCF_CMP_SDK_ID, cmpSdkId);
        editor.putInt(IABTCF_CMP_SDK_VERSION, cmpSdkVersion);
        editor.putInt(IABTCF_POLICY_VERSION, 4); // TCF v2.2 policy version

        // GDPR applicability: 0 = no, 1 = yes
        editor.putInt(IABTCF_GDPR_APPLIES, gdprApplies ? 1 : 0);

        // Publisher info
        editor.putString(IABTCF_PUBLISHER_CC, publisherCountryCode);
        editor.putInt(IABTCF_PURPOSE_ONE_TREATMENT, 0); // 0 = no special treatment
        editor.putInt(IABTCF_USE_NON_STANDARD_TEXTS, 0); // 0 = standard texts

        // Purpose consents — binary string, one bit per purpose (1-indexed)
        String purposeConsents = buildBinaryString(PURPOSE_COUNT, consent);
        editor.putString(IABTCF_PURPOSE_CONSENTS, purposeConsents);

        // Purpose legitimate interests
        editor.putString(IABTCF_PURPOSE_LEGITIMATE_INTERESTS, buildBinaryString(PURPOSE_COUNT, consent));

        // Special features opt-ins
        editor.putString(IABTCF_SPECIAL_FEATURES_OPT_INS, buildBinaryString(SPECIAL_FEATURE_COUNT, consent));

        // Vendor consents and legitimate interests — empty if no consent
        // These are populated per-vendor; for a blanket signal, set all or none
        editor.putString(IABTCF_VENDOR_CONSENTS, "");
        editor.putString(IABTCF_VENDOR_LEGITIMATE_INTERESTS, "");

        // Publisher consent/legitimate interests
        editor.putString(IABTCF_PUBLISHER_CONSENT, buildBinaryString(PURPOSE_COUNT, consent));
        editor.putString(IABTCF_PUBLISHER_LEGITIMATE_INTERESTS, buildBinaryString(PURPOSE_COUNT, consent));
        editor.putString(IABTCF_PUBLISHER_CUSTOM_PURPOSES_CONSENTS, "");
        editor.putString(IABTCF_PUBLISHER_CUSTOM_PURPOSES_LEGITIMATE_INTERESTS, "");

        // TC String — the encoded consent record that many SDKs check first
        String tcString = TcStringEncoder.encode(
                cmpSdkId, cmpSdkVersion, consentLanguage, publisherCountryCode, consent);
        editor.putString(IABTCF_TC_STRING, tcString);

        editor.apply();
    }

    /**
     * Write TCF v2.2 consent signals with per-purpose granularity.
     *
     * @param gdprApplies     whether GDPR applies
     * @param purposeConsents boolean array of length PURPOSE_COUNT (index 0 = purpose 1)
     * @param specialFeatures boolean array of length SPECIAL_FEATURE_COUNT
     */
    public void writeConsentSignals(boolean gdprApplies,
                                    boolean[] purposeConsents,
                                    boolean[] specialFeatures) {
        if (purposeConsents.length != PURPOSE_COUNT) {
            throw new IllegalArgumentException(
                    "purposeConsents must have " + PURPOSE_COUNT + " elements");
        }
        if (specialFeatures.length != SPECIAL_FEATURE_COUNT) {
            throw new IllegalArgumentException(
                    "specialFeatures must have " + SPECIAL_FEATURE_COUNT + " elements");
        }

        SharedPreferences.Editor editor = getDefaultPreferences().edit();

        editor.putInt(IABTCF_CMP_SDK_ID, cmpSdkId);
        editor.putInt(IABTCF_CMP_SDK_VERSION, cmpSdkVersion);
        editor.putInt(IABTCF_POLICY_VERSION, 4);
        editor.putInt(IABTCF_GDPR_APPLIES, gdprApplies ? 1 : 0);
        editor.putString(IABTCF_PUBLISHER_CC, publisherCountryCode);
        editor.putInt(IABTCF_PURPOSE_ONE_TREATMENT, 0);
        editor.putInt(IABTCF_USE_NON_STANDARD_TEXTS, 0);

        editor.putString(IABTCF_PURPOSE_CONSENTS, buildBinaryString(purposeConsents));
        editor.putString(IABTCF_PURPOSE_LEGITIMATE_INTERESTS, buildBinaryString(purposeConsents));
        editor.putString(IABTCF_SPECIAL_FEATURES_OPT_INS, buildBinaryString(specialFeatures));

        editor.putString(IABTCF_VENDOR_CONSENTS, "");
        editor.putString(IABTCF_VENDOR_LEGITIMATE_INTERESTS, "");
        editor.putString(IABTCF_PUBLISHER_CONSENT, buildBinaryString(purposeConsents));
        editor.putString(IABTCF_PUBLISHER_LEGITIMATE_INTERESTS, buildBinaryString(purposeConsents));
        editor.putString(IABTCF_PUBLISHER_CUSTOM_PURPOSES_CONSENTS, "");
        editor.putString(IABTCF_PUBLISHER_CUSTOM_PURPOSES_LEGITIMATE_INTERESTS, "");

        // TC String — pad arrays to 24 and 12 as required by the spec
        boolean[] purposes24 = new boolean[24];
        boolean[] specialFeatures12 = new boolean[12];
        System.arraycopy(purposeConsents, 0, purposes24, 0, purposeConsents.length);
        System.arraycopy(specialFeatures, 0, specialFeatures12, 0, specialFeatures.length);

        String tcString = TcStringEncoder.encode(
                cmpSdkId, cmpSdkVersion, consentLanguage, publisherCountryCode,
                purposes24, purposes24, specialFeatures12, 0);
        editor.putString(IABTCF_TC_STRING, tcString);

        editor.apply();
    }

    /**
     * Clear all TCF signals from SharedPreferences.
     */
    public void clearConsentSignals() {
        SharedPreferences.Editor editor = getDefaultPreferences().edit();

        editor.remove(IABTCF_CMP_SDK_ID);
        editor.remove(IABTCF_CMP_SDK_VERSION);
        editor.remove(IABTCF_POLICY_VERSION);
        editor.remove(IABTCF_GDPR_APPLIES);
        editor.remove(IABTCF_PUBLISHER_CC);
        editor.remove(IABTCF_PURPOSE_ONE_TREATMENT);
        editor.remove(IABTCF_USE_NON_STANDARD_TEXTS);
        editor.remove(IABTCF_TC_STRING);
        editor.remove(IABTCF_VENDOR_CONSENTS);
        editor.remove(IABTCF_VENDOR_LEGITIMATE_INTERESTS);
        editor.remove(IABTCF_PURPOSE_CONSENTS);
        editor.remove(IABTCF_PURPOSE_LEGITIMATE_INTERESTS);
        editor.remove(IABTCF_SPECIAL_FEATURES_OPT_INS);
        editor.remove(IABTCF_PUBLISHER_CONSENT);
        editor.remove(IABTCF_PUBLISHER_LEGITIMATE_INTERESTS);
        editor.remove(IABTCF_PUBLISHER_CUSTOM_PURPOSES_CONSENTS);
        editor.remove(IABTCF_PUBLISHER_CUSTOM_PURPOSES_LEGITIMATE_INTERESTS);
        editor.remove(IABTCF_PUBLISHER_RESTRICTIONS);

        editor.apply();
    }

    /**
     * Check if TCF signals have been written.
     */
    public boolean hasConsentSignals() {
        return getDefaultPreferences().contains(IABTCF_PURPOSE_CONSENTS);
    }

    /**
     * Read the current purpose consents as a boolean array.
     */
    public boolean[] getPurposeConsents() {
        String binary = getDefaultPreferences().getString(IABTCF_PURPOSE_CONSENTS, "");
        boolean[] result = new boolean[PURPOSE_COUNT];
        for (int i = 0; i < Math.min(binary.length(), PURPOSE_COUNT); i++) {
            result[i] = binary.charAt(i) == '1';
        }
        return result;
    }

    /**
     * Build a binary string of the given length, all set to the given value.
     * E.g. buildBinaryString(3, true) = "111", buildBinaryString(3, false) = "000"
     */
    private static String buildBinaryString(int length, boolean value) {
        char c = value ? '1' : '0';
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Build a binary string from a boolean array.
     */
    private static String buildBinaryString(boolean[] values) {
        StringBuilder sb = new StringBuilder(values.length);
        for (boolean v : values) {
            sb.append(v ? '1' : '0');
        }
        return sb.toString();
    }
}
