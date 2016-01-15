package co.wompwomp.sunshine;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.widget.ShareDialog;

import java.util.Random;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageCache;
import co.wompwomp.sunshine.util.ImageFetcher;

public class LikesActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private RecyclerView mRecyclerView = null;
    private MyCursorAdapter mAdapter = null;
    protected LinearLayoutManager mLayoutManager;
    private ImageFetcher mImageFetcher = null;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private ShareDialog mShareDialog;
    private CallbackManager mCallbackManager;
    private LinearLayout mEmptyLikesLayout;

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
            FeedContract.Entry.COLUMN_NAME_CARD_TYPE,
            FeedContract.Entry.COLUMN_NAME_DISMISS_ITEM,
            FeedContract.Entry.COLUMN_NAME_AUTHOR
    };

    private static final String SELECTION = "(" + FeedContract.Entry.COLUMN_NAME_FAVORITE +
                                            " > 0 AND " + FeedContract.Entry.COLUMN_NAME_DISMISS_ITEM + " IS 0)";

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

        mEmptyLikesLayout = (LinearLayout) findViewById(R.id.empty_likes_layout);
        mEmptyLikesLayout.setVisibility(View.VISIBLE);

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this);
        mImageFetcher.setLoadingImage(R.drawable.geometry2);
        mImageFetcher.addImageCache(this.getSupportFragmentManager(), cacheParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);

        mCallbackManager = CallbackManager.Factory.create();
        mShareDialog = new ShareDialog(this);
        // this part is optional
        mShareDialog.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onCancel() {
                //do nothing;
            }

            @Override
            public void onError(FacebookException e) {
                //do nothing;
            }

            @Override
            public void onSuccess(Sharer.Result result) {
                //do nothing;
            }
        });


        mAdapter = new MyCursorAdapter(this, null,mImageFetcher, mShareDialog);
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
                SELECTION, // Selection
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
            mEmptyLikesLayout.setVisibility(View.GONE);
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

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

}
