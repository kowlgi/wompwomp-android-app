package co.wompwomp.sunshine;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import co.wompwomp.sunshine.provider.FeedContract;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NotifierService extends IntentService{
    public static final String ACTION_FINISHED_SYNC_FOR_NOTIFICATION = "co.wompwomp.sunshine.ACTION_FINISHED_SYNC_FOR_NOTIFICATION";

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
            }
        }
    }

    private void initNotificationAlarm() {
        // update notification time using google tag manager if google play services exist on this phone
        if(checkPlayServices()) {
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
            configureAlarmForPushNotification(hour, minute);
            Timber.i("Configured push notification with gtm:" + hour + ":" + minute);
        } else {
            configureAlarmForPushNotification(WompWompConstants.DEFAULT_PUSH_NOTIFY_HOUR, WompWompConstants.DEFAULT_PUSH_NOTIFY_MINUTE);
            Timber.i("Configured push notification alarm:" + WompWompConstants.DEFAULT_PUSH_NOTIFY_HOUR + ":" + WompWompConstants.DEFAULT_PUSH_NOTIFY_MINUTE);
        }
    }

    private void configureAlarmForPushNotification(int minute, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotifierService.class);
        intent.setAction(WompWompConstants.PUSH_NOTIFICATION);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    private void handlePushNotify() {
        // DECIDING WHETHER TO SHOW NOTIFICATION
        // get last logged in timestamp
        // if timestamp is < notificationIntervalInHours, don't push a notification
        // if timestamp is > notificationIntervalInHours or doesn't exist, send a notification

        // CONTENT IN THE NOTIFICATION
        // get timestamp of the newest item in the database
        // sync new items from the server

        DateTime dt = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String twentyFourHoursAgo = fmt.print(dt.minusHours(24));
        String last_logged_in = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(WompWompConstants.LAST_LOGGED_IN_TIMESTAMP, twentyFourHoursAgo);

        int interval =  WompWompConstants.DEFAULT_PUSH_NOTIFY_INTERVAL_IN_HOURS;
        if(checkPlayServices()) {
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

        LocalDateTime last_logged_in_dt = LocalDateTime.parse(last_logged_in, ISODateTimeFormat.dateTime());
        boolean exceedsInterval = Period.fieldDifference(dt.toLocalDateTime(), last_logged_in_dt).getHours() >= interval;





        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("itemid", "4kbF_bOpx");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String appName = getResources().getString(R.string.app_name);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_trombone)
                .setContentTitle(appName)
                .setContentText("When your pet and you have similar fashion taste")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        Bitmap aBigBitmap = getBitmap("http://i.imgur.com/lHCqJdM.jpg");
        if(aBigBitmap != null) {
            NotificationCompat.BigPictureStyle bigStyle = new
                    NotificationCompat.BigPictureStyle();
            bigStyle.setBigContentTitle(appName);
            bigStyle.setSummaryText("When your pet and you have similar fashion taste");
            bigStyle.bigPicture(aBigBitmap);
            notificationBuilder.setStyle(bigStyle);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private Bitmap getBitmap(String imageUri) {

        if(imageUri == null) return null;

        OkHttpClient client = new OkHttpClient();
        Bitmap aBitmap = null;
        ResponseBody body = null;
        try {
            final URL url = new URL(imageUri);
            Call call = client.newCall(new Request.Builder().url(url).get().build());
            Response response = call.execute();
            body = response.body();
            aBitmap = BitmapFactory.decodeStream(body.byteStream());
        } catch (final IOException e) {
            Timber.e( "Error in downloadBitmap - " + e);
        } finally {
            if(body != null) body.close();
        }

        return aBitmap;
    }

    private BroadcastReceiver mSyncBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update your views
            Bundle intentExtras = intent.getExtras();
            if(intentExtras != null) {
                String syncMethod = intentExtras.getString(WompWompConstants.SYNC_METHOD);
                if(syncMethod == null) return;

                if(syncMethod.equals(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR_AUTO_NOTIFIER_SERVICE.name())) {
                    final String[] PROJECTION = new String[]{
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
                            FeedContract.Entry.COLUMN_NAME_FILE_SIZE
                    };

                    DateTime dt = new DateTime();
                    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                    String twentyFourHoursAgo = fmt.print(dt.minusHours(24));
                    String last_logged_in = PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .getString(WompWompConstants.LAST_LOGGED_IN_TIMESTAMP, twentyFourHoursAgo);
                    LocalDateTime.parse(last_logged_in, ISODateTimeFormat.dateTime());

                    final String SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_CREATED_ON +
                            " > " + last_logged_in + " )";

                    Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
                    Cursor c = getApplicationContext().getContentResolver().query(
                            uri,
                            PROJECTION,
                            null,
                            null,
                            FeedContract.Entry.COLUMN_NAME_CREATED_ON + " desc");



                }
            }
        }
    };
}
