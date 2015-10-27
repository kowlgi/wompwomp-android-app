package com.agni.sunshine;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import cz.msebera.android.httpclient.Header;


/**
 * Created by kowlgi on 10/21/15.
 */
public class MainFragment extends Fragment {
    private RecyclerView mRecyclerView = null;
    private RecyclerView.Adapter mAdapter = null;
    private ArrayList<Quote> mQuotes = null;
    private LinearLayoutManager mLayoutManager;
    private static final String TAG = "MainFragment";
    private static final String MAIN_URL = "http://45.55.216.153:3000";
    private ImageFetcher mImageFetcher = null;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private static final String MODEL_FILENAME = "agnimodel";
    private static final String FAVORITE_FILENAME = "agnifavorite.ser";
    private static final String INDEX_FILENAME = "index.ser";
    Handler mHandler = null;
    SwipeRefreshLayout mSwipeRefreshLayout = null;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.agni_main, container, false);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.quotesRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity());
        mImageFetcher.setLoadingImage(R.drawable.geometry2);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);

        // Create a new background thread for processing messages or runnables sequentially
        HandlerThread handlerThread = new HandlerThread("FavoritesThread");
        // Starts the background thread
        handlerThread.start();
        // Create a handler attached to the HandlerThread's Looper
        mHandler = new Handler(handlerThread.getLooper());

        mSwipeRefreshLayout = ( SwipeRefreshLayout) v.findViewById(R.id.swiperefresh);

        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i(TAG, "onRefresh called from SwipeRefreshLayout");

                        new UpdateFeedTask().execute(true);
                    }
                }
        );
        // Configure the refreshing colors
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mSwipeRefreshLayout.setEnabled(mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });

        new UpdateFeedTask().execute(false); //restore feed from files and get latest from the web too
        mSwipeRefreshLayout.setRefreshing(true);
        return v;
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
        public MyAdapter(ArrayList<Quote> myDataset) {
            mDataset = myDataset;
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

            if (mDataset.get(position).getFavorite()) {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

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
                    client.post(MAIN_URL + "/s/" + mQuotes.get(position).getDisplayId(), params, new TextHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String res) {
                            // we received status 200 OK..wohoo!
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                            //do nothing
                        }
                    });

                    mQuotes.get(position).setNumShares(mQuotes.get(position).getNumShares() + 1);
                    notifyDataSetChanged();

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, MAIN_URL + "/v/" + mQuotes.get(position).getDisplayId());
                    shareIntent.setType("text/plain");

                    View parentView = (View) holder.imageView.getParent();
                    Uri bmpUri = Utils.getLocalViewBitmapUri(parentView, getActivity());
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
                    Boolean currentFavorite = mQuotes.get(position).getFavorite();
                    Integer currentNumFavorites = mQuotes.get(position).getNumFavorites();
                    String SUB_URL = "";

                    if(currentFavorite) {
                        // she likes me not :(
                        if(currentNumFavorites > 0) {
                            mQuotes.get(position).setNumFavorites(currentNumFavorites - 1);
                        }
                        SUB_URL = "/uf/";

                    }
                    else {
                        // she likes me :)
                        mQuotes.get(position).setNumFavorites(currentNumFavorites+1);
                        SUB_URL = "/f/";
                    }
                    mQuotes.get(position).setFavorite(!currentFavorite);

                    SUB_URL += mQuotes.get(position).getDisplayId();

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

                    notifyDataSetChanged();
                }
            });
        }

        public class FavoriteRunnable implements Runnable {
            @Override
            public void run() {
                HashSet<String> favorites = new HashSet<String>();
                for (int i = 0; i < mQuotes.size(); i++) {
                    if (mQuotes.get(i).getFavorite() == true) {
                        favorites.add(mQuotes.get(i).getDisplayId());
                    }
                }

                try {
                    FileOutputStream fos = getActivity().openFileOutput(FAVORITE_FILENAME, Context.MODE_PRIVATE);
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

    // The types specified here are the input data type, the progress type, and the result type
    public class UpdateFeedTask extends AsyncTask<Boolean, Void, ArrayList<Quote> > {

        // IMPORTANT: result has to be in reverse chronological order
        protected ArrayList<Quote> doInBackground(Boolean... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String JSONResponse = null;
            ArrayList<Integer> index = null;
            Boolean newestFromWebOnly = params[0];

            if (Utils.fileExists(getActivity(), INDEX_FILENAME)) {
                try {
                    //Index is a list of array sizes stored in a file with the numerical suffix the same as the corresponding position
                    index = (ArrayList<Integer>) Utils.getObjectFromFile(INDEX_FILENAME, getActivity());
                } catch (java.io.FileNotFoundException fnf) {
                    Log.e(TAG, "Error from file stream open operation ", fnf);
                    fnf.printStackTrace();
                } catch (java.lang.Exception e) {
                    Log.e(TAG, "Error from file read operation ", e);
                    e.printStackTrace();
                }
            }

            // If no index file exists, initialize the object nonetheless as index.size() is used
            // in a lot of places
            if(index == null) {
                index = new ArrayList<Integer>();
            }

            int cursor = 0;
            if(newestFromWebOnly == true) {
                //Advance cursor to highest position not saved on file(s) in internal storage
                for (int i = 0; i < index.size(); i++) {
                    cursor += index.get(i);
                }
            }

            try {
                Uri.Builder ub = Uri.parse(MAIN_URL + "/items?offset=" + Integer.valueOf(cursor).toString()).buildUpon();
                URL url = new URL(ub.build().toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                JSONResponse = buffer.toString();
            } catch (IOException e) {
                Log.e(TAG, "Error reading from URL", e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                        e.printStackTrace();
                    }
                }
            }

            ArrayList<Quote> quotesFromWeb = null;
            try {
                quotesFromWeb = getQuotesFromJson(JSONResponse);
            } catch(java.lang.Exception e) {
                Log.e(TAG, "Error from JSON parse operation ", e);
                e.printStackTrace();
            }

            ArrayList<Quote> quotes = new ArrayList<Quote>();
            // Possibilities:
            // 1. request for all quotes and if we didn't fetch successfully from the server:
            // newestFromWebOnly is false and quotesFromWeb is empty -> READ from model data files, provided they exist i.e. index.size() > 0
            // 2. request for all quotes, which were fetched successfully from the server:
            // newestFromWebOnly is false and quotesFromWeb is not empty -> DELETE existing model data, index files and create new ones
            // 3. request for new quotes only, which were fetched successfully from the server:
            // newestFromWebOnly is true and quotesFromWeb is not empty --> DON'T read from model data files
            // 4. request for new quotes only, which were NOT fetched successfully from the server:
            // newestFromWebOnly is true and quotesFromWeb is empty -->  DON'T read from model data files

            /* possibility #1 */
            if (newestFromWebOnly == false &&
                    (quotesFromWeb == null || quotesFromWeb.isEmpty()) &&
                    index.size() > 0) {
                for (int i = index.size() - 1; i >= 0; i--) {
                    try {
                        ArrayList<Quote> quotesFromFile = (ArrayList<Quote>) Utils.getObjectFromFile(MODEL_FILENAME + Integer.valueOf(i).toString(), getActivity());
                        for (int j = 0; j < quotesFromFile.size(); j++) {
                            quotes.add(quotesFromFile.get(j)); // files are stored in reverse chronological order
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error from file read operation ", e);
                        e.printStackTrace();
                    }
                }
            }
            else if (quotesFromWeb != null && !quotesFromWeb.isEmpty()) {

                /* possibility #2 */
                if(newestFromWebOnly == false) {
                    for (int i = 0; i < index.size(); i++) {
                        getActivity().deleteFile(MODEL_FILENAME + Integer.valueOf(i).toString());
                    }

                    if (index.size() > 0) {
                        getActivity().deleteFile(INDEX_FILENAME);
                    }
                    index.clear();
                }
                else {
                    /* possibility #3 */
                }

                try {
                    // Create new file for quotes obtained from the web
                    // stored in reverse chronological order
                    FileOutputStream fos = getActivity().openFileOutput(
                            MODEL_FILENAME + Integer.valueOf(index.size()).toString(),
                            Context.MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(quotesFromWeb);
                    oos.close();

                    // Update the index
                    index.add(quotesFromWeb.size());

                    //Write index to file
                    FileOutputStream index_fos = getActivity().openFileOutput(
                            INDEX_FILENAME,
                            Context.MODE_PRIVATE);
                    ObjectOutputStream index_oos = new ObjectOutputStream(index_fos);
                    index_oos.writeObject(index);
                    index_oos.close();

                    //Update the in-memory Model
                    for(int i = quotesFromWeb.size() - 1; i >= 0 ; i--) {
                        quotes.add(0, quotesFromWeb.get(i)); //reverse chronological order
                    }

                } catch (java.io.FileNotFoundException fnf) {
                    Log.e(TAG, "Error from file stream open operation ", fnf);
                    fnf.printStackTrace();
                } catch (java.io.IOException ioe) {
                    Log.e(TAG, "Error from file write operation ", ioe);
                    ioe.printStackTrace();
                }
            }
            // possibility #4 and other DON'T CARE possibilities
            else {
                // outta luck, mate
            }

            if(quotes != null && Utils.fileExists(getActivity(), FAVORITE_FILENAME)) {
                try {
                    HashSet<String> favorites = (HashSet<String>) Utils.getObjectFromFile(FAVORITE_FILENAME, getActivity());
                    for(int i = 0; i < quotes.size(); i++){
                        if(favorites != null && favorites.contains(quotes.get(i).getDisplayId())) {
                            quotes.get(i).setFavorite(true);
                        }
                        else {
                            quotes.get(i).setFavorite(false);
                        }
                    }
                }catch (java.lang.Exception e) {
                    Log.e(TAG, "Error from file read operation ", e);
                    e.printStackTrace();
                }
            }

            return quotes;
        }

        private ArrayList<Quote> getQuotesFromJson(String JSONStr) throws JSONException {
            if(JSONStr == null) return null;

            // These are the names of the JSON objects that need to be extracted.
            final String AGNI_LIST = "list";
            final String AGNI_TEXT = "text";
            final String AGNI_IMAGEURI = "imageuri";
            final String AGNI_ID = "id";
            final String AGNI_CREATEDON = "created_on";
            final String AGNI_NUMFAVORITES = "numfavorites";
            final String AGNI_NUMSHARES = "numshares";

            JSONObject entireJson = new JSONObject(JSONStr);
            JSONArray quoteArray = entireJson.getJSONArray(AGNI_LIST);

            ArrayList<Quote> result = new ArrayList<Quote>();
            for(int i = 0; i < quoteArray.length() ; i++) {
                JSONObject jsonObject = quoteArray.getJSONObject(i);
                Quote q = new Quote();
                q.setImageSourceUri(jsonObject.getString(AGNI_IMAGEURI));
                q.setQuoteText(jsonObject.getString(AGNI_TEXT));
                q.setDisplayId(jsonObject.getString(AGNI_ID));
                q.setCreatedOn(jsonObject.getString(AGNI_CREATEDON));
                q.setNumFavorites(jsonObject.getInt(AGNI_NUMFAVORITES));
                q.setNumShares(jsonObject.getInt(AGNI_NUMSHARES));
                result.add(0, q); //reverse chronological order
            }

            return result;
        }

        protected void onPostExecute(ArrayList<Quote> result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            if (result != null) {
                // specify an adapter (see also next example)
                if(mQuotes == null) {
                    mQuotes = result;
                    mAdapter = new MyAdapter(mQuotes);
                    mRecyclerView.setAdapter(mAdapter);
                }
                else {
                    for(int i = result.size() - 1; i >= 0; i--) {
                        mQuotes.add(0, result.get(i)); // add in reverse chronological order
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    public void update(Boolean getFeedFromWebOnly) {
        mSwipeRefreshLayout.setRefreshing(true);
        new UpdateFeedTask().execute(getFeedFromWebOnly); // get latest feed from the web only
    }

    public Bundle createBundleForFavorites() {
        Bundle bundle = new Bundle();
        ArrayList<Quote> favoriteQuotes = new ArrayList<Quote>();
        for (int i = 0; i < mQuotes.size(); i++) {
            if(mQuotes.get(i).getFavorite()) {
                favoriteQuotes.add(mQuotes.get(i));
            }
        }
        bundle.putSerializable("favoriteitems", favoriteQuotes);
        return bundle;
    }
}
