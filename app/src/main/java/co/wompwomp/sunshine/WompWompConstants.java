package co.wompwomp.sunshine;

import co.wompwomp.sunshine.provider.FeedContract;

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
    public static final int COLUMN_ANNOTATION = 13;
    public static final int COLUMN_LIST_TYPE = 14;
    public static final int COLUMN_FEATURED_PRIORITY = 15;

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
    public static final String WOMPWOMP_ANNOTATION = "n";
    public static final String WOMPWOMP_FEATURED_PRIORITY = "r";

    public static final String LIST_TYPE_HOME = "home";
    public static final String LIST_TYPE_FEATURED = "featured";

    public static final String ANNOTATION_ALL_TIME_POPULAR = "A";
    public static final String ANNOTATION_TRENDING_THIS_WEEK = "W";
    public static final String ANNOTATION_TRENDING_TODAY = "D";
    public static final String ANNOTATION_BEST_OF_THIS_MONTH = "BC";
    public static final String ANNOTATION_BEST_OF_PREVIOUS_MONTH = "B";

    // Constants for entry id for prompt cards
    public static final String WOMPWOMP_CTA_SHARE = "CTA_SHARE";
    public static final String WOMPWOMP_CTA_RATE = "CTA_RATE";
    public static final String WOMPWOMP_CTA_UPGRADE = "CTA_UPGRADE";
    public static final String[] WOMPWOMP_CTA_LIST = {WOMPWOMP_CTA_SHARE, WOMPWOMP_CTA_RATE, WOMPWOMP_CTA_UPGRADE};

    // item fetch limit/cursor
    public static final String SYNC_METHOD = "sync_method";
    public static final String SYNC_CURSOR = "sync_cursor";
    public static final int SYNC_NUM_SUBSET_ITEMS = 20;
    public static final int SYNC_NUM_ALL_ITEMS = 0;

    public enum SyncMethod {
        EXISTING_AND_NEW_ABOVE_LOW_CURSOR, /* insert and update db in automatic background sync scenario */
        ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_AUTO_NOTIFIER_SERVICE, /* insert only into db before push notification */
        ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_AUTO, /* insert only into db in in-app refresh scenario */
        ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_USER, /* user initiated refresh */
        SUBSET_OF_LATEST_ITEMS_NO_CURSOR, /* insert and update db on app open */
        SUBSET_OF_ITEMS_BELOW_LOW_CURSOR, /* insert only into db in in-app scroll down to bottom scenario */
        ALL_FEATURED_ITEMS, /* insert and update db on app open */
        SYNC_METHOD_NONE
    }

    // Preferences
    public static final String PREF_APP_RESUMED_FROM_BG = "app_resumed_from_bg";
    public static final String PREF_LAST_LOGGED_IN_TIMESTAMP = "last_logged_in_timestamp";
    public static final String PREF_SHARE_LIKE_COUNTER = "share_like_counter";
    public static final String PREF_NOTIFICATION_ALARM_TIME = "notification_alarm_time";
    public static final Integer DEFAULT_SHARE_APP_THRESHOLD = 4;

    public static final String LIKES_FILENAME = "wwlikes.ser";
    public static final String CONTENT_NOTIFICATION = "/topics/content";
    public static final String CTA_SHARE_NOTIFICATION = "/topics/cta_share";
    public static final String SYNC_NOTIFICATION = "/topics/sync";
    public static final String CTA_RATE_NOTIFICATION = "/topics/cta_rate";
    public static final String CTA_UPGRADE_NOTIFICATION = "/topics/cta_upgrade";
    public static final String REMOVE_ALL_CTAS_NOTIFICATION = "/topics/remove_all_ctas";
    public static final String INIT_NOTIF_ALARM = "/topics/init_notification_alarm";
    public static final String[] NOTIFICATION_TOPICS = {
            CONTENT_NOTIFICATION,
            SYNC_NOTIFICATION,
            CTA_SHARE_NOTIFICATION,
            CTA_RATE_NOTIFICATION,
            REMOVE_ALL_CTAS_NOTIFICATION,
            CTA_UPGRADE_NOTIFICATION,
            INIT_NOTIF_ALARM
    };

    public static final Integer MAX_VIDEOS_FILES_TO_RETAIN = 100;

    // Push Notification defaults
    public static final int DEFAULT_PUSH_NOTIFY_HOUR = 8;
    public static final int DEFAULT_PUSH_NOTIFY_MINUTE = 0;
    public static final int DEFAULT_PUSH_NOTIFY_INTERVAL_IN_HOURS = 24;
    public static final String INIT_NOTIFICATION_ALARM = "init_notification_alarm";
    public static final String PUSH_NOTIFICATION = "push_notification";
    public static final String SYNC_COMPLETE = "sync_complete";

    // Google Tag Manager stuff
    public static final String GTM_CONTAINER_ID = "GTM-PV558G";
    public static final String GTM_NOTIFICATION_HOUR = "notificationHour";
    public static final String GTM_NOTIFICATION_MINUTE = "notificationMinute";
    public static final String GTM_NOTIFICATION_INTERVAL_IN_HOURS = "notificationIntervalInHours";

    public static final String[] PROJECTION =  {
            FeedContract.Entry._ID,
            FeedContract.Entry.COLUMN_NAME_ENTRY_ID,
            FeedContract.Entry.COLUMN_NAME_IMAGE_SOURCE_URI,
            FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT,
            FeedContract.Entry.COLUMN_NAME_FAVORITE,
            FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES,
            FeedContract.Entry.COLUMN_NAME_NUM_SHARES,
            FeedContract.Entry.COLUMN_NAME_CREATED_ON,
            FeedContract.Entry.COLUMN_NAME_CARD_TYPE,
            FeedContract.Entry.COLUMN_NAME_AUTHOR,
            FeedContract.Entry.COLUMN_NAME_VIDEOURI,
            FeedContract.Entry.COLUMN_NAME_NUM_PLAYS,
            FeedContract.Entry.COLUMN_NAME_FILE_SIZE,
            FeedContract.Entry.COLUMN_NAME_ANNOTATION,
            FeedContract.Entry.COLUMN_NAME_LIST_TYPE,
            FeedContract.Entry.COLUMN_NAME_FEATURED_PRIORITY
    };

    public static final String HOME_LIST_SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_LIST_TYPE +
            " IS NULL OR " + FeedContract.Entry.COLUMN_NAME_LIST_TYPE + " = '" +
            WompWompConstants.LIST_TYPE_HOME + "' OR " + FeedContract.Entry.COLUMN_NAME_LIST_TYPE + " = '' )";

    public static final String FEATURED_LIST_SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_LIST_TYPE + " = '" +
            WompWompConstants.LIST_TYPE_FEATURED + "' )";

    public static final String ACTION_FINISHED_SYNC = "co.wompwomp.sunshine.ACTION_FINISHED_SYNC";
    public static final Integer MAX_NUM_PREFETCH_VIDEOS = 3;

    public static final int HOME_LOADER_ID = 0;
    public static final int FEATURED_LOADER_ID = 1;
}