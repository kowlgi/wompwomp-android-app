package co.wompwomp.sunshine;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.webkit.URLUtil;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Calendar;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.Utils;
import co.wompwomp.sunshine.util.VideoFileInfo;
import timber.log.Timber;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NotifierService extends IntentService {
    public NotifierService() {
        super("NotifierService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (WompWompConstants.INIT_NOTIFICATION_ALARM.equals(action)) {
                initNotificationAlarm();
            } else if (WompWompConstants.PUSH_NOTIFICATION.equals(action)) {
                handlePushNotify();
            } else if( WompWompConstants.SYNC_COMPLETE.equals(action)) {
                Bundle intentExtras = intent.getExtras();
                String oldContentTimestamp = intentExtras.getString(WompWompConstants.SYNC_CURSOR);
                onSyncComplete(oldContentTimestamp);
            }
        }
    }

    private void initNotificationAlarm() {
        Intent intent = new Intent(this, NotifierService.class);
        intent.setAction(WompWompConstants.PUSH_NOTIFICATION);

        boolean alarmUp = PendingIntent.getService(this,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE) != null;

        // update notification time using google tag manager if google play services exist on this phone
        if (checkPlayServices()) {
            TagManager tagManager = TagManager.getInstance(this);
            PendingResult<ContainerHolder> pending =
                    tagManager.loadContainerPreferNonDefault(WompWompConstants.GTM_CONTAINER_ID,
                            R.raw.gtminfo);

            ContainerHolder containerHolder = pending.await();
            int hour = WompWompConstants.DEFAULT_PUSH_NOTIFY_HOUR, minute = WompWompConstants.DEFAULT_PUSH_NOTIFY_MINUTE;
            Container container = containerHolder.getContainer();
            if (container != null && containerHolder.getStatus().isSuccess()) {
                hour = (int) container.getLong(WompWompConstants.GTM_NOTIFICATION_HOUR);
                minute = (int) container.getLong(WompWompConstants.GTM_NOTIFICATION_MINUTE);
            }

            // To make the default time different than the expected alarm time, we add
            // a minute
            String default_alarm_time = buildTimeString(hour, minute+1);
            String alarm_time = PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .getString(WompWompConstants.PREF_NOTIFICATION_ALARM_TIME, default_alarm_time);

            if(alarmUp && alarm_time.equals(buildTimeString(hour, minute))) return;

            configureAlarmForPushNotification(intent, hour, minute);
        } else {
            // To make the default time different than the expected alarm time, we add
            // a minute
            String default_alarm_time = buildTimeString(WompWompConstants.DEFAULT_PUSH_NOTIFY_HOUR,
                    WompWompConstants.DEFAULT_PUSH_NOTIFY_MINUTE + 1);
            String alarm_time = PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .getString(WompWompConstants.PREF_NOTIFICATION_ALARM_TIME, default_alarm_time);

            if(alarmUp && alarm_time.equals(buildTimeString(WompWompConstants.DEFAULT_PUSH_NOTIFY_HOUR,
                    WompWompConstants.DEFAULT_PUSH_NOTIFY_MINUTE))) return;

            configureAlarmForPushNotification(intent, WompWompConstants.DEFAULT_PUSH_NOTIFY_HOUR,
                    WompWompConstants.DEFAULT_PUSH_NOTIFY_MINUTE);
        }
    }

    private void configureAlarmForPushNotification(Intent intent, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            // If time is in the past, it'll trigger the alarm immediately. To avoid this situation,
            // if the trigger time has already passed we add a day to it.
            calendar.setTimeInMillis(calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY);
        }

        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putString(WompWompConstants.PREF_NOTIFICATION_ALARM_TIME, buildTimeString(
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)))
                .apply();

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent;
        alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
        Timber.i("Notifier Service alarm has been set for this time: " +
                calendar.get(Calendar.HOUR_OF_DAY) + ":" +
                calendar.get(Calendar.MINUTE) + ":" +
                calendar.get(Calendar.SECOND) + ":" +
                calendar.get(Calendar.MILLISECOND) + "(trigger time: " + calendar.getTimeInMillis() +
                ", current time: " + System.currentTimeMillis() + ")");
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return resultCode == ConnectionResult.SUCCESS;
    }

    private void handlePushNotify() {
        DateTime now = DateTime.now();
        DateTime twentyFourHoursAgo = now.minusHours(24);
        String last_logged_in = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(WompWompConstants.PREF_LAST_LOGGED_IN_TIMESTAMP, twentyFourHoursAgo.toString());

        int interval = WompWompConstants.DEFAULT_PUSH_NOTIFY_INTERVAL_IN_HOURS;
        if (checkPlayServices()) {
            TagManager tagManager = TagManager.getInstance(this);
            PendingResult<ContainerHolder> pending =
                    tagManager.loadContainerPreferNonDefault(WompWompConstants.GTM_CONTAINER_ID,
                            R.raw.gtminfo);

            ContainerHolder containerHolder = pending.await();
            Container container = containerHolder.getContainer();

            if (container != null && containerHolder.getStatus().isSuccess()) {
                interval = (int) container.getLong(WompWompConstants.GTM_NOTIFICATION_INTERVAL_IN_HOURS);
            }
        }

        DateTime last_logged_in_dt = new DateTime(last_logged_in);
        Duration duration_since_login = new Duration(last_logged_in_dt, now);
        Timber.i("Interval since last log in: " +
                duration_since_login.getStandardHours() + ":" +
                duration_since_login.getStandardMinutes() + ":" +
                duration_since_login.getStandardSeconds());


        if (duration_since_login.getStandardHours() >= interval) {
            SyncUtils.TriggerRefresh(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_AUTO_NOTIFIER_SERVICE);
        }
    }

    private String buildTimeString(int hour, int minute){
        return Integer.valueOf(hour).toString() + ":" + Integer.valueOf(minute).toString();
    }

    private void onSyncComplete(String oldContentTimestamp) {
        final String SELECTION = FeedContract.Entry.COLUMN_NAME_CREATED_ON +
                " > '" + oldContentTimestamp + "'";

        Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
        Cursor cursor = getApplicationContext().getContentResolver().query(
                uri,
                WompWompConstants.PROJECTION,
                SELECTION,
                null,
                FeedContract.Entry.COLUMN_NAME_CREATED_ON + " desc");

        if (cursor == null || cursor.getCount() <= 0) {
            return; // do nothing
        }else {
            cursor.moveToFirst();
            ArrayList<VideoFileInfo> videoPrefetchList = new ArrayList<>();
            int numVideos = 0;
            do {
                String videoUri = cursor.getString(WompWompConstants.COLUMN_VIDEOURI);
                Integer fileSize = cursor.getInt(WompWompConstants.COLUMN_FILE_SIZE);
                if(videoUri == null || videoUri.length() <= 0 ) continue;

                String videofilename = URLUtil.guessFileName(videoUri, null, null);
                if(Utils.validVideoFile(this, videofilename, fileSize)) continue;

                VideoFileInfo vfi = new VideoFileInfo(videoUri, fileSize);
                videoPrefetchList.add(vfi);
                numVideos++;
            } while(cursor.moveToNext() && numVideos <= WompWompConstants.MAX_NUM_PREFETCH_VIDEOS);
            FileDownloaderService.startDownload(this, videoPrefetchList);
        }

        cursor.moveToFirst();
        String quoteText = cursor.getString(WompWompConstants.COLUMN_QUOTE_TEXT);
        String imageUri = cursor.getString(WompWompConstants.COLUMN_IMAGE_SOURCE_URI);
        String itemId = cursor.getString(WompWompConstants.COLUMN_ENTRY_ID);
        cursor.close();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("itemid", itemId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0 /* Request code */,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String appName = getResources().getString(R.string.app_name);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_wompwomp_newicon);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_stat_wompwomp_newicon)
                .setLargeIcon(largeIcon)
                .setContentTitle(appName)
                .setContentText(quoteText)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        Bitmap aBigBitmap = Utils.getBitmap(imageUri);
        if (aBigBitmap != null) {
            NotificationCompat.BigPictureStyle bigStyle = new
                    NotificationCompat.BigPictureStyle();
            bigStyle.setBigContentTitle(appName);
            bigStyle.setSummaryText(quoteText);
            bigStyle.bigPicture(aBigBitmap);
            notificationBuilder.setStyle(bigStyle);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
