package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import lab.galaxy.yahfa.HookMain;

public class VungleLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "vungle";
    static final String TAG = "HOOKED";

    public static void replacementInit(Object thiz, @NonNull final Object callback, boolean isReconfig) {
        Log.d(TAG, "successfully hooked Vungle");

        originalInit(thiz, callback, isReconfig);

        boolean consent = Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER));
        try {
            passConsent(consent);
        } catch (LibraryInteractionException e) {
            throw new RuntimeException("Passing consent to Vungle failed.");
        }
    }

    // this method will be replaced by hook
    public static void originalInit(Object thiz, @NonNull final Object callback, boolean isReconfig) {
        throw new RuntimeException("Could not overwrite original Vungle method");
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

            // public static void updateConsentStatus(@NonNull Consent status, @NonNull String consentMessageVersion)
            Method updateConsentStatus = baseClass.getMethod("updateConsentStatus", consentClass, String.class);
            updateConsentStatus.invoke(null, consent ? optIn : optOut, "1.0.0");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new LibraryInteractionException("Could not save settings to Vungle.");
        }
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        // private void configure(@NonNull final InitCallback callback, boolean isReconfig) {
        Class<?> baseClass = findBaseClass();
        String methodName = "configure";
        String methodSig = "(Lcom/vungle/warren/InitCallback;Z)V";

        try {
            Method methodOrig = (Method) HookMain.findMethodNative(baseClass, methodName, methodSig);
            Method methodHook = VungleLibrary.class.getMethod("replacementInit", Object.class, Object.class, boolean.class);
            Method methodBackup = VungleLibrary.class.getMethod("originalInit", Object.class, Object.class, boolean.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not overwrite method");
        }

        return this;
    }

    @Override
    public @NonNull
    String getId() {
        return LIBRARY_IDENTIFIER;
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
