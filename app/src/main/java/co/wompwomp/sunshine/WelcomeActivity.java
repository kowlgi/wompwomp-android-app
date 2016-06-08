package co.wompwomp.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashSet;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.Utils;
import co.wompwomp.sunshine.util.VideoFileInfo;

public class WelcomeActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private IntentFilter syncIntentFilter = new IntentFilter(WompWompConstants.ACTION_FINISHED_SYNC);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        SyncUtils.CreateSyncAccount(this);

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        Crashlytics.setUserIdentifier(Installation.id(this));

        /* Only keep the videos that correspond to items in the db */
        try {

            final String[] VIDEOS_PROJECTION = new String[]{
                    FeedContract.Entry.COLUMN_NAME_VIDEOURI,
            };
            final String VIDEOS_SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_VIDEOURI +
                    " IS NOT NULL AND " + FeedContract.Entry.COLUMN_NAME_VIDEOURI + " <> '' )";

            Cursor c = getContentResolver().query(FeedContract.Entry.CONTENT_URI,
                    VIDEOS_PROJECTION,
                    VIDEOS_SELECTION,
                    null,
                    FeedContract.Entry.COLUMN_NAME_CREATED_ON + " desc");

            if(c != null && c.getCount() > 0) {
                c.moveToFirst();
                HashSet<String> videoFilesToKeep = new HashSet<>();
                int count = WompWompConstants.MAX_VIDEOS_FILES_TO_RETAIN;
                do{
                    String filename = URLUtil.guessFileName(c.getString(0), null, null);
                    videoFilesToKeep.add(filename);
                    count--;
                    if(count == 0) break;
                } while(c.moveToNext());

                for(String filename: fileList()) {
                    if(filename.endsWith(".mp4") && !videoFilesToKeep.contains(filename)){
                        deleteFile(filename);
                    }
                }
            }
            if(c != null) c.close();
        }catch (Exception ignored) {

        }

        getContentResolver().delete(FeedContract.Entry.CONTENT_URI,
                FeedContract.Entry.COLUMN_NAME_ENTRY_ID + "= '" + WompWompConstants.WOMPWOMP_CTA_UPGRADE + "' " +
                        " AND CAST(" + FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT + " AS INTEGER) <=" + BuildConfig.VERSION_CODE,
                null);

        Intent pushNotificationIntent = new Intent(this, NotifierService.class);
        pushNotificationIntent.setAction(WompWompConstants.INIT_NOTIFICATION_ALARM);
        startService(pushNotificationIntent);

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean hasConnectivity = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!hasConnectivity) {
            Toast.makeText(this,
                    R.string.no_network_connection_toast,
                    Toast.LENGTH_SHORT).show();

            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        } else {
            LocalBroadcastManager.getInstance(this).registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
            SyncUtils.TriggerRefresh(WompWompConstants.SyncMethod.SUBSET_OF_LATEST_ITEMS_NO_CURSOR);
        }
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
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    private BroadcastReceiver mSyncBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
            final String VIDEOS_SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_VIDEOURI +
                    " IS NOT NULL AND " + FeedContract.Entry.COLUMN_NAME_VIDEOURI + " <> '' )";
            Cursor c = getApplicationContext().getContentResolver().query(
                    uri,
                    WompWompConstants.PROJECTION,
                    VIDEOS_SELECTION,
                    null,
                    FeedContract.Entry.COLUMN_NAME_CREATED_ON + " desc");

            if(c != null && c.getCount() > 0) {
                c.moveToFirst();
                ArrayList<VideoFileInfo> videoPrefetchList = new ArrayList<>();
                int numVideos = 0;
                do {
                    String videoUri = c.getString(WompWompConstants.COLUMN_VIDEOURI);
                    Integer fileSize = c.getInt(WompWompConstants.COLUMN_FILE_SIZE);
                    if(videoUri == null || videoUri.length() <= 0 ) continue;

                    String videofilename = URLUtil.guessFileName(videoUri, null, null);
                    if(Utils.validVideoFile(context, videofilename, fileSize)) continue;

                    VideoFileInfo vfi = new VideoFileInfo(videoUri, fileSize);
                    videoPrefetchList.add(vfi);
                    numVideos++;
                } while(c.moveToNext() && numVideos < WompWompConstants.MAX_NUM_PREFETCH_VIDEOS);
                FileDownloaderService.startDownload(context, videoPrefetchList);
            }
            if(c != null) c.close();

            LocalBroadcastManager.getInstance(context).unregisterReceiver(mSyncBroadcastReceiver);
            Intent mainIntent = new Intent(context, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }
    };
}
