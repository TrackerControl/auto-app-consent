package net.kollnig.consent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConsentManager {
    public static final String FIREBASE_ANALYTICS_CLASS = "com.google.firebase.analytics.FirebaseAnalytics";
    public static final String PREFERENCES_NAME = "net.kollnig.consent";
    static final String TAG = "HOOKED";
    @SuppressLint("StaticFieldLeak")
    private static ConsentManager mConsentManager = null;
    private final Uri privacyPolicy;
    private final boolean showConsent;

    private final Context context;

    private ConsentManager(Context context, boolean showConsent, Uri privacyPolicy) {
        this.context = context;
        this.showConsent = showConsent;
        this.privacyPolicy = privacyPolicy;
    }

    public static ConsentManager getInstance(Context context, Boolean showConsent, Uri privacyPolicy) {
        if (mConsentManager == null) {
            mConsentManager = new ConsentManager(context, showConsent, privacyPolicy);
            mConsentManager.initialise();
        }

        return mConsentManager;
    }

    // in this method, we can run whatever checks we would like to run before calling Firebase
    /*public static Object hookMe(@NonNull Context context) {
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
    }*/

    public void saveConsent(boolean consent) {
        SharedPreferences prefs = getPreferences();
        prefs.edit().putBoolean("has_consent", consent).apply();

        if (consent) {
            Class firebaseAnalyticsClass = findFirebaseAnalytics();

            if (firebaseAnalyticsClass != null) {
                try {
                    // Call FirebaseAnalytics.getInstance(context)
                    Object[] arglist = {context};
                    Method getInstance = firebaseAnalyticsClass.getMethod("getInstance", Context.class);
                    Object firebaseAnalytics = getInstance.invoke(null, arglist);

                    // Call FirebaseAnalytics.setAnalyticsCollectionEnabled(true)
                    arglist[0] = true;
                    Method setAnalyticsCollectionEnabled = firebaseAnalyticsClass.getMethod("setAnalyticsCollectionEnabled", boolean.class);
                    setAnalyticsCollectionEnabled.invoke(firebaseAnalytics, arglist);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public @Nullable
    Boolean hasConsent() {
        SharedPreferences prefs = getPreferences();

        if (!prefs.contains("has_consent"))
            return null;

        return prefs.getBoolean("has_consent", false);
    }

    private void initialise() {
        if (findFirebaseAnalytics() != null) {
            Log.d(TAG, "has Firebase, needs consent");

            Boolean consent = hasConsent();
            if (consent == null && showConsent) {
                final AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.consent_title)
                    .setMessage(R.string.consent_msg)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        saveConsent(true);
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        saveConsent(false);
                    })
                    .setNeutralButton("Privacy Policy", null)
                    .setCancelable(false)
                    .create();
                alertDialog.setOnShowListener(dialogInterface -> {
                    Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    neutralButton.setOnClickListener(view -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, privacyPolicy);
                        context.startActivity(browserIntent);
                    });
                });
                alertDialog.show();
            }

            /*String methodName = "getInstance";
            String methodSig = "(Landroid/content/Context;)Lcom/google/firebase/analytics/FirebaseAnalytics;";

            try {
                Method methodOrig = (Method) HookMain.findMethodNative(firebaseAnalyticsClass, methodName, methodSig);
                Method methodHook = ConsentManager.class.getMethod("hookMe", Context.class);
                Method methodBackup = ConsentManager.class.getMethod("hookMeBackup", Context.class);
                HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Could not overwrite original Firebase method");
            }*/
        }
    }

    private @Nullable
    Class findFirebaseAnalytics() {
        try {
            return Class.forName(FIREBASE_ANALYTICS_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
