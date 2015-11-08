package com.agni.sunshine;

import android.database.Cursor;

/**
 * Created by kowlgi on 10/29/15.
 */
public class MyListItem {
    private Integer _id; // database id
    private String id; //  globally unique item id
    private String imageSourceUri;
    private String quoteText;
    private Boolean favorite;
    private Integer numFavorites;
    private Integer numShares;
    private String createdOn;
    private Integer cardType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageSourceUri() {
        return imageSourceUri;
    }

    public void setImageSourceUri(String imageSourceUri) {
        this.imageSourceUri = imageSourceUri;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
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

    public void setNumShares(Integer numShares) {
        this.numShares = numShares;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public Integer get_id() {
        return _id;
    }

    public void set_id(Integer _id) {
        this._id = _id;
    }

    public Integer getCardType() {
        return cardType;
    }

    public void setCardType(Integer cardType) {
        this.cardType = cardType;
    }

    public static MyListItem fromCursor(Cursor c) {
        MyListItem myItem = new MyListItem();
        myItem.set_id(c.getInt(AgniConstants.COLUMN_ID));
        myItem.setId(c.getString(AgniConstants.COLUMN_ENTRY_ID));
        myItem.setImageSourceUri(c.getString(AgniConstants.COLUMN_IMAGE_SOURCE_URI));
        myItem.setQuoteText(c.getString(AgniConstants.COLUMN_QUOTE_TEXT));
        myItem.setFavorite(c.getInt(AgniConstants.COLUMN_FAVORITE) > 0);
        myItem.setNumFavorites(c.getInt(AgniConstants.COLUMN_NUM_FAVORITES));
        myItem.setNumShares(c.getInt(AgniConstants.COLUMN_NUM_SHARES));
        myItem.setCreatedOn(c.getString(AgniConstants.COLUMN_CREATED_ON));
        myItem.setCardType(c.getInt(AgniConstants.COLUMN_CARD_TYPE));
        return myItem;
    }
}
