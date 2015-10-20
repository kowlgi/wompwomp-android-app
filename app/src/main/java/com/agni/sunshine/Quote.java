package com.agni.sunshine;

/**
 * Created by kowlgi on 9/25/15.
 */
public class Quote {
    private String imageSourceUri;
    private String quoteText;
    private String imageDisplayUri;
    private String backgroundColor;
    private String bodytextColor;


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

    public String getBodytextColor() {
        return bodytextColor;
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
}
