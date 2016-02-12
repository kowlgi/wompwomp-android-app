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

package co.wompwomp.sunshine;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import co.wompwomp.sunshine.provider.FeedContract;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Define a sync adapter for the app.
 *
 * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "WompwompSyncAdapter";

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;

    /**
     * Project used when querying content provider. Returns all known fields.
     */
    private static final String[] PROJECTION = new String[] {
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
            FeedContract.Entry.COLUMN_NAME_NUM_PLAYS};

    /* Projection used for obtaining high and low cursors for fetching feed data */
    final String[] CURSOR_PROJECTION = new String[]{
            FeedContract.Entry.COLUMN_NAME_CREATED_ON
    };

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     .
     *
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     *
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        WompWompConstants.SyncMethod syncMethod = WompWompConstants.SyncMethod.SYNC_METHOD_NONE;
        InputStream stream = null;
        try {
            String syncMethodStr = extras.getString(WompWompConstants.SYNC_METHOD);
            if(syncMethodStr == null) {
                // this happens when user enables 'sync' now from Settings>Accounts
                syncMethod = WompWompConstants.SyncMethod.EXISTING_AND_NEW_ABOVE_LOW_CURSOR;
            }
            else {
                syncMethod = WompWompConstants.SyncMethod.valueOf(syncMethodStr);
            }

            int limit = 0;
            boolean updateAndDeleteStaleItems = true;
            String cursor = null, params = "", cursorInclusive = null;

            Cursor c = mContentResolver.query(
                    FeedContract.Entry.CONTENT_URI,
                    CURSOR_PROJECTION,
                    null,
                    null,
                    FeedContract.Entry.COLUMN_NAME_CREATED_ON + " DESC");

            if((c == null) || (c.getCount() < 1) ||
                    syncMethod == WompWompConstants.SyncMethod.SUBSET_OF_LATEST_ITEMS_NO_CURSOR) {
                /* insert and update db happens on app open */
                limit = WompWompConstants.SYNC_NUM_SUBSET_ITEMS;
                updateAndDeleteStaleItems =  true;
            }
            else if (syncMethod == WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR) {
                /* insert only into db in in-app refresh scenario */
                limit = WompWompConstants.SYNC_NUM_ALL_ITEMS;
                c.moveToFirst();
                cursor = c.getString(0);
                updateAndDeleteStaleItems = false;
            }
            else if(syncMethod == WompWompConstants.SyncMethod.SUBSET_OF_ITEMS_BELOW_LOW_CURSOR) {
                /* insert only into db in in-app scroll down to bottom scenario */
                limit = WompWompConstants.SYNC_NUM_SUBSET_ITEMS * -1; /* negative limit implies items below cursor */
                c.moveToLast();
                cursor = c.getString(0);
                updateAndDeleteStaleItems = false;
            }
            else if(syncMethod == WompWompConstants.SyncMethod.EXISTING_AND_NEW_ABOVE_LOW_CURSOR) {
                /* insert and update db happens on app resume */
                limit = WompWompConstants.SYNC_NUM_ALL_ITEMS;
                c.moveToLast();
                cursor = c.getString(0);
                updateAndDeleteStaleItems = true;
                cursorInclusive = "yes";
            }
            if(c != null) {
                c.close();
            }

            params += "?limit=" + Integer.valueOf(limit).toString();
            if(cursor != null) {
                params += "&cursor=" + cursor;
            }
            if(cursorInclusive != null) {
                params+= "&cursorInclusive=" + cursorInclusive;
            }
            final URL location = new URL(FeedContract.FEED_URL + params);
            stream = downloadUrl(location);
            updateLocalFeedData(stream, syncResult, updateAndDeleteStaleItems);
            if(stream != null) {
                stream.close();
                stream = null;
            }
        } catch (MalformedURLException | XmlPullParserException | ParseException e) {
            syncResult.stats.numParseExceptions++;
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
        } catch (RemoteException | OperationApplicationException e) {
            syncResult.databaseError = true;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            Timber.i("Sending sync intent: " + syncMethod.name());
            Intent intent = new Intent(MainActivity.ACTION_FINISHED_SYNC);
            intent.putExtra(WompWompConstants.SYNC_METHOD, syncMethod.name());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    /**
     * Read XML from an input stream, storing it into the content provider.
     *
     * <p>This is where incoming data is persisted, committing the results of a sync. In order to
     * minimize (expensive) disk operations, we compare incoming data with what's already in our
     * database, and compute a merge. Only changes (insert/update/delete) will result in a database
     * write.
     *
     * <p>As an additional optimization, we use a batch operation to perform all database writes at
     * once.
     *
     * <p>Merge strategy:
     * 1. Get cursor to all items in feed<br/>
     * 2. For each item, check if it's in the incoming data.<br/>
     *    a. YES: Remove from "incoming" list. Check if data has mutated, if so, perform
     *            database UPDATE.<br/>
     *    b. NO: Schedule DELETE from database.<br/>
     * (At this point, incoming database only contains missing items.)<br/>
     * 3. For any items remaining in incoming list, ADD to database.
     */
    public void updateLocalFeedData(final InputStream stream,
                                    final SyncResult syncResult,
                                    final boolean updateAndDeleteStaleItems)
            throws IOException, XmlPullParserException, RemoteException,
            OperationApplicationException, ParseException {
        final FeedParser feedParser = new FeedParser();
        final ContentResolver contentResolver = getContext().getContentResolver();
        final List<FeedParser.Entry> entries = feedParser.parse(stream);

        ArrayList<ContentProviderOperation> batch;
        batch = new ArrayList<>();

        // Build hash table of incoming entries
        HashMap<String, FeedParser.Entry> entryMap;
        entryMap = new HashMap<String, FeedParser.Entry>();
        for (FeedParser.Entry e : entries) {
            entryMap.put(e.id, e);
        }

        // Get list of all items

        Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, PROJECTION, null, null, null);
        assert c != null;

        // Update stale data
        if(updateAndDeleteStaleItems) {
            int id;
            String entryId;
            Integer numFavorites;
            Integer numShares;
            String author;
            String imageSourceUri;
            String quoteText;
            String videoUri;
            Integer numPlays;

            while (c.moveToNext()) {
                syncResult.stats.numEntries++;
                id = c.getInt(WompWompConstants.COLUMN_ID);
                entryId = c.getString(WompWompConstants.COLUMN_ENTRY_ID);
                numFavorites = c.getInt(WompWompConstants.COLUMN_NUM_FAVORITES);
                numShares = c.getInt(WompWompConstants.COLUMN_NUM_SHARES);
                author = c.getString(WompWompConstants.COLUMN_AUTHOR);
                imageSourceUri = c.getString(WompWompConstants.COLUMN_IMAGE_SOURCE_URI);
                quoteText = c.getString(WompWompConstants.COLUMN_QUOTE_TEXT);
                videoUri = c.getString(WompWompConstants.COLUMN_VIDEOURI);
                numPlays = c.getInt(WompWompConstants.COLUMN_NUM_PLAYS);

                FeedParser.Entry match = entryMap.get(entryId);
                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later.
                    entryMap.remove(entryId);
                    // Check to see if the entry needs to be updated
                    Uri existingUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                            .appendPath(Integer.toString(id)).build();
                    if ((match.numFavorites != numFavorites) ||
                            (match.numShares != numShares) ||
                            (match.author != author) ||
                            (match.imageSourceUri != imageSourceUri) ||
                            (match.quoteText != quoteText) ||
                            (match.videoUri != videoUri) ||
                            (match.numPlays != numPlays)){
                        // Update existing record
                        batch.add(ContentProviderOperation.newUpdate(existingUri)
                                .withValue(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, match.numFavorites)
                                .withValue(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, match.numShares)
                                .withValue(FeedContract.Entry.COLUMN_NAME_AUTHOR, match.author)
                                .withValue(FeedContract.Entry.COLUMN_NAME_IMAGE_SOURCE_URI, match.imageSourceUri)
                                .withValue(FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT, match.quoteText)
                                .withValue(FeedContract.Entry.COLUMN_NAME_VIDEOURI, match.videoUri)
                                .withValue(FeedContract.Entry.COLUMN_NAME_NUM_PLAYS, match.numPlays)
                                .build());
                        syncResult.stats.numUpdates++;
                    } else {
                        //no action
                    }
                } else {
                    // Entry doesn't exist on server. Remove it from the database if it's not a CTA card
                    if (Arrays.asList(WompWompConstants.WOMPWOMP_CTA_LIST).contains(entryId)) {
                        // It's a CTA card, don't remove it
                    } else {
                        Uri deleteUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(Integer.toString(id)).build();
                        batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                        syncResult.stats.numDeletes++;
                    }
                }
            }
        }
        c.close();

        // Add new items
        for (FeedParser.Entry e : entryMap.values()) {
            batch.add(ContentProviderOperation.newInsert(FeedContract.Entry.CONTENT_URI)
                    .withValue(FeedContract.Entry.COLUMN_NAME_ENTRY_ID, e.id)
                    .withValue(FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT, e.quoteText)
                    .withValue(FeedContract.Entry.COLUMN_NAME_IMAGE_SOURCE_URI, e.imageSourceUri)
                    .withValue(FeedContract.Entry.COLUMN_NAME_FAVORITE, 0)
                    .withValue(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, e.numFavorites)
                    .withValue(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, e.numShares)
                    .withValue(FeedContract.Entry.COLUMN_NAME_CREATED_ON, e.createdOn)
                    .withValue(FeedContract.Entry.COLUMN_NAME_CARD_TYPE,
                            e.videoUri.length() > 0 ? WompWompConstants.TYPE_VIDEO_CONTENT_CARD: WompWompConstants.TYPE_CONTENT_CARD)
                    .withValue(FeedContract.Entry.COLUMN_NAME_AUTHOR, e.author)
                    .withValue(FeedContract.Entry.COLUMN_NAME_VIDEOURI, e.videoUri)
                    .withValue(FeedContract.Entry.COLUMN_NAME_NUM_PLAYS, e.numPlays)
                    .build());
            syncResult.stats.numInserts++;
        }
        mContentResolver.applyBatch(FeedContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(
                FeedContract.Entry.CONTENT_URI, // URI where data was modified
                null,                           // No local observer
                false);                         // IMPORTANT: Do not sync to network
        // This sample doesn't support uploads, but if *your* code does, make sure you set
        // syncToNetwork=false in the line above to prevent duplicate syncs.
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    private InputStream downloadUrl(final URL url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(new Request.Builder().url(url).get().build());
        Response response = call.execute();
        return response.body().byteStream();
    }
}
