package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FirebaseAnalyticsLibrary extends Library {
    public static final String FIREBASE_ANALYTICS_CLASS = "com.google.firebase.analytics.FirebaseAnalytics";
    public static final String FIREBASE_ANALYTICS_LIBRARY = "firebase_analytics";

    public FirebaseAnalyticsLibrary(Context context) throws LibraryInteractionException {
        super(context);
    }

    static @Nullable
    Class findFirebaseAnalytics() {
        try {
            return Class.forName(FIREBASE_ANALYTICS_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public String getId() {
        return FIREBASE_ANALYTICS_LIBRARY;
    }

    @Override
    void initialise() {
        // do nothing
    }

    @Override
    public void saveConsent(Boolean consent) throws LibraryInteractionException {
        Class firebaseAnalyticsClass = findFirebaseAnalytics();
        if (firebaseAnalyticsClass != null) {
            try {
                // Call FirebaseAnalytics.getInstance(context)
                Object[] arglist = {getContext()};
                Method getInstance = firebaseAnalyticsClass.getMethod("getInstance", Context.class);
                Object firebaseAnalytics = getInstance.invoke(null, arglist);

                // Call FirebaseAnalytics.setAnalyticsCollectionEnabled(true)
                arglist[0] = consent;
                Method setAnalyticsCollectionEnabled = firebaseAnalyticsClass.getMethod("setAnalyticsCollectionEnabled", boolean.class);
                setAnalyticsCollectionEnabled.invoke(firebaseAnalytics, arglist);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to Firebase Analytics.");
            }
        }
    }

    @Override
    public boolean isPresent() {
        return findFirebaseAnalytics() != null;
    }
}
