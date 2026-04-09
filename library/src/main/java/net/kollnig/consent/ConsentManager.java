package net.kollnig.consent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.kollnig.consent.library.AdColonyLibrary;
import net.kollnig.consent.library.AdvertisingIdLibrary;
import net.kollnig.consent.library.AppLovinLibrary;
import net.kollnig.consent.library.AppsFlyerLibrary;
import net.kollnig.consent.library.CrashlyticsLibrary;
import net.kollnig.consent.library.FacebookSdkLibrary;
import net.kollnig.consent.library.FirebaseAnalyticsLibrary;
import net.kollnig.consent.library.FlurryLibrary;
import net.kollnig.consent.library.GoogleAdsLibrary;
import net.kollnig.consent.library.InMobiLibrary;
import net.kollnig.consent.library.IronSourceLibrary;
import net.kollnig.consent.library.Library;
import net.kollnig.consent.library.LibraryInteractionException;
import net.kollnig.consent.library.VungleLibrary;
import net.kollnig.consent.purpose.ConsentPurpose;
import net.kollnig.consent.standards.GoogleConsentMode;
import net.kollnig.consent.standards.GpcInterceptor;
import net.kollnig.consent.standards.GpcUrlHandler;
import net.kollnig.consent.standards.TcfConsentManager;
import net.kollnig.consent.standards.UsPrivacyManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConsentManager {
    public static final String PREFERENCES_NAME = "net.kollnig.consent";
    static final String TAG = ConsentManager.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static ConsentManager mConsentManager = null;
    private final Uri privacyPolicy;
    private final boolean showConsent;
    private List<Library> libraries;
    private Map<String, ConsentPurpose> purposes;
    private final Context context;
    private final String[] excludedLibraries;

    // Standards support
    private TcfConsentManager tcfManager;
    private UsPrivacyManager usPrivacyManager;
    private boolean gpcEnabled;
    private boolean gdprApplies;
    private boolean ccpaApplies;
    private boolean consentModeEnabled;

    Library[] availableLibraries = {
            new FirebaseAnalyticsLibrary(),
            new CrashlyticsLibrary(),
            new FacebookSdkLibrary(),
            new AppLovinLibrary(),
            new AdvertisingIdLibrary(),
            new GoogleAdsLibrary(),
            new AppsFlyerLibrary(),
            new VungleLibrary(),
            new InMobiLibrary(),
            new FlurryLibrary(),
            new IronSourceLibrary(),
            new AdColonyLibrary()
    };

    private ConsentManager(Context context,
                           boolean showConsent,
                           Uri privacyPolicy,
                           String[] excludedLibraries,
                           boolean enableTcf,
                           int tcfCmpSdkId,
                           int tcfCmpSdkVersion,
                           String publisherCountryCode,
                           boolean enableUsPrivacy,
                           boolean enableGpc,
                           boolean enableConsentMode,
                           boolean gdprApplies,
                           boolean ccpaApplies) {

        this.context = context;
        this.showConsent = showConsent;
        this.privacyPolicy = privacyPolicy;
        this.excludedLibraries = excludedLibraries;
        this.gdprApplies = gdprApplies;
        this.ccpaApplies = ccpaApplies;
        this.gpcEnabled = enableGpc;
        this.consentModeEnabled = enableConsentMode;

        if (enableTcf) {
            this.tcfManager = new TcfConsentManager(
                    context, tcfCmpSdkId, tcfCmpSdkVersion, publisherCountryCode);
        }
        if (enableUsPrivacy) {
            this.usPrivacyManager = new UsPrivacyManager(context);
        }
        if (enableGpc) {
            GpcInterceptor.setEnabled(true);
            GpcUrlHandler.install();
        }
    }

    private static ConsentManager getInstance(Context context,
                                              Boolean showConsent,
                                              Uri privacyPolicy,
                                              String[] excludeLibraries,
                                              Library[] customLibraries,
                                              boolean enableTcf,
                                              int tcfCmpSdkId,
                                              int tcfCmpSdkVersion,
                                              String publisherCountryCode,
                                              boolean enableUsPrivacy,
                                              boolean enableGpc,
                                              boolean enableConsentMode,
                                              boolean gdprApplies,
                                              boolean ccpaApplies) {
        if (mConsentManager == null) {
            mConsentManager = new ConsentManager(
                    context, showConsent, privacyPolicy, excludeLibraries,
                    enableTcf, tcfCmpSdkId, tcfCmpSdkVersion, publisherCountryCode,
                    enableUsPrivacy, enableGpc, enableConsentMode, gdprApplies, ccpaApplies);

            mConsentManager.libraries = new LinkedList<>();
            try {
                List<Library> allLibraries = new LinkedList<>(Arrays.asList(mConsentManager.availableLibraries));
                allLibraries.addAll(Arrays.asList(customLibraries));

                for (Library library : allLibraries) {
                    if (!library.isPresent() ||
                            Arrays.asList(mConsentManager.excludedLibraries).contains(library.getId()))
                        continue;

                    library.initialise(context);
                    mConsentManager.libraries.add(library);
                }
            } catch (LibraryInteractionException e) {
                e.printStackTrace();
            }

            // Build purpose map, filtering to only purposes that have present libraries
            mConsentManager.purposes = ConsentPurpose.getDefaults();

            // Write initial deny signals
            mConsentManager.updateStandardsSignals(false);
            if (mConsentManager.consentModeEnabled) {
                GoogleConsentMode.setConsent(context, false, false);
            }

            mConsentManager.askConsent();
        }

        return mConsentManager;
    }

    public static ConsentManager getInstance() {
        if (mConsentManager == null)
            throw new RuntimeException("ConsentManager has not yet been correctly initialised.");
        return mConsentManager;
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    // ---- Per-library consent (used by build-time transforms) ----

    public @Nullable Boolean hasConsent(String libraryId) {
        SharedPreferences prefs = getPreferences(context);
        Set<String> set = prefs.getStringSet("consents", new HashSet<>());
        if (set.contains(libraryId + ":" + true))
            return true;
        else if (set.contains(libraryId + ":" + false))
            return false;
        else
            return null;
    }

    public void saveConsent(String libraryId, boolean consent) {
        SharedPreferences prefs = getPreferences(context);
        Set<String> set = prefs.getStringSet("consents", null);
        Set<String> prefsSet = new HashSet<>();
        if (set != null) prefsSet.addAll(set);

        for (Library library : libraries) {
            if (!library.getId().equals(libraryId)) continue;
            try {
                library.passConsentToLibrary(consent);
                prefsSet.remove(library.getId() + ":" + true);
                prefsSet.remove(library.getId() + ":" + false);
                prefsSet.add(library.getId() + ":" + consent);
            } catch (LibraryInteractionException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putStringSet("consents", prefsSet).apply();
    }

    // ---- Purpose-based consent ----

    /**
     * Save consent for an entire purpose. This sets consent for all SDKs
     * in that purpose and updates all standards signals.
     */
    public void savePurposeConsent(String purposeId, boolean consent) {
        ConsentPurpose purpose = purposes.get(purposeId);
        if (purpose == null) return;

        // Save consent for each library in this purpose
        for (String libraryId : purpose.libraryIds) {
            saveConsent(libraryId, consent);
        }

        // Save purpose-level consent
        SharedPreferences prefs = getPreferences(context);
        Set<String> purposeSet = prefs.getStringSet("purpose_consents", null);
        Set<String> prefsSet = new HashSet<>();
        if (purposeSet != null) prefsSet.addAll(purposeSet);
        prefsSet.remove(purposeId + ":true");
        prefsSet.remove(purposeId + ":false");
        prefsSet.add(purposeId + ":" + consent);
        prefs.edit().putStringSet("purpose_consents", prefsSet).apply();

        // Update all standards signals
        updateAllSignals();
    }

    /**
     * Check if a purpose has been consented to.
     */
    public @Nullable Boolean hasPurposeConsent(String purposeId) {
        SharedPreferences prefs = getPreferences(context);
        Set<String> set = prefs.getStringSet("purpose_consents", new HashSet<>());
        if (set.contains(purposeId + ":true")) return true;
        else if (set.contains(purposeId + ":false")) return false;
        else return null;
    }

    /**
     * Get the purpose definitions.
     */
    public Map<String, ConsentPurpose> getPurposes() {
        return purposes;
    }

    public String[] getManagedLibraries() {
        String[] libraryIds = new String[libraries.size()];
        for (int i = 0; i < libraries.size(); i++) {
            libraryIds[i] = libraries.get(i).getId();
        }
        return libraryIds;
    }

    public void clearConsent() {
        getPreferences(context).edit().clear().apply();
        if (tcfManager != null) tcfManager.clearConsentSignals();
        if (usPrivacyManager != null) usPrivacyManager.clearConsentSignal();
        GpcInterceptor.setEnabled(gpcEnabled);
    }

    // ---- Standards signals ----

    private void updateAllSignals() {
        boolean analyticsConsent = Boolean.TRUE.equals(
                hasPurposeConsent(ConsentPurpose.PURPOSE_ANALYTICS));
        boolean adsConsent = Boolean.TRUE.equals(
                hasPurposeConsent(ConsentPurpose.PURPOSE_ADVERTISING));
        boolean anyConsent = analyticsConsent || adsConsent
                || Boolean.TRUE.equals(hasPurposeConsent(ConsentPurpose.PURPOSE_CRASH_REPORTING))
                || Boolean.TRUE.equals(hasPurposeConsent(ConsentPurpose.PURPOSE_SOCIAL));

        // TCF: build per-purpose consent array from purpose decisions
        if (tcfManager != null) {
            boolean[] tcfPurposes = new boolean[TcfConsentManager.PURPOSE_COUNT];
            boolean[] tcfSpecialFeatures = new boolean[TcfConsentManager.SPECIAL_FEATURE_COUNT];

            for (ConsentPurpose purpose : purposes.values()) {
                boolean consented = Boolean.TRUE.equals(hasPurposeConsent(purpose.id));
                for (int tcfId : purpose.tcfPurposeIds) {
                    if (tcfId >= 1 && tcfId <= TcfConsentManager.PURPOSE_COUNT) {
                        tcfPurposes[tcfId - 1] = consented;
                    }
                }
                // Special case: identification maps to special feature 2
                if (ConsentPurpose.PURPOSE_IDENTIFICATION.equals(purpose.id)) {
                    tcfSpecialFeatures[1] = consented; // Special Feature 2
                }
            }

            tcfManager.writeConsentSignals(gdprApplies, tcfPurposes, tcfSpecialFeatures);
        }

        // US Privacy
        if (usPrivacyManager != null) {
            usPrivacyManager.writeConsentSignal(ccpaApplies, anyConsent);
        }

        // Google Consent Mode v2
        if (consentModeEnabled) {
            GoogleConsentMode.setConsent(context, analyticsConsent, adsConsent);
        }
    }

    private void updateStandardsSignals(boolean consent) {
        if (tcfManager != null) {
            tcfManager.writeConsentSignals(gdprApplies, consent);
        }
        if (usPrivacyManager != null) {
            usPrivacyManager.writeConsentSignal(ccpaApplies, consent);
        }
    }

    @Nullable public TcfConsentManager getTcfManager() { return tcfManager; }
    @Nullable public UsPrivacyManager getUsPrivacyManager() { return usPrivacyManager; }
    public boolean isGpcEnabled() { return GpcInterceptor.isEnabled(); }

    // ---- Consent dialog ----

    /**
     * Show the purpose-based consent dialog.
     * Lists purposes (Analytics, Advertising, etc.) instead of individual SDKs.
     */
    public void askConsent() {
        // Build list of purposes that have at least one present library and need consent
        List<ConsentPurpose> pendingPurposes = new LinkedList<>();

        Set<String> presentLibraryIds = new HashSet<>();
        for (Library library : libraries) {
            if (library.isPresent()) {
                presentLibraryIds.add(library.getId());
            }
        }

        for (ConsentPurpose purpose : purposes.values()) {
            if (purpose.essential) continue;
            if (hasPurposeConsent(purpose.id) != null) continue;
            if (!showConsent) continue;

            // Only show purposes that have at least one present library
            boolean hasPresent = false;
            for (String libId : purpose.libraryIds) {
                if (presentLibraryIds.contains(libId)) {
                    hasPresent = true;
                    break;
                }
            }
            if (hasPresent) {
                pendingPurposes.add(purpose);
            }
        }

        if (pendingPurposes.isEmpty()) return;

        String[] names = new String[pendingPurposes.size()];
        for (int i = 0; i < pendingPurposes.size(); i++) {
            ConsentPurpose p = pendingPurposes.get(i);
            names[i] = context.getString(p.nameResId) + "\n"
                    + context.getString(p.descriptionResId);
        }

        List<String> selectedPurposes = new LinkedList<>();

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.consent_purpose_title)
                .setPositiveButton(R.string.accept_selected, (dialog, which) -> {
                    for (ConsentPurpose purpose : pendingPurposes) {
                        savePurposeConsent(purpose.id,
                                selectedPurposes.contains(purpose.id));
                    }
                })
                .setNegativeButton(R.string.reject_all, (dialog, which) -> {
                    for (ConsentPurpose purpose : pendingPurposes) {
                        savePurposeConsent(purpose.id, false);
                    }
                })
                .setMultiChoiceItems(names, null, (dialog, i, isChecked) -> {
                    String purposeId = pendingPurposes.get(i).id;
                    if (isChecked) selectedPurposes.add(purposeId);
                    else selectedPurposes.remove(purposeId);
                })
                .setNeutralButton(R.string.privacy_policy, null)
                .setCancelable(false)
                .create();

        alertDialog.setOnShowListener(dialogInterface -> {
            Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, privacyPolicy);
                context.startActivity(browserIntent);
            });
        });

        alertDialog.show();
    }

    // ---- Builder ----

    public static class Builder {
        Context context;
        boolean showConsent = true;
        Uri privacyPolicy = null;
        String[] excludedLibraries = {};
        Library[] customLibraries = {};

        boolean enableTcf = false;
        int tcfCmpSdkId = 0;
        int tcfCmpSdkVersion = 1;
        String publisherCountryCode = "AA";
        boolean enableUsPrivacy = false;
        boolean enableGpc = false;
        boolean enableConsentMode = false;
        boolean gdprApplies = false;
        boolean ccpaApplies = false;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCustomLibraries(Library[] customLibraries) {
            this.customLibraries = customLibraries;
            return this;
        }

        public Builder setShowConsent(boolean showConsent) {
            this.showConsent = showConsent;
            return this;
        }

        public Builder setPrivacyPolicy(Uri privacyPolicy) {
            this.privacyPolicy = privacyPolicy;
            return this;
        }

        public Builder setExcludedLibraries(String[] excludedLibraries) {
            this.excludedLibraries = excludedLibraries;
            return this;
        }

        public Builder enableTcf(int cmpSdkId, int sdkVersion) {
            this.enableTcf = true;
            this.tcfCmpSdkId = cmpSdkId;
            this.tcfCmpSdkVersion = sdkVersion;
            return this;
        }

        public Builder enableTcf() {
            return enableTcf(0, 1);
        }

        public Builder setPublisherCountryCode(String countryCode) {
            this.publisherCountryCode = countryCode;
            return this;
        }

        public Builder enableUsPrivacy() {
            this.enableUsPrivacy = true;
            return this;
        }

        public Builder enableGpc() {
            this.enableGpc = true;
            return this;
        }

        /**
         * Enable Google Consent Mode v2. Sets ad_storage, analytics_storage,
         * ad_user_data, and ad_personalization signals that Google SDKs read.
         * Required for EU ad serving since March 2024.
         */
        public Builder enableGoogleConsentMode() {
            this.enableConsentMode = true;
            return this;
        }

        public Builder setGdprApplies(boolean gdprApplies) {
            this.gdprApplies = gdprApplies;
            return this;
        }

        public Builder setCcpaApplies(boolean ccpaApplies) {
            this.ccpaApplies = ccpaApplies;
            return this;
        }

        public ConsentManager build() {
            if (privacyPolicy == null)
                throw new RuntimeException("No privacy policy provided.");

            return ConsentManager.getInstance(
                    context, showConsent, privacyPolicy, excludedLibraries, customLibraries,
                    enableTcf, tcfCmpSdkId, tcfCmpSdkVersion, publisherCountryCode,
                    enableUsPrivacy, enableGpc, enableConsentMode, gdprApplies, ccpaApplies);
        }
    }
}
