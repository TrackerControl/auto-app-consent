package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.io.IOException;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

public class AdvertisingIdLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "google_ads_identifier";
    static final String TAG = "HOOKED";

    public static Object replacementMethod(@NonNull Context context) throws IOException {
        Log.d(TAG, "successfully hooked AAID");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            throw new IOException("Blocked attempt to access Advertising Identifier without consent.");

        return originalMethod(context);
    }

    // this method will be replaced by hook
    public static Object originalMethod(@NonNull Context context) {
        throw new RuntimeException("Could not overwrite original Firebase method");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        Class advertisingIdClass = findBaseClass();
        String methodName = "getAdvertisingIdInfo";
        String methodSig = "(Landroid/content/Context;)Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;";

        try {
            Method methodOrig = (Method) HookMain.findMethodNative(advertisingIdClass, methodName, methodSig);
            Method methodHook = AdvertisingIdLibrary.class.getMethod("replacementMethod", Context.class);
            Method methodBackup = AdvertisingIdLibrary.class.getMethod("originalMethod", Context.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not overwrite method");
        }

        return this;
    }

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
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
