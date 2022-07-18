package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppLovinLibrary extends Library {
    public AppLovinLibrary(Context context) throws LibraryInteractionException {
        super(context);
    }

    @NonNull
    @Override
    public String getId() {
        return "applovin";
    }

    @Override
    public void saveConsent(boolean consent) throws LibraryInteractionException {
        Class baseClass = findBaseClass();
        if (baseClass != null) {
            try {
                // Call AppLovinPrivacySettings.setDoNotSell( consent, context );
                Object[] arglist = {!consent, getContext()};
                Method setDoNotSell = baseClass.getMethod("setDoNotSell", boolean.class, Context.class);
                setDoNotSell.invoke(null, arglist);

                // Call AppLovinPrivacySettings.setHasUserConsent( consent, context );
                arglist[0] = consent;
                Method setHasUserConsent = baseClass.getMethod("setHasUserConsent", boolean.class, Context.class);
                setHasUserConsent.invoke(null, arglist);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to Firebase Analytics.");
            }
        }
    }

    @Override
    String getBaseClass() {
        return "com.applovin.sdk.AppLovinPrivacySettings";
    }

    @Override
    public int getConsentMessage() {
        return 0;
    }
}
