package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppLovinLibrary extends Library {
    @NonNull
    @Override
    public String getId() {
        return "applovin";
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class baseClass = findBaseClass();
        if (baseClass != null) {
            try {
                // Call AppLovinPrivacySettings.setDoNotSell( consent, context );
                Method setDoNotSell = baseClass.getMethod("setDoNotSell", boolean.class, Context.class);
                setDoNotSell.invoke(null, !consent, getContext());

                // Call AppLovinPrivacySettings.setHasUserConsent( consent, context );
                Method setHasUserConsent = baseClass.getMethod("setHasUserConsent", boolean.class, Context.class);
                setHasUserConsent.invoke(null, consent, getContext());

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to Applovin.");
            }
        }
    }

    @Override
    String getBaseClass() {
        return "com.applovin.sdk.AppLovinPrivacySettings";
    }

    @Override
    public int getConsentMessage() {
        return R.string.applovin_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.applovin;
    }
}
