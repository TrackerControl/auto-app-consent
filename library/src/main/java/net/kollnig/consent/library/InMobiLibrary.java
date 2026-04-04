package net.kollnig.consent.library;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

public class InMobiLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "inmobi";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        // Consent enforcement handled by build-time bytecode transform
        // (blocks init() if no consent) or TCF SharedPreferences signals
    }

    @Override
    String getBaseClass() {
        return "com.inmobi.sdk.InMobiSdk";
    }

    @Override
    public int getConsentMessage() {
        return R.string.inmobi_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.inmobi;
    }
}
