package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

public class AppsFlyerLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "appsflyer";
    static final String TAG = "HOOKED";

    public static void replacementStart(Object thiz, Context context, String string, Object object) {
        Log.d(TAG, "successfully hooked AppsFlyer");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            return;

        originalStart(thiz, context, string, object);
    }

    // this method will be replaced by hook
    public static void originalStart(Object thiz, Context context, String string, Object object) {
        throw new RuntimeException("Could not overwrite original AppsFlyer method");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        // AppsFlyerLib.getInstance().start(this);
        try {
            Class abstractBaseClass = Class.forName(getBaseClass());
            Method getInstance = abstractBaseClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);

            Class baseClass = instance.getClass();
            String methodName = "start";
            String methodSig = "(Landroid/content/Context;Ljava/lang/String;Lcom/appsflyer/attribution/AppsFlyerRequestListener;)V";

            try {
                Method methodOrig = (Method) HookMain.findMethodNative(baseClass, methodName, methodSig);
                Method methodHook = AppsFlyerLibrary.class.getMethod("replacementStart", Object.class, Context.class, String.class, Object.class);
                Method methodBackup = AppsFlyerLibrary.class.getMethod("originalStart", Object.class, Context.class, String.class, Object.class);
                HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Could not overwrite method");
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        try {
            Class abstractBaseClass = Class.forName(getBaseClass());
            Method getInstance = abstractBaseClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method stop = abstractBaseClass.getMethod("stop", boolean.class, Context.class);
            stop.invoke(instance, !consent, getContext());
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
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
