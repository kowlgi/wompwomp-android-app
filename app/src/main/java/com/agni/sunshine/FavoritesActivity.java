package com.agni.sunshine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.agni.sunshine.util.ImageCache;
import com.agni.sunshine.util.ImageFetcher;
import com.agni.sunshine.util.Utils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.ocpsoft.pretty.time.PrettyTime;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;

import cz.msebera.android.httpclient.Header;

public class FavoritesActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView = null;
    private RecyclerView.Adapter mAdapter = null;
    private LinearLayoutManager mLayoutManager = null;
    private ArrayList<Quote> mFavorites = null;
    private ImageFetcher mImageFetcher = null;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private static final String MAIN_URL = "http://45.55.216.153:3000";
    private static final String TAG = "FavoritesActivity";
    private Handler mHandler = null;
    private static final String FAVORITE_FILENAME = "agnifavorite.ser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorites_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHideOnContentScrollEnabled(true);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        mFavorites = (ArrayList<Quote>)bundle.getSerializable("favoriteitems");

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);

        mRecyclerView = (RecyclerView) findViewById(R.id.quotesRecyclerView);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Create a new background thread for processing messages or runnables sequentially
        HandlerThread handlerThread = new HandlerThread("FavoritesThread");
        // Starts the background thread
        handlerThread.start();
        // Create a handler attached to the HandlerThread's Looper
        mHandler = new Handler(handlerThread.getLooper());

        mAdapter = new MyAdapter(mFavorites, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
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

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private ArrayList<Quote> mDataset = null;
        private Context mContext;

        // Provide a direct reference to each of the views within a data item
        // Used to cache the views within the item layout for fast access
        public class ViewHolder extends RecyclerView.ViewHolder {
            // Your holder should contain a member variable
            // for any view that will be set as you render a row
            public SquareImageView imageView;
            public TextView textView;
            public ImageButton shareButton;
            public ImageButton favoriteButton;
            public TextView createdOnView;
            public TextView numfavoritesView;
            public TextView numsharesView;

            // We also create a constructor that accepts the entire item row
            // and does the view lookups to find each subview
            public ViewHolder(View itemView) {
                // Stores the itemView in a public final member variable that can be used
                // to access the context from any ViewHolder instance.
                super(itemView);
                imageView = (SquareImageView) itemView.findViewById(R.id.imageView);
                textView = (TextView) itemView.findViewById(R.id.textView);
                shareButton = (ImageButton) itemView.findViewById(R.id.share_button);
                favoriteButton = (ImageButton) itemView.findViewById(R.id.favorite_button);
                createdOnView = (TextView) itemView.findViewById(R.id.createdon);
                numfavoritesView = (TextView) itemView.findViewById(R.id.favoriteCount);
                numsharesView = (TextView) itemView.findViewById(R.id.shareCount);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(ArrayList<Quote> myDataset, Context context) {
            mDataset = myDataset;
            mContext = context;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_main, parent, false);

            MyAdapter.ViewHolder vh = new MyAdapter.ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final MyAdapter.ViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

            mImageFetcher.loadImage(mDataset.get(position).getImageSourceUri(), holder.imageView);

            holder.textView.setMinHeight((int) Math.round(dpHeight * 0.20)); //min 20% of height
            holder.textView.setText(mDataset.get(position).getQuoteText());

            holder.favoriteButton.setImageResource(R.drawable.ic_favorite_black_24dp);

            PrettyTime prettyTime = new PrettyTime();
            LocalDateTime createdOn = LocalDateTime.parse(mDataset.get(position).getCreatedOn(), ISODateTimeFormat.dateTime());
            //http://www.flowstopper.org/2012/11/prettytime-and-joda-playing-nice.html
            holder.createdOnView.setText(prettyTime.format(createdOn.toDateTime(DateTimeZone.UTC).toDate()));

            holder.numfavoritesView.setText(mDataset.get(position).getNumFavorites().toString());
            holder.numsharesView.setText(mDataset.get(position).getNumShares().toString());

            holder.shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AsyncHttpClient client = new AsyncHttpClient();
                    RequestParams params = new RequestParams();
                    client.post(MAIN_URL + "/s/" + mFavorites.get(position).getDisplayId(), params, new TextHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String res) {
                            // we received status 200 OK..wohoo!
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                            //do nothing
                        }
                    });

                    mFavorites.get(position).setNumShares(mFavorites.get(position).getNumShares() + 1);
                    notifyDataSetChanged();

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, MAIN_URL + "/v/" + mFavorites.get(position).getDisplayId());
                    shareIntent.setType("text/plain");

                    View parentView = (View) holder.imageView.getParent();
                    Uri bmpUri = Utils.getLocalViewBitmapUri(parentView, mContext);
                    if (bmpUri != null) {
                        // Construct a ShareIntent with link to image
                        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                        shareIntent.setType("image/*");
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share"));
                }
            });

            holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean currentFavorite = mFavorites.get(position).getFavorite();
                    Integer currentNumFavorites = mFavorites.get(position).getNumFavorites();
                    String SUB_URL = "";

                    if(currentFavorite) {
                        // she likes me not :(
                        if(currentNumFavorites > 0) {
                            mFavorites.get(position).setNumFavorites(currentNumFavorites - 1);
                        }
                        SUB_URL = "/uf/";

                    }
                    else {
                        // she likes me :)
                        mFavorites.get(position).setNumFavorites(currentNumFavorites+1);
                        SUB_URL = "/f/";
                    }

                    SUB_URL += mFavorites.get(position).getDisplayId();

                    // Execute the specified code on the worker thread
                    FavoriteRunnable favRunnable = new FavoriteRunnable();
                    mHandler.post(favRunnable);

                    AsyncHttpClient client = new AsyncHttpClient();
                    RequestParams params = new RequestParams();
                    client.post(MAIN_URL + SUB_URL, params, new TextHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String res) {
                            // called when response HTTP status is "200 OK"
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                            //do nothing
                        }
                    });

                    mFavorites.remove(position);
                    notifyDataSetChanged();
                }
            });
        }

        public class FavoriteRunnable implements Runnable {
            @Override
            public void run() {
                HashSet<String> favorites = new HashSet<String>();
                for (int i = 0; i < mFavorites.size(); i++) {
                    if (mFavorites.get(i).getFavorite() == true) {
                        favorites.add(mFavorites.get(i).getDisplayId());
                    }
                }

                try {
                    FileOutputStream fos = mContext.openFileOutput(FAVORITE_FILENAME, Context.MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(favorites);
                    oos.close();
                } catch (java.io.FileNotFoundException fnf) {
                    Log.e(TAG, "Error from file stream open operation ", fnf);
                    fnf.printStackTrace();
                } catch (java.io.IOException ioe) {
                    Log.e(TAG, "Error from file write operation ", ioe);
                    ioe.printStackTrace();
                }
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }

}
