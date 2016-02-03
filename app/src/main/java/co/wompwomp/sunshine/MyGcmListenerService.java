/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.wompwomp.sunshine;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import co.wompwomp.sunshine.provider.FeedContract;
import timber.log.Timber;

import com.google.android.gms.gcm.GcmListenerService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyGcmListenerService extends GcmListenerService {
    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (from.startsWith(WompWompConstants.CONTENT_NOTIFICATION)) {
            String message = data.getString("message");
            String imageUri = data.getString("imageuri");
            String itemId = data.getString("itemid");
            String versionCode = data.getString("versionCode");
            String versionCondition = data.getString("versionCondition");

            if(versionCondition == null || versionCode == null) {
                versionCondition = ">";
                versionCode = "0";
            }

            boolean pushNotify = false;
            if(versionCondition.equals(">")){
                pushNotify = (BuildConfig.VERSION_CODE > Integer.valueOf(versionCode));
            } else if (versionCondition.equals("<")){
                pushNotify = (BuildConfig.VERSION_CODE < Integer.valueOf(versionCode));
            }
            Timber.d("wompwomp notify", "versionCode: " + versionCode + ", versionCondition: "
                    + versionCondition + ", pushNotify: " + pushNotify +
                    ", build version: " + BuildConfig.VERSION_CODE);

            if(pushNotify) {
                pushNotification(message, imageUri, itemId);
                Timber.i("Pushed notification to user. Text: %s, ImageUri: %s, ItemId: %s", message, imageUri, itemId);
            }
        } else if (from.startsWith(WompWompConstants.SYNC_NOTIFICATION)){
            SyncUtils.TriggerSync(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR);
            Timber.i("Initiated sync latest items");
        } else if(from.startsWith(WompWompConstants.CTA_SHARE_NOTIFICATION)) {
            String timestamp = data.getString("message");
            Timber.i("Pushed Share CTA to feed");
            // push 'share now' card to feed
            getContentResolver().insert(FeedContract.Entry.CONTENT_URI, populateContentValues(WompWompConstants.TYPE_SHARE_CARD, timestamp));
        } else if(from.startsWith(WompWompConstants.CTA_RATE_NOTIFICATION)) {
            String timestamp = data.getString("message");
            // push 'rate now' card to feed
            getContentResolver().insert(FeedContract.Entry.CONTENT_URI, populateContentValues(WompWompConstants.TYPE_RATE_CARD, timestamp));
            Timber.i("Pushed Rate CTA to feed");
        } else if(from.startsWith(WompWompConstants.CTA_UPGRADE_NOTIFICATION)) {
            String timestamp = data.getString("message");
            String versionCode = data.getString("versionCode");

            if(versionCode == null) {
                versionCode = "0";
            }

            if(BuildConfig.VERSION_CODE < Integer.valueOf(versionCode)) {
                // push 'upgrade now' card to feed
                getContentResolver().insert(FeedContract.Entry.CONTENT_URI, populateContentValues(WompWompConstants.TYPE_UPGRADE_CARD, timestamp));
                Timber.i("Pushed Upgrade CTA to feed");
            }
        } else if(from.startsWith(WompWompConstants.REMOVE_ALL_CTAS_NOTIFICATION)) {
            Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
            String[] share_args = new String[] { WompWompConstants.WOMPWOMP_CTA_SHARE};
            String[] rate_args = new String[] { WompWompConstants.WOMPWOMP_CTA_RATE};
            String[] upgrade_args = new String[] { WompWompConstants.WOMPWOMP_CTA_UPGRADE};
            getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", share_args);
            getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", rate_args);
            getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", upgrade_args);
            Timber.i("Removed all CTAs from feed");
        } else {
            // normal downstream message.
            Timber.i("GOT NOTIFIED BOUT NOTHIN'");
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void pushNotification(String message, String imageUri, String itemId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("itemid", itemId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String appName = getResources().getString(R.string.app_name);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_trombone)
                .setContentTitle(appName)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        Bitmap aBigBitmap = getBitmap(imageUri);
        if(aBigBitmap != null) {
            NotificationCompat.BigPictureStyle bigStyle = new
                    NotificationCompat.BigPictureStyle();
            bigStyle.setBigContentTitle(appName);
            bigStyle.setSummaryText(message);
            bigStyle.bigPicture(aBigBitmap);
            notificationBuilder.setStyle(bigStyle);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private Bitmap getBitmap(String imageUri) {

        if(imageUri == null) return null;

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        Bitmap aBitmap = null;

        try {
            URL url = new URL(imageUri);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            aBitmap = BitmapFactory.decodeStream(inputStream);

        }catch (IOException e) {
            Timber.e("Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return aBitmap;
    }

    private ContentValues populateContentValues(Integer card_type, String timestamp) {
        ContentValues contentValues = new ContentValues();
        String entry_id = null;
        if(card_type == WompWompConstants.TYPE_RATE_CARD) {
            entry_id = WompWompConstants.WOMPWOMP_CTA_RATE;
        } else if(card_type == WompWompConstants.TYPE_SHARE_CARD) {
            entry_id = WompWompConstants.WOMPWOMP_CTA_SHARE;
        } else if(card_type == WompWompConstants.TYPE_UPGRADE_CARD) {
            entry_id = WompWompConstants.WOMPWOMP_CTA_UPGRADE;
        }

        contentValues.put(FeedContract.Entry.COLUMN_NAME_ENTRY_ID, entry_id);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT, "");
        contentValues.put(FeedContract.Entry.COLUMN_NAME_IMAGE_SOURCE_URI, "");
        contentValues.put(FeedContract.Entry.COLUMN_NAME_FAVORITE, 0);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, 0);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, 0);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_CREATED_ON, timestamp);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_CARD_TYPE, card_type);
        contentValues.put(FeedContract.Entry.COLUMN_NAME_AUTHOR, "");
        return contentValues;
    }
}
