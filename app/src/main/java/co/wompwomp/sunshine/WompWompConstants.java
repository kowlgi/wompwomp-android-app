package co.wompwomp.sunshine;

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
    public static final int COLUMN_DISMISS_ITEM = 9;

    /* Card types */
    public static final int TYPE_CONTENT_CARD = 100;
    public static final int TYPE_SHARE_CARD = 101;
    public static final int TYPE_RATE_CARD = 102;

    // Constants indicting XML element names that we're interested in
    public static final String WOMPWOMP_ID = "i";
    public static final String WOMPWOMP_TEXT = "t";
    public static final String WOMPWOMP_IMAGEURI = "u";
    public static final String WOMPWOMP_CREATEDON = "c";
    public static final String WOMPWOMP_NUMFAVORITES = "f";
    public static final String WOMPWOMP_NUMSHARES = "s";

    // Constants for entry id for prompt cards
    public static final String WOMPWOMP_CTA_SHARE = "CTA_SHARE";
    public static final String WOMPWOMP_CTA_RATE = "CTA_RATE";
    public static final String[] WOMPWOMP_CTA_LIST = {WOMPWOMP_CTA_SHARE, WOMPWOMP_CTA_RATE};

    // item fetch limit/cursor
    public static final String SYNC_METHOD = "sync_method";
    public static final int SYNC_NUM_SUBSET_ITEMS = 10;
    public static final int SYNC_NUM_ALL_ITEMS = 0;

    public enum SyncMethod {
        EXISTING_AND_NEW_ABOVE_LOW_CURSOR, /* insert and update db in automatic background sync scenario */
        ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR, /* insert only into db in in-app refresh scenario */
        SUBSET_OF_LATEST_ITEMS_NO_CURSOR, /* insert and update db happens on first app load as
        well or after clearing internal data. db is always empty in this scenario. */
        SUBSET_OF_ITEMS_BELOW_LOW_CURSOR, /* insert only into db in in-app scroll down to bottom scenario */
        SYNC_METHOD_NONE
    }

    public static final String PLAY_STORE_APP_PAGE_URL = "market://details?id=" + BuildConfig.APPLICATION_ID;
}
