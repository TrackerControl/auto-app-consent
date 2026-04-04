package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class VungleLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "vungle";

    @Override
    public @NonNull
    String getId() {
        return LIBRARY_IDENTIFIER;
    }

    static final String TAG = "HOOKED";

    public static void replacementInit(Object thiz, @NonNull final Object callback, boolean isReconfig) {
        Log.d(TAG, "successfully hooked Vungle");

        try {
            HookCompat.callOriginal(
                    VungleLibrary.class, "originalInit",
                    new Class[]{Object.class, Object.class, boolean.class},
                    thiz, callback, isReconfig);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original Vungle configure", e);
        }

        boolean consent = Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER));
        try {
            passConsent(consent);
        } catch (LibraryInteractionException e) {
            throw new RuntimeException("Passing consent to Vungle failed.");
        }
    }

    // stub — used as key for HookCompat backup registration
    public static void originalInit(Object thiz, @NonNull final Object callback, boolean isReconfig) {
        throw new RuntimeException("Hook not installed for Vungle configure");
    }

    private static void passConsent(boolean consent) throws LibraryInteractionException {
        try {
            Class<?> baseClass = Class.forName("com.vungle.warren.Vungle");
            Class<?> consentClass = Class.forName("com.vungle.warren.Vungle$Consent");

            Object optIn = null;
            Object optOut = null;
            for (Object enumConstant : Objects.requireNonNull(consentClass.getEnumConstants())) {
                Method m = consentClass.getMethod("name");
                if (String.valueOf(m.invoke(enumConstant)).equals("OPTED_IN"))
                    optIn = enumConstant;

                if (String.valueOf(m.invoke(enumConstant)).equals("OPTED_OUT"))
                    optOut = enumConstant;
            }

            if (optIn == null || optOut == null)
                throw new LibraryInteractionException("Could not retrieve consent objects.");

            Method updateConsentStatus = baseClass.getMethod("updateConsentStatus", consentClass, String.class);
            updateConsentStatus.invoke(null, consent ? optIn : optOut, "1.0.0");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new LibraryInteractionException("Could not save settings to Vungle.");
        }
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        Class<?> baseClass = findBaseClass();
        try {
            Class<?> callbackClass = Class.forName("com.vungle.warren.InitCallback");
            Method methodOrig = baseClass.getDeclaredMethod("configure", callbackClass, boolean.class);
            methodOrig.setAccessible(true);
            Method methodHook = VungleLibrary.class.getMethod("replacementInit", Object.class, Object.class, boolean.class);
            Method methodBackup = VungleLibrary.class.getMethod("originalInit", Object.class, Object.class, boolean.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Could not find method to hook", e);
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        passConsent(consent);
    }

    @Override
    String getBaseClass() {
        return "com.vungle.warren.Vungle";
    }

    @Override
    public int getConsentMessage() {
        return R.string.vungle_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.vungle;
    }
}
