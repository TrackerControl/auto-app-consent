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
        Class baseClass = findBaseClass();
        if (baseClass != null) {
            try {
                // IronSource.setConsent(false);
                Object[] arglist = {consent};
                Method setConsent = baseClass.getMethod("setConsent", boolean.class);
                setConsent.invoke(null, arglist);

                // IronSource.setMetaData("do_not_sell","true");
                Object[] arglist2 = {"do_not_sell", Boolean.toString(!consent)};
                Method setMetaData = baseClass.getMethod("setMetaData", String.class, String.class);
                setMetaData.invoke(null, arglist2);

                // IronSource.setMetaData("is_deviceid_optout","true");
                arglist2[0] = "is_deviceid_optout";
                setMetaData.invoke(null, arglist2);
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to Facebook SDK.");
            }
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
