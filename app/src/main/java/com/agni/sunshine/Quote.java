package com.agni.sunshine;

/**
 * Created by kowlgi on 9/25/15.
 */
public class Quote {
    private String imageSourceUri;
    private String quoteText;
    private String imageDisplayUri;

    public Quote(String sourceUri, String text, String displayUri) {
        this.imageSourceUri = sourceUri;
        this.quoteText = text;
        this.imageDisplayUri = displayUri;
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
