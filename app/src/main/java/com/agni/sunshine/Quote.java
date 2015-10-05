package com.agni.sunshine;

/**
 * Created by kowlgi on 9/25/15.
 */
public class Quote {
    private String uri;
    private String quotetext;

    public Quote(String uri, String quotetext) {
        this.uri = uri;
        this.quotetext = quotetext;
    }

    public String getUri() {
        return uri;
    }

    public String getQuotetext() {
        return quotetext;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setQuotetext(String quotetext) {
        this.quotetext = quotetext;
    }
}
