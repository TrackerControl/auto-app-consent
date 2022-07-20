package net.kollnig.consent.library;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kollnig.consent.ConsentManager;

public abstract class Library {
    private Context context;

    public Library() {
        if (getId().contains(":"))
            throw new RuntimeException("id cannot contain ':'");
    }

    Context getContext() {
        return context;
    }

    abstract public @NonNull
    String getId();

    @Nullable
    Boolean hasConsent() {
        return ConsentManager.getInstance().hasConsent(getId());
    }

    public Library initialise(Context context) throws LibraryInteractionException {
        this.context = context;

        Boolean consent = hasConsent();

        if (consent == null
                || consent == false)
            passConsentToLibrary(false);

        return this;
    }

    abstract public void passConsentToLibrary(boolean consent) throws LibraryInteractionException;

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

    abstract public int getName();
}
