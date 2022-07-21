package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

public class FlurryLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "flurry";
    static final String TAG = "HOOKED";

    public static void replacementBuild(Object thiz, @NonNull Context var1, @NonNull String var2) {
        Log.d(TAG, "successfully hooked Flurry");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER))) {
            /*try {
                Method withDataSaleOptOut = thiz.getClass().getMethod("withDataSaleOptOut", boolean.class);
                withDataSaleOptOut.invoke(thiz, true);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }*/

            return;
        }

        originalBuild(thiz, var1, var2);
    }

    // this method will be replaced by hook
    public static void originalBuild(Object thiz, @NonNull Context var1, @NonNull String var2) {
        throw new RuntimeException("Could not overwrite original Flurry method");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        // build(Landroid/content/Context;Ljava/lang/String;)V
        Class baseClass = findBaseClass();
        String methodName = "build";
        String methodSig = "(Landroid/content/Context;Ljava/lang/String;)V";

        try {
            Method methodOrig = (Method) HookMain.findMethodNative(baseClass, methodName, methodSig);
            Method methodHook = FlurryLibrary.class.getMethod("replacementBuild", Object.class, Context.class, String.class);
            Method methodBackup = FlurryLibrary.class.getMethod("originalBuild", Object.class, Context.class, String.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not overwrite method");
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        // do nothing
    }

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
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
