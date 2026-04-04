package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.io.IOException;
import java.lang.reflect.Method;

public class AdvertisingIdLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "google_ads_identifier";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    static final String TAG = "HOOKED";

    public static Object replacementMethod(@NonNull Context context) throws IOException {
        Log.d(TAG, "successfully hooked AAID");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            throw new IOException("Blocked attempt to access Advertising Identifier without consent.");

        try {
            return HookCompat.callOriginal(
                    AdvertisingIdLibrary.class, "originalMethod",
                    new Class[]{Context.class}, null, context);
        } catch (Exception e) {
            throw new IOException("Failed to call original getAdvertisingIdInfo", e);
        }
    }

    // stub — used as key for HookCompat backup registration
    public static Object originalMethod(@NonNull Context context) {
        throw new RuntimeException("Hook not installed for getAdvertisingIdInfo");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        Class<?> advertisingIdClass = findBaseClass();
        try {
            Method methodOrig = advertisingIdClass.getMethod("getAdvertisingIdInfo", Context.class);
            Method methodHook = AdvertisingIdLibrary.class.getMethod("replacementMethod", Context.class);
            Method methodBackup = AdvertisingIdLibrary.class.getMethod("originalMethod", Context.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find method to hook", e);
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        // do nothing
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
