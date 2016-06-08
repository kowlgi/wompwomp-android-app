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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.Utils;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.gcm.GcmListenerService;

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
        if(from == null) return; // this should ideally never happen but we've seen a few crashes reported on crashlytics

        if (from.startsWith(WompWompConstants.CONTENT_NOTIFICATION)) {
            if(true) return; // remove this statement after the server stops sending regular content notifications
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

            if(pushNotify) {
                pushNotification(message, imageUri, itemId);
                Answers.getInstance().logCustom(new CustomEvent("Posted push notification")
                        .putCustomAttribute("itemid", itemId));
            }
        } else if (from.startsWith(WompWompConstants.SYNC_NOTIFICATION)){
            SyncUtils.TriggerSync(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_AUTO);
        } else if(from.startsWith(WompWompConstants.CTA_SHARE_NOTIFICATION)) {
            String timestamp = data.getString("message");
            // push 'share now' card to feed
            getContentResolver().insert(FeedContract.Entry.CONTENT_URI, Utils.populateContentValues(WompWompConstants.TYPE_SHARE_CARD, timestamp, null));
        } else if(from.startsWith(WompWompConstants.CTA_RATE_NOTIFICATION)) {
            String timestamp = data.getString("message");
            // push 'rate now' card to feed
            getContentResolver().insert(FeedContract.Entry.CONTENT_URI, Utils.populateContentValues(WompWompConstants.TYPE_RATE_CARD, timestamp, null));
        } else if(from.startsWith(WompWompConstants.CTA_UPGRADE_NOTIFICATION)) {
            String timestamp = data.getString("message");
            String versionCode = data.getString("versionCode");

            if(versionCode == null) {
                versionCode = "0";
            }

            if(BuildConfig.VERSION_CODE < Integer.valueOf(versionCode)) {
                // push 'upgrade now' card to feed
                getContentResolver().insert(FeedContract.Entry.CONTENT_URI, Utils.populateContentValues(WompWompConstants.TYPE_UPGRADE_CARD, timestamp, versionCode));
            }
        } else if(from.startsWith(WompWompConstants.REMOVE_ALL_CTAS_NOTIFICATION)) {
            Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
            String[] share_args = new String[] { WompWompConstants.WOMPWOMP_CTA_SHARE};
            String[] rate_args = new String[] { WompWompConstants.WOMPWOMP_CTA_RATE};
            String[] upgrade_args = new String[] { WompWompConstants.WOMPWOMP_CTA_UPGRADE};
            getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", share_args);
            getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", rate_args);
            getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", upgrade_args);
        } else if(from.startsWith(WompWompConstants.INIT_NOTIF_ALARM)) {
            Intent pushNotificationIntent = new Intent(this, NotifierService.class);
            pushNotificationIntent.setAction(WompWompConstants.INIT_NOTIFICATION_ALARM);
            startService(pushNotificationIntent);
        }
        else {
            // normal downstream message.
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
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_wompwomp_newicon);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_wompwomp_newicon)
                .setLargeIcon(largeIcon)
                .setContentTitle(appName)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        Bitmap aBigBitmap = Utils.getBitmap(imageUri);
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
}
