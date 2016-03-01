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
    public static final int COLUMN_AUTHOR = 9;
    public static final int COLUMN_VIDEOURI = 10;
    public static final int COLUMN_NUM_PLAYS = 11;
    public static final int COLUMN_FILE_SIZE = 12;

    /* Card types */
    public static final int TYPE_CONTENT_CARD = 100;
    public static final int TYPE_SHARE_CARD = 101;
    public static final int TYPE_RATE_CARD = 102;
    public static final int TYPE_UPGRADE_CARD = 103;
    public static final int TYPE_VIDEO_CONTENT_CARD = 104;

    // Constants indicting XML element names that we're interested in
    public static final String WOMPWOMP_ID = "i";
    public static final String WOMPWOMP_TEXT = "t";
    public static final String WOMPWOMP_IMAGEURI = "u";
    public static final String WOMPWOMP_CREATEDON = "c";
    public static final String WOMPWOMP_NUMFAVORITES = "f";
    public static final String WOMPWOMP_NUMSHARES = "s";
    public static final String WOMPWOMP_AUTHOR = "a";
    public static final String WOMPWOMP_VIDEOURI = "m";
    public static final String WOMPWOMP_NUMPLAYS = "p";
    public static final String WOMPWOMP_FILESIZE = "z";

    // Constants for entry id for prompt cards
    public static final String WOMPWOMP_CTA_SHARE = "CTA_SHARE";
    public static final String WOMPWOMP_CTA_RATE = "CTA_RATE";
    public static final String WOMPWOMP_CTA_UPGRADE = "CTA_UPGRADE";
    public static final String[] WOMPWOMP_CTA_LIST = {WOMPWOMP_CTA_SHARE, WOMPWOMP_CTA_RATE, WOMPWOMP_CTA_UPGRADE};

    // item fetch limit/cursor
    public static final String SYNC_METHOD = "sync_method";
    public static final int SYNC_NUM_SUBSET_ITEMS = 10;
    public static final int SYNC_NUM_ALL_ITEMS = 0;

    public enum SyncMethod {
        EXISTING_AND_NEW_ABOVE_LOW_CURSOR, /* insert and update db in automatic background sync scenario */
        ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_AUTO, /* insert only into db in in-app refresh scenario */
        ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_USER,
        SUBSET_OF_LATEST_ITEMS_NO_CURSOR, /* insert and update db on app open */
        SUBSET_OF_ITEMS_BELOW_LOW_CURSOR, /* insert only into db in in-app scroll down to bottom scenario */
        SYNC_METHOD_NONE
    }

    public static final String APP_RESUMED_FROM_BG = "app_resumed_from_bg";
    public static final String LIKES_FILENAME = "wwlikes.ser";
    public static final String VIDEO_DOWNLOADS_FILENAME = "wwdownloads.ser";
    public static final String CONTENT_NOTIFICATION = "/topics/content";
    public static final String CTA_SHARE_NOTIFICATION = "/topics/cta_share";
    public static final String SYNC_NOTIFICATION = "/topics/sync";
    public static final String CTA_RATE_NOTIFICATION = "/topics/cta_rate";
    public static final String CTA_UPGRADE_NOTIFICATION = "/topics/cta_upgrade";
    public static final String REMOVE_ALL_CTAS_NOTIFICATION = "/topics/remove_all_ctas";
    public static final String[] NOTIFICATION_TOPICS = {
            CONTENT_NOTIFICATION,
            SYNC_NOTIFICATION,
            CTA_SHARE_NOTIFICATION,
            CTA_RATE_NOTIFICATION,
            REMOVE_ALL_CTAS_NOTIFICATION,
            CTA_UPGRADE_NOTIFICATION
    };

    public static final Integer MAX_VIDEOS_FILES_TO_RETAIN = 10;
}
