/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.wompwomp.sunshine.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import co.wompwomp.sunshine.BuildConfig;

/**
 * Field and table name constants for
 * {@link co.wompwomp.sunshine}.
 */
public class FeedContract {
    private FeedContract() {
    }

    public static final String BASE_URL = "http://54.201.227.101:3000";
    //Deprecated: public static final String FEED_URL = BASE_URL + "/i";
    public static final String FEED_URL = BASE_URL + "/iv";
    public static final String ITEM_VIEW_URL = BASE_URL + "/v/";
    public static final String ITEM_FAVORITE_URL = BASE_URL + "/f/";
    public static final String ITEM_UNFAVORITE_URL = BASE_URL + "/uf/";
    public static final String ITEM_SHARE_URL = BASE_URL + "/s/";
    public static final String ITEM_PLAY_URL = BASE_URL + "/p/";
    public static final String APP_INSTALLED_URL = BASE_URL + "/in";
    public static final String APP_OPENED_URL = BASE_URL + "/op";

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID;

    /**
     * Base URI. (content://com.example.android.basicsyncadapter)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path component for "entry"-type resources..
     */
    private static final String PATH_ENTRIES = "entries";

    /**
     * Columns supported by "entries" records.
     */
    public static class Entry implements BaseColumns {
        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.wompwomp.entries";
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.wompwomp.entry";

        /**
         * Fully qualified URI for "entry" resources.
         */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ENTRIES).build();

        /**
         * Table name where records are stored for "entry" resources.
         */
        public static final String TABLE_NAME = "entry";

        /**
         * Unique wompwomp ID, not related to database entry _ID.
         */
        public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
        /**
         * Source for image
         */
        public static final String COLUMN_NAME_IMAGE_SOURCE_URI = "image_source_uri";
        /**
         * Source for image
         */
        public static final String COLUMN_NAME_QUOTE_TEXT = "quote_text";
        /**
         * Whether the entry has been favorited by the user or not
         */
        public static final String COLUMN_NAME_FAVORITE = "favorite";
        /**
         * Number of times the entry has been favorited
         */
        public static final String COLUMN_NAME_NUM_FAVORITES = "num_favorites";
        /**
         * Number of times the entry has been shared
         */
        public static final String COLUMN_NAME_NUM_SHARES = "num_shares";
        /**
         * Timestamp when the entry was added to the backend
         */
        public static final String COLUMN_NAME_CREATED_ON = "created_on";
        /**
         * Type of card: content/rate/share card
         */
        public static final String COLUMN_NAME_CARD_TYPE = "card_type";
        /**
         * Item author
         */
        public static final String COLUMN_NAME_AUTHOR = "author";
        /**
         * Link to video
         */
        public static final String COLUMN_NAME_VIDEOURI = "videouri";
        /**
         * Number of times the video has been played
         */
        public static final String COLUMN_NAME_NUM_PLAYS = "num_plays";

    }
}