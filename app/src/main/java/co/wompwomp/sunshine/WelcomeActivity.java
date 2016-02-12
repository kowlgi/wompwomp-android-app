package co.wompwomp.sunshine;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.URLUtil;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import co.wompwomp.sunshine.provider.FeedContract;
import timber.log.Timber;

public class WelcomeActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

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

        HashSet<String> likes;
        if (fileExists(this, WompWompConstants.LIKES_FILENAME)) {
            // All likes have apparently been migrated from the database to file,
            // so we're good
            Timber.d(WompWompConstants.LIKES_FILENAME +" exists");
        } else {
            // we need to store 'likes' data from DB into a file
            likes = new HashSet<>();
            final String[] LIKES_PROJECTION = new String[]{
                    FeedContract.Entry.COLUMN_NAME_ENTRY_ID,
            };
            final String LIKES_SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_FAVORITE +
                    " IS 1 )";
            Cursor c = getContentResolver().query(FeedContract.Entry.CONTENT_URI,
                    LIKES_PROJECTION,
                    LIKES_SELECTION,
                    null,
                    null);

            if(c != null && c.getCount() > 0) {
                c.moveToFirst();

                do {
                    likes.add(c.getString(0));
                } while(c.moveToNext());
            }

            Timber.d("Creating " + WompWompConstants.LIKES_FILENAME + " with content: " + likes.toString());

            try {
                FileOutputStream fos = this.openFileOutput(WompWompConstants.LIKES_FILENAME, Context.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(likes);
                oos.close();
            } catch (java.io.FileNotFoundException fnf) {
                Timber.e("Error from file stream open operation ", fnf);
                fnf.printStackTrace();
            } catch (java.io.IOException ioe) {
                Timber.e("Error from file write operation ", ioe);
                ioe.printStackTrace();
            }
        }

        /* Only keep the videos that correspond to items in the db */
        if (fileExists(this, WompWompConstants.VIDEO_DOWNLOADS_FILENAME)) {
            try {
                FileInputStream fis = openFileInput(WompWompConstants.VIDEO_DOWNLOADS_FILENAME);
                ObjectInputStream ois = new ObjectInputStream(fis);
                HashSet<String> videoFiles = (HashSet<String>) ois.readObject();
                ois.close();

                final String[] VIDEOS_PROJECTION = new String[]{
                        FeedContract.Entry.COLUMN_NAME_VIDEOURI,
                };
                final String VIDEOS_SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_VIDEOURI +
                        " IS NOT NULL OR " + FeedContract.Entry.COLUMN_NAME_VIDEOURI + " <> '' )";

                Cursor c = getContentResolver().query(FeedContract.Entry.CONTENT_URI,
                        VIDEOS_PROJECTION,
                        VIDEOS_SELECTION,
                        null,
                        null);

                if(c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    HashSet<String> videoFilesToKeep = new HashSet<>();
                    do {
                        String filename = URLUtil.guessFileName(c.getString(0), null, null);
                        if(videoFiles.remove(filename)){
                            videoFilesToKeep.add(filename); // video file we want to keep
                        }; // remove filenames corresponding to items in db
                    } while(c.moveToNext());

                    // delete files corresponding to items not in the db
                    for(String filename: videoFiles) {
                        deleteFile(filename);
                    }

                    FileOutputStream fos = openFileOutput(WompWompConstants.VIDEO_DOWNLOADS_FILENAME, Context.MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(videoFilesToKeep);
                    oos.close();

                    Timber.d("Video files to keep: " + videoFilesToKeep);
                }

            }catch (Exception ignored) {
            }
        }

        SyncUtils.TriggerRefresh(WompWompConstants.SyncMethod.SUBSET_OF_LATEST_ITEMS_NO_CURSOR);

        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
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

    private boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        return file != null && file.exists();
    }
}
