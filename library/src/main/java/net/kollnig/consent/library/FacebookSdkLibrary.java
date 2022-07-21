package net.kollnig.consent.library;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FacebookSdkLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "facebook_sdk";

    @Override
    public @NonNull
    String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        try {
            // Call FacebookSdk.setAutoInitEnabled(consent);
            Method setAutoInitEnabled = baseClass.getMethod("setAutoInitEnabled", boolean.class);
            setAutoInitEnabled.invoke(null, consent);

            // Call FacebookSdk.fullyInitialize();
            if (!consent) {
                Method fullyInitialize = baseClass.getMethod("fullyInitialize");
                fullyInitialize.invoke(null);
            }

            // Call FacebookSdk.setAutoLogAppEventsEnabled(consent);
            try {
                Method setAutoLogAppEventsEnabled = baseClass.getMethod("setAutoLogAppEventsEnabled", boolean.class);
                setAutoLogAppEventsEnabled.invoke(null, consent);
            } catch (InvocationTargetException e) {
                if (!e.getTargetException().getClass().getName().equals("com.facebook.FacebookSdkNotInitializedException"))
                    throw e;
            }

            // Call FacebookSdk.setAdvertiserIDCollectionEnabled(consent);
            //Method setAdvertiserIDCollectionEnabled = baseClass.getMethod("setAdvertiserIDCollectionEnabled", boolean.class);
            //setAdvertiserIDCollectionEnabled.invoke(null, consent);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new LibraryInteractionException("Could not save settings to Facebook SDK.");
        }
    }

    @Override
    String getBaseClass() {
        return "com.facebook.FacebookSdk";
    }

    @Override
    public int getConsentMessage() {
        return R.string.facebook_sdk_consent_msg;
    }
    @Override
    public int getName() {
        return R.string.facebook_sdk;
    }
}
