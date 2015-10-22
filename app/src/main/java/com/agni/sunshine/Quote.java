package com.agni.sunshine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by kowlgi on 9/25/15.
 */
public class Quote implements Serializable {
    private String imageSourceUri = null;
    private String quoteText = null;
    private String imageDisplayUri = null;
    private String backgroundColor = null;
    private String bodytextColor = null;
    private Boolean favorite = false;

    public Quote() {
    }

    public String getSourceUri() {
        return imageSourceUri;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public String getDisplayUri() {
        return imageDisplayUri;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setFavorite(Boolean state) {
        this.favorite = state;
    }

    public String getBodytextColor() {
        return bodytextColor;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setBodytextColor(String bodytextColor) {
        this.bodytextColor = bodytextColor;
    }

    public void setSourceUri(String sourceUri) {
        this.imageSourceUri = sourceUri;
    }

    public void setQuoteText(String text) {
        this.quoteText = text;
    }

    public void setDisplayUri(String displayUri) {
        this.imageDisplayUri = displayUri;
    }

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    private void readObject(
            ObjectInputStream aInputStream
    ) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();

        //MUSTFIX: ensure that object state has not been corrupted or tampered with maliciously
    }

    /**
     * This is the default implementation of writeObject.
     * Customise if necessary.
     */
    private void writeObject(
            ObjectOutputStream aOutputStream
    ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }
}
