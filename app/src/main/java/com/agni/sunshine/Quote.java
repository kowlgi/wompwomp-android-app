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
    private String displayId = null;
    private Boolean favorite = false;
    private Integer numFavorites = 0;
    private Integer numShares = 0;
    private String createdOn = null;

    public Quote() {
    }

    public String getQuoteText() {
        return quoteText;
    }


    public void setFavorite(Boolean state) {
        this.favorite = state;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public Integer getNumFavorites() {
        return numFavorites;
    }

    public void setNumFavorites(Integer numFavorites) {
        this.numFavorites = numFavorites;
    }

    public Integer getNumShares() {
        return numShares;
    }

    public String getImageSourceUri() {
        return imageSourceUri;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public void setImageSourceUri(String imageSourceUri) {
        this.imageSourceUri = imageSourceUri;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    public void setNumShares(Integer numShares) {
        this.numShares = numShares;
    }

    public void setQuoteText(String text) {
        this.quoteText = text;
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
