package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public class AdColonyLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "adcolony";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @NonNull
    private static Class<?> getOptionsClass() throws ClassNotFoundException {
        return Class.forName("com.adcolony.sdk.AdColonyAppOptions");
    }

    private @NonNull
    static Object getAppOptions(Object options, boolean consent) {
        try {
            if (options == null) {
                Class<?> baseClass = Class.forName("com.adcolony.sdk.AdColony");
                Method getAppOptions = baseClass.getMethod("getAppOptions");
                options = getAppOptions.invoke(null);
            }

            Class<?> optionsClass = getOptionsClass();
            if (options == null) {
                options = optionsClass.getConstructor().newInstance();
            }

            String GDPR = (String) optionsClass.getDeclaredField("GDPR").get(null);

            Method setPrivacyFrameworkRequired = optionsClass.getMethod("setPrivacyFrameworkRequired", String.class, boolean.class);
            setPrivacyFrameworkRequired.invoke(options, GDPR, true);

            String consentString = consent ? "1" : "0";
            Method setPrivacyConsentString = optionsClass.getMethod("setPrivacyConsentString", String.class, String.class);
            setPrivacyConsentString.invoke(options, GDPR, consentString);

            return options;
        } catch (Exception e) {
            throw new RuntimeException("Failed to interact with AdColony SDK.");
        }
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        try {
            Object options = getAppOptions(null, consent);

            Method setAppOptions = baseClass.getMethod("setAppOptions", options.getClass());
            setAppOptions.invoke(null, options);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new LibraryInteractionException("Could not save settings to AdColony.");
        }
    }

    @Override
    String getBaseClass() {
        return "com.adcolony.sdk.AdColony";
    }

    @Override
    public int getConsentMessage() {
        return R.string.adcolony_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.adcolony;
    }
}
