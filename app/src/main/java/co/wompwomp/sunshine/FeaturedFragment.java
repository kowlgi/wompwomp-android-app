package co.wompwomp.sunshine;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.widget.ShareDialog;

import java.util.Arrays;
import java.util.Random;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageCache;
import co.wompwomp.sunshine.util.ImageFetcher;
import co.wompwomp.sunshine.util.Utils;
import timber.log.Timber;

public class FeaturedFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SmoothScrollToTopListener {
    private RecyclerView mRecyclerView = null;
    private MyCursorAdapter mAdapter = null;
    private LinearLayoutManager mLayoutManager;
    private ImageFetcher mImageFetcher = null;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private View mProgressBarLayout;
    private ShareDialog mShareDialog;
    private CallbackManager mCallbackManager;

    public FeaturedFragment() {
        // Required empty public constructor
    }

    public static FeaturedFragment newInstance() {
        return new FeaturedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        // Set up the image cache
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
        mImageFetcher = new ImageFetcher(getActivity());
        mImageFetcher.setLoadingImage(R.drawable.geometry2);
        mImageFetcher.addImageCache(getFragmentManager(), cacheParams);

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
                Timber.e(e.getMessage());
                Timber.e(Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(Sharer.Result result) {
                //do nothing;
            }
        });

        mAdapter = new MyCursorAdapter(getActivity(), getContext(), null, mImageFetcher, mShareDialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_featured, container, false);
        /* Set up the recycler view */
        mRecyclerView = (RecyclerView) view.findViewById(R.id.quotesRecyclerView);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.clearOnScrollListeners();

        mProgressBarLayout = view.findViewById(R.id.progressBarLayout);
        String[] contentLoadingMessages = getResources().getStringArray(R.array.loading_messages);
        TextView progressBarText = (TextView) mProgressBarLayout.findViewById(R.id.progressBarText);
        int randomIndex = new Random().nextInt(contentLoadingMessages.length);
        progressBarText.setText(contentLoadingMessages[randomIndex]);
        mProgressBarLayout.setVisibility(View.VISIBLE);

        mRecyclerView.setAdapter(mAdapter);
        getActivity().getSupportLoaderManager().initLoader(WompWompConstants.FEATURED_LOADER_ID, null, this);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume(){
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        /* Because we're loading images asynchronously, we might pause() this activity before
        fetching the image from network. When this activity is resumed, recycler view doesn't call
        adapter.onBindViewholder() automatically, which is needed to kickstart image loading again.
        So, we call notifyDataSetChanged() so onBindViewHolder() gets invoked */
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStart(){
        super.onStart();
        mAdapter.start();

        /* Opportunistically resync only if we're reopening the app */
        if (PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getBoolean(WompWompConstants.PREF_APP_RESUMED_FROM_BG, true)) {
            SyncUtils.TriggerRefresh(WompWompConstants.SyncMethod.ALL_FEATURED_ITEMS);
            Utils.postToWompwomp(FeedContract.APP_OPENED_URL, getActivity());
        } else {
            PreferenceManager
                    .getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean(WompWompConstants.PREF_APP_RESUMED_FROM_BG, true)
                    .apply();
        }
        Answers.getInstance().logCustom(new CustomEvent("View Popular Fragment"));
    }

    @Override
    public void onPause(){
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
        Utils.postToWompwomp(FeedContract.APP_CLOSED_URL, getActivity());
    }

    @Override
    public void onStop(){
        super.onStop();
        mAdapter.stop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        getActivity().getSupportLoaderManager().destroyLoader(WompWompConstants.FEATURED_LOADER_ID);
        mAdapter.close();
        mImageFetcher.closeCache();
    }

    /**
     * Query the content provider for data.
     * <p/>
     * <p>Loaders do queries in a background thread. They also provide a ContentObserver that is
     * triggered when data in the content provider changes. When the sync adapter updates the
     * content provider, the ContentObserver responds by resetting the loader and then reloading
     * it.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // We only have one loader, so we can ignore the value of i.
        // (It'll be '0', as set in onCreate().)
        return new CursorLoader(getActivity(),  // Context
                FeedContract.Entry.CONTENT_URI, // URI
                WompWompConstants.PROJECTION, // Projection
                WompWompConstants.FEATURED_LIST_SELECTION, // Selection
                null, // Selection args
                FeedContract.Entry.COLUMN_NAME_FEATURED_PRIORITY + " desc"); // Sort
    }

    /**
     * Move the Cursor returned by the query into the ListView adapter. This refreshes the existing
     * UI with the data in the Cursor.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.getCount() >= 1) {
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

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void onSmoothScrollToTop(){
        if (mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

}
