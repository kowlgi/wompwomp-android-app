package co.wompwomp.sunshine;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import co.wompwomp.sunshine.provider.FeedContract;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1 &&
                checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            //Any app that declares the WRITE_EXTERNAL_STORAGE permission is implicitly granted the
            // READ_EXTERNAL_STORAGE permission.
            String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            int permissionRequestCode = 200;
            requestPermissions(permissions, permissionRequestCode);
        }

        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(android.R.id.content, new MainFragment(), TAG);
            ft.commit();
        }

        logUser();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        Bundle extras = intent.getExtras();
        if(extras != null) {
            String itemId = extras.getString("itemid");
            if(itemId != null) {
                Answers.getInstance().logCustom(new CustomEvent("Push notification clicked")
                        .putCustomAttribute("itemlink", itemId));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Refresh"));
            MainFragment f = (MainFragment) getSupportFragmentManager().findFragmentByTag(TAG);
            f.update();
            return true;
        }
        else if (id == R.id.action_likes) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Likes"));
            Intent favoritesIntent = new Intent(this, LikesActivity.class);
            startActivity(favoritesIntent);
            return true;
        }
        else if( id == R.id.action_rate_us) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Rate"));
            Toast toast = Toast.makeText(this, getResources().getString(R.string.rate_us_placeholder), Toast.LENGTH_SHORT);
            toast.show();
            return true;
        }
        else if(id == R.id.action_share_app) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Share_app"));
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_app) + ": " + FeedContract.BASE_URL);
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, "Share the app"));
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

        switch(permsRequestCode){
            case 200:
                Log.v(TAG, "write external storage accepted");
                boolean writeExternalStorageAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                break;

        }
    }

    private void logUser() {
        Crashlytics.setUserIdentifier(Installation.id(this));
    }
}