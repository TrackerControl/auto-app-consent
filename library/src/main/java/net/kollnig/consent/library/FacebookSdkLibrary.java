package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FacebookSdkLibrary extends Library {
    public FacebookSdkLibrary(Context context) throws LibraryInteractionException {
        super(context);
    }

    @Override
    public @NonNull String getId() {
        return "facebook_sdk";
    }

    @Override
    public void saveConsent(Boolean consent) throws LibraryInteractionException {
        Class baseClass = findBaseClass();
        if (baseClass != null) {
            try {
                // Call FacebookSdk.setAutoInitEnabled(consent);
                Object[] arglist = {consent};
                Method setAutoInitEnabled = baseClass.getMethod("setAutoInitEnabled", boolean.class);
                setAutoInitEnabled.invoke(null, arglist);

                // Call FacebookSdk.fullyInitialize();
                Method fullyInitialize = baseClass.getMethod("fullyInitialize");
                fullyInitialize.invoke(null);

                // Call FacebookSdk.setAutoLogAppEventsEnabled(consent);
                Method setAutoLogAppEventsEnabled = baseClass.getMethod("setAutoLogAppEventsEnabled", boolean.class);
                setAutoLogAppEventsEnabled.invoke(null, arglist);

                // Call FacebookSdk.setAdvertiserIDCollectionEnabled(consent);
                Method setAdvertiserIDCollectionEnabled = baseClass.getMethod("setAdvertiserIDCollectionEnabled", boolean.class);
                setAdvertiserIDCollectionEnabled.invoke(null, arglist);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to Firebase Analytics.");
            }
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
}
