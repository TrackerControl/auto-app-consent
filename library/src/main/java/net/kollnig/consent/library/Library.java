package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kollnig.consent.ConsentManager;

public abstract class Library {
    private final Context context;

    public Library(Context context) throws LibraryInteractionException {
        this.context = context;

        if (getId().contains(":"))
            throw new RuntimeException("id cannot contain ':'");

        initialise();
    }

    Context getContext() {
        return context;
    }

    abstract public @NonNull
    String getId();

    void initialise() throws LibraryInteractionException {
        Boolean consent = ConsentManager.hasConsent(context, getId());

        if (consent == null
                || consent == false)
            saveConsent(false);
    };

    abstract public void saveConsent(Boolean consent) throws LibraryInteractionException;

    public boolean isPresent() {
        return findBaseClass() != null;
    }

    abstract String getBaseClass();

    @Nullable
    Class findBaseClass() {
        try {
            return Class.forName(getBaseClass());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    abstract public int getConsentMessage();

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
}
