package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

public class GoogleAdsLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "google_ads";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        if (!consent)
            getContext().deleteSharedPreferences("admob");
    }

    @Override
    String getBaseClass() {
        return "com.google.android.gms.ads.MobileAds";
    }

    @Override
    public int getConsentMessage() {
        return R.string.google_ads_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.google_ads;
    }
}
