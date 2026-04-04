package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppsFlyerLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "appsflyer";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    static final String TAG = "HOOKED";

    public static void replacementStart(Object thiz, Context context, String string, Object object) {
        Log.d(TAG, "successfully hooked AppsFlyer");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            return;

        try {
            HookCompat.callOriginal(
                    AppsFlyerLibrary.class, "originalStart",
                    new Class[]{Object.class, Context.class, String.class, Object.class},
                    thiz, context, string, object);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original AppsFlyer start", e);
        }
    }

    // stub — used as key for HookCompat backup registration
    public static void originalStart(Object thiz, Context context, String string, Object object) {
        throw new RuntimeException("Hook not installed for AppsFlyer start");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        try {
            Class<?> abstractBaseClass = findBaseClass();
            Method getInstance = abstractBaseClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);

            Class<?> baseClass = instance.getClass();
            Class<?> listenerClass = Class.forName("com.appsflyer.attribution.AppsFlyerRequestListener");

            Method methodOrig = baseClass.getMethod("start", Context.class, String.class, listenerClass);
            Method methodHook = AppsFlyerLibrary.class.getMethod("replacementStart", Object.class, Context.class, String.class, Object.class);
            Method methodBackup = AppsFlyerLibrary.class.getMethod("originalStart", Object.class, Context.class, String.class, Object.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        try {
            Class<?> abstractBaseClass = findBaseClass();
            Method getInstance = abstractBaseClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method stop = abstractBaseClass.getMethod("stop", boolean.class, Context.class);
            stop.invoke(instance, !consent, getContext());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    String getBaseClass() {
        return "com.appsflyer.AppsFlyerLib";
    }

    @Override
    public int getConsentMessage() {
        return R.string.appsflyer_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.appsflyer;
    }
}
