package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.Method;

public class FlurryLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "flurry";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    static final String TAG = "HOOKED";

    public static void replacementBuild(Object thiz, @NonNull Context var1, @NonNull String var2) {
        Log.d(TAG, "successfully hooked Flurry");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            return;

        try {
            HookCompat.callOriginal(
                    FlurryLibrary.class, "originalBuild",
                    new Class[]{Object.class, Context.class, String.class},
                    thiz, var1, var2);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original Flurry build", e);
        }
    }

    // stub — used as key for HookCompat backup registration
    public static void originalBuild(Object thiz, @NonNull Context var1, @NonNull String var2) {
        throw new RuntimeException("Hook not installed for Flurry build");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        Class<?> baseClass = findBaseClass();
        try {
            Method methodOrig = baseClass.getMethod("build", Context.class, String.class);
            Method methodHook = FlurryLibrary.class.getMethod("replacementBuild", Object.class, Context.class, String.class);
            Method methodBackup = FlurryLibrary.class.getMethod("originalBuild", Object.class, Context.class, String.class);
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
