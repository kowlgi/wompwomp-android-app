package co.wompwomp.sunshine;

/**
 * Created by kowlgi on 11/5/15.
 */
public class WompWompConstants {
    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_ENTRY_ID = 1;
    public static final int COLUMN_IMAGE_SOURCE_URI = 2;
    public static final int COLUMN_QUOTE_TEXT = 3;
    public static final int COLUMN_FAVORITE = 4;
    public static final int COLUMN_NUM_FAVORITES = 5;
    public static final int COLUMN_NUM_SHARES = 6;
    public static final int COLUMN_CREATED_ON = 7;
    public static final int COLUMN_CARD_TYPE = 8;

    /* Card types */
    public static final int TYPE_CONTENT_CARD = 100;
    public static final int TYPE_SHARE_CARD = 101;
    public static final int TYPE_RATE_CARD = 102;

    // Constants indicting XML element names that we're interested in
    public static final String WOMPWOMP_ID = "id";
    public static final String WOMPWOMP_TEXT = "text";
    public static final String WOMPWOMP_IMAGEURI = "imageuri";
    public static final String WOMPWOMP_CREATEDON = "created_on";
    public static final String WOMPWOMP_NUMFAVORITES = "numfavorites";
    public static final String WOMPWOMP_NUMSHARES = "numshares";

    // Constants for entry id for prompt cards
    public static final String WOMPWOMP_CTA_SHARE = "CTA_SHARE";
    public static final String WOMPWOMP_CTA_RATE = "CTA_RATE";
    public static final String[] WOMPWOMP_CTA_LIST = {WOMPWOMP_CTA_SHARE, WOMPWOMP_CTA_RATE};
}
