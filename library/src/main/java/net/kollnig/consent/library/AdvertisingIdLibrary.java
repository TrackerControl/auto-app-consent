package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

public class AdvertisingIdLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "google_ads_identifier";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        // Consent enforcement handled by build-time bytecode transform
        // (throws IOException if no consent) or TCF SharedPreferences signals
    }

    @Override
    String getBaseClass() {
        return "com.google.android.gms.ads.identifier.AdvertisingIdClient";
    }

    @Override
    public int getConsentMessage() {
        return R.string.aaid_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.aaid;
    }
}
