package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FirebaseAnalyticsLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "firebase_analytics";

    @Override
    public @NonNull
    String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        try {
            // Call FirebaseAnalytics.getInstance(context)
            Method getInstance = baseClass.getMethod("getInstance", Context.class);
            Object firebaseAnalytics = getInstance.invoke(null, getContext());

            // Call FirebaseAnalytics.setAnalyticsCollectionEnabled(true)
            Method setAnalyticsCollectionEnabled = baseClass.getMethod("setAnalyticsCollectionEnabled", boolean.class);
            setAnalyticsCollectionEnabled.invoke(firebaseAnalytics, consent);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new LibraryInteractionException("Could not save settings to Firebase Analytics.");
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

    @Override
    public int getName() {
        return R.string.firebase_analytics;
    }
}
