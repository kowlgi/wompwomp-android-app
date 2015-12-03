package co.wompwomp.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.Random;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageCache;
import co.wompwomp.sunshine.util.ImageFetcher;
import co.wompwomp.sunshine.util.Utils;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private RecyclerView mRecyclerView = null;
    private MyCursorAdapter mAdapter = null;
    private LinearLayoutManager mLayoutManager;
    private ImageFetcher mImageFetcher = null;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private SwipeRefreshLayout mSwipeRefreshLayout = null;
    private final int mVisibleThreshold = 1;
    private int mFirstVisibleItem, mVisibleItemCount, mTotalItemCount;
    private View mProgressBarLayout;
    public static final String ACTION_FINISHED_SYNC = "co.wompwomp.sunshine.ACTION_FINISHED_SYNC";
    private static IntentFilter syncIntentFilter = new IntentFilter(ACTION_FINISHED_SYNC);
    private boolean mLoadingBottom, mLoadingTop;
    private Toast mNoNetworkToast = null;

    /**
     * Projection for querying the content provider.
     */
    private static final String[] PROJECTION = new String[]{
            FeedContract.Entry._ID,
            FeedContract.Entry.COLUMN_NAME_ENTRY_ID,
            FeedContract.Entry.COLUMN_NAME_IMAGE_SOURCE_URI,
            FeedContract.Entry.COLUMN_NAME_QUOTE_TEXT,
            FeedContract.Entry.COLUMN_NAME_FAVORITE,
            FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES,
            FeedContract.Entry.COLUMN_NAME_NUM_SHARES,
            FeedContract.Entry.COLUMN_NAME_CREATED_ON,
            FeedContract.Entry.COLUMN_NAME_CARD_TYPE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNoNetworkToast = Toast.makeText(this,
                R.string.no_network_connection_toast,
                Toast.LENGTH_SHORT);

        SyncUtils.CreateSyncAccount(this);

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

        Crashlytics.setUserIdentifier(Installation.id(this));

        /* Set up the recycler view */
        setContentView(R.layout.main_activity);
        mRecyclerView = (RecyclerView) findViewById(R.id.quotesRecyclerView);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mLoadingBottom = false;
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // check for scroll down
                if (dy > 0 && !mLoadingBottom) {
                    mVisibleItemCount = mRecyclerView.getChildCount();
                    mTotalItemCount = mLayoutManager.getItemCount();
                    mFirstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

                    if ((mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem + mVisibleThreshold)) {
                        Answers.getInstance().logCustom(new CustomEvent("Scrolled down for older items"));
                        syncItems(WompWompConstants.SyncMethod.SUBSET_OF_ITEMS_BELOW_LOW_CURSOR);
                    }
                }
            }
        });

        mProgressBarLayout = findViewById(R.id.progressBarLayout);
        String[] contentLoadingMessages = getResources().getStringArray(R.array.loading_messages);
        TextView progressBarText = (TextView) mProgressBarLayout.findViewById(R.id.progressBarText);
        int randomIndex = new Random().nextInt(contentLoadingMessages.length);
        progressBarText.setText(contentLoadingMessages[randomIndex]);
        mProgressBarLayout.setVisibility(View.VISIBLE);

        /* Set up the swipe refresh layout */
        mSwipeRefreshLayout = ( SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Answers.getInstance().logCustom(new CustomEvent("Swiped to refresh"));
                        syncItems(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR);
                    }
                }
        );

        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.wompwompblue,
                R.color.spinner_complementarycolor1,
                R.color.spinner_complementarycolor2,
                R.color.spinner_complementarycolor3);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false); // disable default title
        }
        myToolbar.setNavigationIcon(R.drawable.ic_action_trombone_white);
        TextView toolbarTitle = (TextView) myToolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smoothScrollToTop();
            }
        });
        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smoothScrollToTop();
            }
        });

        // Set up the image cache
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this);
        mImageFetcher.setLoadingImage(R.drawable.geometry2);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);

        mAdapter = new MyCursorAdapter(this, null,mImageFetcher);
        mRecyclerView.setAdapter(mAdapter);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoadingBottom = false;
        mLoadingTop = false;
        mImageFetcher.setExitTasksEarly(false);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncBroadcastReceiver);
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
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
                smoothScrollToTop();
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
            mSwipeRefreshLayout.setRefreshing(true);
            syncItems(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR);
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
            Utils.showAppPageLaunchToast(this);
            startActivity(Utils.getRateAppIntent());
            return true;
        }
        else if(id == R.id.action_share_app) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Share_app"));
            Utils.showShareToast(this);
            startActivity(Intent.createChooser(Utils.getShareAppIntent(this),
                    getResources().getString(R.string.app_chooser_title)));
            return true;
        }
        else if(id == R.id.action_about) {
            FragmentManager fm = getSupportFragmentManager();
            AboutDialogFragment aboutDialogFragment = AboutDialogFragment.newInstance();
            aboutDialogFragment.show(fm, "about_dialog");
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
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Query the content provider for data.
     *
     * <p>Loaders do queries in a background thread. They also provide a ContentObserver that is
     * triggered when data in the content provider changes. When the sync adapter updates the
     * content provider, the ContentObserver responds by resetting the loader and then reloading
     * it.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        // We only have one loader, so we can ignore the value of i.
        // (It'll be '0', as set in onCreate().)
        return new CursorLoader(this,  // Context
                FeedContract.Entry.CONTENT_URI, // URI
                PROJECTION,                // Projection
                null,                           // Selection
                null,                           // Selection args
                FeedContract.Entry.COLUMN_NAME_CREATED_ON + " desc"); // Sort
    }

    /**
     * Move the Cursor returned by the query into the ListView adapter. This refreshes the existing
     * UI with the data in the Cursor.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(cursor.getCount() >= 1) {
            mProgressBarLayout.setVisibility(View.GONE);
        }
        mAdapter.changeCursor(cursor);
    }

    /**
     * Called when the ContentObserver defined for the content provider detects that data has
     * changed. The ContentObserver resets the loader, and then re-runs the loader. In the adapter,
     * set the Cursor value to null. This removes the reference to the Cursor, allowing it to be
     * garbage-collected.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }

    private void syncItems(WompWompConstants.SyncMethod syncMethod) {
        if(!Utils.hasConnectivity(this)) {
            mNoNetworkToast.show();
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        if(syncMethod == WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR) {
            mLoadingTop = true;
        } else if (syncMethod == WompWompConstants.SyncMethod.SUBSET_OF_ITEMS_BELOW_LOW_CURSOR) {
            mLoadingBottom = true;
        }

        SyncUtils.TriggerRefresh(syncMethod);
        mSwipeRefreshLayout.setRefreshing(true);
    }

    private void smoothScrollToTop() {
        if(mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    private BroadcastReceiver mSyncBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update your views
            Bundle intentExtras = intent.getExtras();
            if(intentExtras != null) {
                String syncMethod = intentExtras.getString(WompWompConstants.SYNC_METHOD);
                if(syncMethod == null) return;

                Timber.i("Received sync intent: " + syncMethod);
                if(syncMethod.equals(WompWompConstants.SyncMethod.SUBSET_OF_ITEMS_BELOW_LOW_CURSOR.name())) {
                    mLoadingBottom = false;
                }
                else if(syncMethod.equals(WompWompConstants.SyncMethod.ALL_LATEST_ITEMS_ABOVE_HIGH_CURSOR.name())) {
                    mLoadingTop = false;
                }
            }

            mSwipeRefreshLayout.setRefreshing(mLoadingBottom || mLoadingTop);
        }
    };
}