package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FirebaseAnalyticsLibrary extends Library {
    public FirebaseAnalyticsLibrary(Context context) throws LibraryInteractionException {
        super(context);
    }

    @Override
    public @NonNull String getId() {
        return "firebase_analytics";
    }

    @Override
    public void saveConsent(boolean consent) throws LibraryInteractionException {
        Class baseClass = findBaseClass();
        if (baseClass != null) {
            try {
                // Call FirebaseAnalytics.getInstance(context)
                Object[] arglist = {getContext()};
                Method getInstance = baseClass.getMethod("getInstance", Context.class);
                Object firebaseAnalytics = getInstance.invoke(null, arglist);

                // Call FirebaseAnalytics.setAnalyticsCollectionEnabled(true)
                arglist[0] = consent;
                Method setAnalyticsCollectionEnabled = baseClass.getMethod("setAnalyticsCollectionEnabled", boolean.class);
                setAnalyticsCollectionEnabled.invoke(firebaseAnalytics, arglist);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to Firebase Analytics.");
            }
        }
    }

    @Override
    String getBaseClass() {
        return "com.google.firebase.analytics.FirebaseAnalytics";
    }

    @Override
    public int getConsentMessage() {
        return R.string.firebase_analytics_consent_msg;
    }
}
