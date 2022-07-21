package net.kollnig.consent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.kollnig.consent.library.AdvertisingIdLibrary;
import net.kollnig.consent.library.AppLovinLibrary;
import net.kollnig.consent.library.AppsFlyerLibrary;
import net.kollnig.consent.library.FacebookSdkLibrary;
import net.kollnig.consent.library.FirebaseAnalyticsLibrary;
import net.kollnig.consent.library.FlurryLibrary;
import net.kollnig.consent.library.GoogleAdsLibrary;
import net.kollnig.consent.library.InMobiLibrary;
import net.kollnig.consent.library.Library;
import net.kollnig.consent.library.LibraryInteractionException;

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

    Library[] availableLibraries = {
            new FirebaseAnalyticsLibrary(),
            new FacebookSdkLibrary(),
            new AppLovinLibrary(),
            new AdvertisingIdLibrary(),
            new GoogleAdsLibrary(),
            new AppsFlyerLibrary(),
            new InMobiLibrary(),
            new FlurryLibrary()
    };

    private ConsentManager(Context context,
                           boolean showConsent,
                           Uri privacyPolicy,
                           String[] excludedLibraries) {

        this.context = context;
        this.showConsent = showConsent;
        this.privacyPolicy = privacyPolicy;
        this.excludedLibraries = excludedLibraries;
    }

    private static ConsentManager getInstance(Context context,
                                              Boolean showConsent,
                                              Uri privacyPolicy,
                                              String[] excludeLibraries) {
        if (mConsentManager == null) {
            mConsentManager = new ConsentManager(context, showConsent, privacyPolicy, excludeLibraries);

            mConsentManager.libraries = new LinkedList<>();
            try {
                for (Library library : mConsentManager.availableLibraries) {
                    if (!library.isPresent() ||
                            Arrays.asList(mConsentManager.excludedLibraries).contains(library.getId()))
                        continue;

                    library.initialise(context);

                    mConsentManager.libraries.add(library);
                }
            } catch (LibraryInteractionException e) {
                e.printStackTrace();
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
    }

    public void askConsent() {
        List<String> ids = new LinkedList<>();
        List<String> names = new LinkedList<>();

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

        List<String> selectedItems = new LinkedList<>();
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.consent_title)
                .setPositiveButton(R.string.accept_all, (dialog, which) -> {
                    for (Library library: libraries) {
                        String libraryId = library.getId();

                        if (!ids.contains(libraryId))
                            continue;

                        saveConsent(libraryId, true);
                    }
                })
                .setNegativeButton(R.string.accept_selected, (dialog, which) -> {
                    for (Library library: libraries) {
                        String libraryId = library.getId();

                        if (!ids.contains(libraryId))
                            continue;

                        saveConsent(libraryId, selectedItems.contains(libraryId));
                    }
                })
                .setMultiChoiceItems(names.toArray(new String[0]), null, (dialog, i, isChecked) -> {
                    if (isChecked) selectedItems.add(ids.get(i));
                    else selectedItems.remove(ids.get(i));
                })
                .setNeutralButton("Privacy Policy", null)
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

        public Builder(Context context) {
            this.context = context;
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

        public ConsentManager build() {
            if (privacyPolicy == null)
                throw new RuntimeException("No privacy policy provided.");

            return ConsentManager.getInstance(context, showConsent, privacyPolicy, excludedLibraries);
        }
    }
}
