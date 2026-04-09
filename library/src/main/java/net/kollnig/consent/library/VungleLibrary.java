package net.kollnig.consent.library;

import androidx.annotation.NonNull;

import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class VungleLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "vungle";

    @Override
    public @NonNull
    String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        try {
            Class<?> baseClass = Class.forName("com.vungle.warren.Vungle");
            Class<?> consentClass = Class.forName("com.vungle.warren.Vungle$Consent");

            Object optIn = null;
            Object optOut = null;
            for (Object enumConstant : Objects.requireNonNull(consentClass.getEnumConstants())) {
                Method m = consentClass.getMethod("name");
                if (String.valueOf(m.invoke(enumConstant)).equals("OPTED_IN"))
                    optIn = enumConstant;

                if (String.valueOf(m.invoke(enumConstant)).equals("OPTED_OUT"))
                    optOut = enumConstant;
            }

            if (optIn == null || optOut == null)
                throw new LibraryInteractionException("Could not retrieve consent objects.");

            Method updateConsentStatus = baseClass.getMethod("updateConsentStatus", consentClass, String.class);
            updateConsentStatus.invoke(null, consent ? optIn : optOut, "1.0.0");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new LibraryInteractionException("Could not save settings to Vungle.");
        }
    }

    @Override
    String getBaseClass() {
        return "com.vungle.warren.Vungle";
    }

    @Override
    public int getConsentMessage() {
        return R.string.vungle_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.vungle;
    }
}
