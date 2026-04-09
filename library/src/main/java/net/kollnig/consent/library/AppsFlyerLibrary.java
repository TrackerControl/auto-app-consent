package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppsFlyerLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "appsflyer";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        try {
            Class<?> abstractBaseClass = findBaseClass();
            Method getInstance = abstractBaseClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method stop = abstractBaseClass.getMethod("stop", boolean.class, Context.class);
            stop.invoke(instance, !consent, getContext());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    String getBaseClass() {
        return "com.appsflyer.AppsFlyerLib";
    }

    @Override
    public int getConsentMessage() {
        return R.string.appsflyer_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.appsflyer;
    }
}
