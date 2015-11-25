package co.wompwomp.sunshine;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageCache;
import co.wompwomp.sunshine.util.ImageFetcher;

public class LikesActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private RecyclerView mRecyclerView = null;
    private MyCursorAdapter mAdapter = null;
    protected LinearLayoutManager mLayoutManager;
    private ImageFetcher mImageFetcher = null;
    private static final String IMAGE_CACHE_DIR = "thumbs";

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
        setContentView(R.layout.likes_activity);
        mRecyclerView = (RecyclerView) findViewById(R.id.quotesRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // 2. Set up the image cache
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this);
        mImageFetcher.setLoadingImage(R.drawable.geometry2);
        mImageFetcher.addImageCache(this.getSupportFragmentManager(), cacheParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);

        mAdapter = new MyCursorAdapter(this, null,mImageFetcher);
        mRecyclerView.setAdapter(mAdapter);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
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
                "(" + FeedContract.Entry.COLUMN_NAME_FAVORITE + "> 0)", // Selection
                null,                           // Selection args
                FeedContract.Entry.COLUMN_NAME_CREATED_ON + " desc"); // Sort
    }

    /**
     * Move the Cursor returned by the query into the ListView adapter. This refreshes the existing
     * UI with the data in the Cursor.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
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
}
