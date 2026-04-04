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
import net.kollnig.consent.standards.GpcInterceptor;
import net.kollnig.consent.standards.TcfConsentManager;
import net.kollnig.consent.standards.UsPrivacyManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ConsentManager {
    public static final String PREFERENCES_NAME = "net.kollnig.consent";
    static final String TAG = ConsentManager.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static ConsentManager mConsentManager = null;
    private final Uri privacyPolicy;
    private final boolean showConsent;
    private List<Library> libraries;
    private final Context context;
    private final String[] excludedLibraries;

    // Standards support
    private TcfConsentManager tcfManager;
    private UsPrivacyManager usPrivacyManager;
    private boolean gpcEnabled;
    private boolean gdprApplies;
    private boolean ccpaApplies;

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
                           boolean gdprApplies,
                           boolean ccpaApplies) {

        this.context = context;
        this.showConsent = showConsent;
        this.privacyPolicy = privacyPolicy;
        this.excludedLibraries = excludedLibraries;
        this.gdprApplies = gdprApplies;
        this.ccpaApplies = ccpaApplies;
        this.gpcEnabled = enableGpc;

        // Initialize standards managers
        if (enableTcf) {
            this.tcfManager = new TcfConsentManager(
                    context, tcfCmpSdkId, tcfCmpSdkVersion, publisherCountryCode);
        }
        if (enableUsPrivacy) {
            this.usPrivacyManager = new UsPrivacyManager(context);
        }
        if (enableGpc) {
            GpcInterceptor.setEnabled(true);
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
                                              boolean gdprApplies,
                                              boolean ccpaApplies) {
        if (mConsentManager == null) {
            mConsentManager = new ConsentManager(
                    context, showConsent, privacyPolicy, excludeLibraries,
                    enableTcf, tcfCmpSdkId, tcfCmpSdkVersion, publisherCountryCode,
                    enableUsPrivacy, enableGpc, gdprApplies, ccpaApplies);

            mConsentManager.libraries = new LinkedList<>();
            try {
                // merge `availableLibraries` and `customLibraries` into `allLibraries`
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

            // Write initial deny signals for standards (default deny until consent given)
            mConsentManager.updateStandardsSignals(false);

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

    public @Nullable
    Boolean hasConsent(String libraryId) {
        SharedPreferences prefs = getPreferences(context);

        Set<String> set = prefs.getStringSet("consents", new HashSet<>());
        if (set.contains(libraryId + ":" + true))
            return true;
        else if (set.contains(libraryId + ":" + false))
            return false;
        else
            return null;
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

        // Clear standards signals too
        if (tcfManager != null) {
            tcfManager.clearConsentSignals();
        }
        if (usPrivacyManager != null) {
            usPrivacyManager.clearConsentSignal();
        }
        GpcInterceptor.setEnabled(gpcEnabled);
    }

    public void saveConsent(String libraryId, boolean consent) {
        SharedPreferences prefs = getPreferences(context);

        Set<String> set = prefs.getStringSet("consents", null);
        Set<String> prefsSet = new HashSet<>();
        if (set != null)
            prefsSet.addAll(set);

        for (Library library : libraries) {
            if (!library.getId().equals(libraryId))
                continue;

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

        // Update standards signals based on overall consent state
        updateStandardsSignals(hasAnyConsent());
    }

    /**
     * Check if the user has given consent to at least one library.
     */
    private boolean hasAnyConsent() {
        for (Library library : libraries) {
            if (Boolean.TRUE.equals(hasConsent(library.getId()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update all enabled industry-standard consent signals.
     * Called when consent state changes.
     */
    private void updateStandardsSignals(boolean consent) {
        if (tcfManager != null) {
            tcfManager.writeConsentSignals(gdprApplies, consent);
        }
        if (usPrivacyManager != null) {
            usPrivacyManager.writeConsentSignal(ccpaApplies, consent);
        }
        // GPC is always-on when enabled — it signals "do not sell" regardless of
        // per-library consent, as it represents the user's general privacy preference
    }

    /**
     * Get the TCF consent manager for advanced per-purpose configuration.
     * Returns null if TCF was not enabled in the builder.
     */
    @Nullable
    public TcfConsentManager getTcfManager() {
        return tcfManager;
    }

    /**
     * Get the US Privacy manager for advanced CCPA configuration.
     * Returns null if US Privacy was not enabled in the builder.
     */
    @Nullable
    public UsPrivacyManager getUsPrivacyManager() {
        return usPrivacyManager;
    }

    /**
     * Check if GPC (Global Privacy Control) is enabled.
     */
    public boolean isGpcEnabled() {
        return GpcInterceptor.isEnabled();
    }

    public void askConsent() {
        List<String> ids = new LinkedList<>();
        List<String> names = new LinkedList<>();
        List<String> selectedItems = new LinkedList<>();

        for (Library library : libraries) {
            if (library.isPresent()) {
                String libraryId = library.getId();
                if (hasConsent(libraryId) == null && showConsent) {
                    ids.add(libraryId);
                    names.add(context.getString(library.getName()));
                }
            }
        }

        if (ids.size() == 0)
            return;

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.consent_title)
                .setPositiveButton(R.string.accept_selected, (dialog, which) -> {
                    for (Library library : libraries) {
                        String libraryId = library.getId();

                        if (!ids.contains(libraryId))
                            continue;

                        saveConsent(libraryId, selectedItems.contains(libraryId));
                    }
                })
                .setNegativeButton(R.string.reject_all, (dialog, which) -> {
                    for (Library library : libraries) {
                        String libraryId = library.getId();

                        if (!ids.contains(libraryId))
                            continue;

                        saveConsent(libraryId, false);
                    }
                })
                .setMultiChoiceItems(names.toArray(new String[0]), null, (dialog, i, isChecked) -> {
                    if (isChecked) selectedItems.add(ids.get(i));
                    else selectedItems.remove(ids.get(i));
                })
                .setNeutralButton(R.string.privacy_policy, null)
                .setCancelable(false)
                .create();

        // this is needed to avoid the dialog from closing on clicking the policy button
        alertDialog.setOnShowListener(dialogInterface -> {
            Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, privacyPolicy);
                context.startActivity(browserIntent);
            });
        });

        alertDialog.show();
    }

    public static class Builder {
        Context context;
        boolean showConsent = true;
        Uri privacyPolicy = null;
        String[] excludedLibraries = {};
        Library[] customLibraries = {};

        // Standards support options
        boolean enableTcf = false;
        int tcfCmpSdkId = 0;
        int tcfCmpSdkVersion = 1;
        String publisherCountryCode = "AA"; // "AA" = unknown per TCF spec
        boolean enableUsPrivacy = false;
        boolean enableGpc = false;
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

        /**
         * Enable IAB TCF v2.2 support. Writes standard IABTCF_ keys to
         * SharedPreferences that most ad SDKs read natively.
         *
         * @param cmpSdkId   your registered CMP SDK ID (0 = unregistered)
         * @param sdkVersion your CMP SDK version
         */
        public Builder enableTcf(int cmpSdkId, int sdkVersion) {
            this.enableTcf = true;
            this.tcfCmpSdkId = cmpSdkId;
            this.tcfCmpSdkVersion = sdkVersion;
            return this;
        }

        /**
         * Enable IAB TCF v2.2 with default values (unregistered CMP).
         */
        public Builder enableTcf() {
            return enableTcf(0, 1);
        }

        /**
         * Set the publisher's country code for TCF (ISO 3166-1 alpha-2).
         */
        public Builder setPublisherCountryCode(String countryCode) {
            this.publisherCountryCode = countryCode;
            return this;
        }

        /**
         * Enable IAB US Privacy String (CCPA) support.
         * Writes IABUSPrivacy_String to SharedPreferences.
         */
        public Builder enableUsPrivacy() {
            this.enableUsPrivacy = true;
            return this;
        }

        /**
         * Enable Global Privacy Control (GPC).
         * Adds Sec-GPC: 1 header to HTTP requests and sets
         * navigator.globalPrivacyControl in WebViews.
         */
        public Builder enableGpc() {
            this.enableGpc = true;
            return this;
        }

        /**
         * Set whether GDPR applies to users of this app.
         * Affects TCF signal output.
         */
        public Builder setGdprApplies(boolean gdprApplies) {
            this.gdprApplies = gdprApplies;
            return this;
        }

        /**
         * Set whether CCPA applies to users of this app.
         * Affects US Privacy String output.
         */
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
                    enableUsPrivacy, enableGpc, gdprApplies, ccpaApplies);
        }
    }
}
