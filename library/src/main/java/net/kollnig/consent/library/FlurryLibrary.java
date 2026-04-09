package net.kollnig.consent.library;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

public class FlurryLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "flurry";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        // Consent enforcement handled by build-time bytecode transform
        // (blocks build() if no consent) or TCF SharedPreferences signals
    }

    @Override
    String getBaseClass() {
        return "com.flurry.android.FlurryAgent$Builder";
    }

    @Override
    public int getConsentMessage() {
        return R.string.flurry_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.flurry;
    }
}
