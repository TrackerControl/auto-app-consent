package net.kollnig.consent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

public class ConsentManager {
    @SuppressLint("StaticFieldLeak")
    private static ConsentManager mConsentManager = null;

    private final Context context;

    private ConsentManager(Context context) {
        this.context = context;
    }

    public static ConsentManager getInstance (Context context) {
        if (mConsentManager == null) {
            mConsentManager = new ConsentManager(context);
            mConsentManager.initialise();
        }

        return mConsentManager;
    }

    public void saveConsent(boolean consent) {
        SharedPreferences prefs = context.getSharedPreferences(
                ConsentManager.class.getSimpleName(), Context.MODE_PRIVATE);
        prefs.edit().putBoolean("has_consent", consent).apply();
    }

    public boolean hasConsent() {
        SharedPreferences prefs = context.getSharedPreferences(
                ConsentManager.class.getSimpleName(), Context.MODE_PRIVATE);
        return prefs.getBoolean("has_consent", false);
    }

    // in this method, we can run whatever checks we would like to run before calling Firebase
    public static Object hookMe(@NonNull Context context) {
        Log.d("HOOKED", "successfully hooked");

        if (mConsentManager.hasConsent())
            return hookMeBackup(context);
        else {
            throw new RuntimeException("Accessed Google Firebase without consent!");
        }
    }

    // this method will be replaced by hook
    public static Object hookMeBackup(@NonNull Context context) {
        throw new RuntimeException("Could not overwrite original Firebase method");
    }

    public void initialise() {
        String className = "com.google.firebase.analytics.FirebaseAnalytics";
        String methodName = "getInstance";
        String methodSig = "(Landroid/content/Context;)Lcom/google/firebase/analytics/FirebaseAnalytics;";

        try {
            Method methodOrig = (Method) HookMain.findMethodNative(Class.forName(className), methodName, methodSig);
            Method methodHook = ConsentManager.class.getMethod("hookMe", Context.class);
            Method methodBackup = ConsentManager.class.getMethod("hookMeBackup", Context.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("Could not overwrite original Firebase method");
        }
    }
}
