package net.kollnig.consent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

public class ConsentManager {
    public static final String FIREBASE_ANALYTICS_CLASS = "com.google.firebase.analytics.FirebaseAnalytics";
    static final String TAG = "HOOKED";
    @SuppressLint("StaticFieldLeak")
    private static ConsentManager mConsentManager = null;

    private final Context context;

    private ConsentManager(Context context) {
        this.context = context;
    }

    public static ConsentManager getInstance(Context context) {
        if (mConsentManager == null) {
            mConsentManager = new ConsentManager(context);
            mConsentManager.initialise();
        }

        return mConsentManager;
    }

    // in this method, we can run whatever checks we would like to run before calling Firebase
    public static Object hookMe(@NonNull Context context) {
        Log.d(TAG, "successfully hooked");

        if (mConsentManager.hasConsent()) {
            // should contain FirebaseAnalytics object
            Object firebaseAnalytics = hookMeBackup(context);

            if (firebaseAnalytics == null)
                return null;

            // call firebaseAnalytics.setUserProperty("allow_personalized_ads", "true");
            try {
                Object[] arglist = {"allow_personalized_ads", "true"};
                Method setUserProperty = firebaseAnalytics.getClass().getMethod("setUserProperty", String.class, String.class);
                setUserProperty.invoke(firebaseAnalytics, arglist);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }

            return firebaseAnalytics;
        } else {
            throw new RuntimeException("Accessed Google Firebase without consent!");
        }
    }

    // this method will be replaced by hook
    public static Object hookMeBackup(@NonNull Context context) {
        throw new RuntimeException("Could not overwrite original Firebase method");
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

    public void initialise() {
        Class firebaseAnalyticsClass = findFirebaseAnalytics();
        if (firebaseAnalyticsClass != null) {
            Log.d(TAG, "has Firebase, needs consent");

            String methodName = "getInstance";
            String methodSig = "(Landroid/content/Context;)Lcom/google/firebase/analytics/FirebaseAnalytics;";

            try {
                Method methodOrig = (Method) HookMain.findMethodNative(firebaseAnalyticsClass, methodName, methodSig);
                Method methodHook = ConsentManager.class.getMethod("hookMe", Context.class);
                Method methodBackup = ConsentManager.class.getMethod("hookMeBackup", Context.class);
                HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Could not overwrite original Firebase method");
            }
        }
    }

    private Class findFirebaseAnalytics() {
        try {
            return Class.forName(FIREBASE_ANALYTICS_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
