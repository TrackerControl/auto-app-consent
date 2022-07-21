package net.kollnig.consent.library;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CrashlyticsLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "crashlytics";

    @Override
    public @NonNull
    String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        try {
            Method getInstance = baseClass.getMethod("getInstance");
            Object mFirebaseCrashlytics = getInstance.invoke(null);

            Method set = baseClass.getMethod("setCrashlyticsCollectionEnabled", boolean.class);
            set.invoke(mFirebaseCrashlytics, consent);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new LibraryInteractionException("Could not save settings to Crashlytics.");
        }
    }

    @Override
    String getBaseClass() {
        return "com.google.firebase.crashlytics.FirebaseCrashlytics";
    }

    @Override
    public int getConsentMessage() {
        return R.string.crashlytics_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.crashlytics;
    }
}
