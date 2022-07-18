package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kollnig.consent.ConsentManager;

public abstract class Library {
    private final Context context;

    public Library(Context context) throws LibraryInteractionException {
        this.context = context;

        if (getId().contains(":"))
            throw new RuntimeException("id cannot contain ':'");

        initialise();
    }

    Context getContext() {
        return context;
    }

    abstract public @NonNull
    String getId();

    @Nullable
    Boolean hasConsent() {
        return ConsentManager.hasConsent(context, getId());
    }

    void initialise() throws LibraryInteractionException {
        Boolean consent = hasConsent();

        if (consent == null
                || consent == false)
            saveConsent(false);
    };

    abstract public void saveConsent(boolean consent) throws LibraryInteractionException;

    public boolean isPresent() {
        return findBaseClass() != null;
    }

    abstract String getBaseClass();

    @Nullable
    Class findBaseClass() {
        try {
            return Class.forName(getBaseClass());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    abstract public int getConsentMessage();
}
