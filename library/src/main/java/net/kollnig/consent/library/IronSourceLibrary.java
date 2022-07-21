package net.kollnig.consent.library;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IronSourceLibrary extends Library {
    @Override
    public @NonNull
    String getId() {
        return "ironsource";
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        try {
            // IronSource.setConsent(false);
            Method setConsent = baseClass.getMethod("setConsent", boolean.class);
            setConsent.invoke(null, consent);

            // IronSource.setMetaData("do_not_sell","true");
            Method setMetaData = baseClass.getMethod("setMetaData", String.class, String.class);
            setMetaData.invoke(null, "do_not_sell", Boolean.toString(!consent));

            // IronSource.setMetaData("is_deviceid_optout","true");
            setMetaData.invoke(null, "is_deviceid_optout", Boolean.toString(!consent));
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new LibraryInteractionException("Could not save settings to ironSource.");
        }
    }

    @Override
    String getBaseClass() {
        return "com.ironsource.mediationsdk.IronSource";
    }

    @Override
    public int getConsentMessage() {
        return R.string.ironsource_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.ironsource;
    }
}
