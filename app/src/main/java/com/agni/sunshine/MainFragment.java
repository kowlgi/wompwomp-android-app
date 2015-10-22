package com.agni.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;

/**
 * Created by kowlgi on 10/21/15.
 */
public class MainFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private Quote[] mQuotes = null;
    private RecyclerView.LayoutManager mLayoutManager;
    private static final String TAG = "MainFragment";
    private static final String MAIN_URL = "http://45.55.216.153:3000";
    private ImageFetcher mImageFetcher;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private static final String MODEL_FILENAME = "agnimodel.ser";
    private static final String FAVORITE_FILENAME = "agnifavorite.ser";
    Handler mHandler = null;

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

        new UpdateFeedTask().execute();
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
        private Quote[] mDataset = null;


        // Provide a direct reference to each of the views within a data item
        // Used to cache the views within the item layout for fast access
        public class ViewHolder extends RecyclerView.ViewHolder {
            // Your holder should contain a member variable
            // for any view that will be set as you render a row
            public SquareNetworkImageView imageView;
            public TextView textView;
            public ImageButton shareButton;
            public ImageButton favoriteButton;

            // We also create a constructor that accepts the entire item row
            // and does the view lookups to find each subview
            public ViewHolder(View itemView) {
                // Stores the itemView in a public final member variable that can be used
                // to access the context from any ViewHolder instance.
                super(itemView);
                imageView = (SquareNetworkImageView) itemView.findViewById(R.id.imageView);
                textView = (TextView) itemView.findViewById(R.id.textView);
                shareButton = (ImageButton) itemView.findViewById(R.id.share_button);
                favoriteButton = (ImageButton) itemView.findViewById(R.id.favorite_button);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(Quote[] myDataset) {
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

            mImageFetcher.loadImage(mDataset[position].getSourceUri(), holder.imageView);

            holder.textView.setMinHeight((int) Math.round(dpHeight * 0.20)); //min 20% of height
            holder.textView.setText(mDataset[position].getQuoteText());
            holder.textView.setTextColor(Color.parseColor(mDataset[position].getBodytextColor()));

            holder.shareButton.setBackgroundColor(Color.parseColor(mDataset[position].getBackgroundColor()));
            holder.favoriteButton.setBackgroundColor(Color.parseColor(mDataset[position].getBackgroundColor()));
            Log.v(TAG, Integer.valueOf(position).toString());
            if (mDataset[position].getFavorite()) {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

            View parentView = (View) holder.imageView.getParent();
            parentView.setBackgroundColor(Color.parseColor(mDataset[position].getBackgroundColor()));

            CardView cardView = (CardView) holder.itemView;
            cardView.setCardBackgroundColor(Color.parseColor(mDataset[position].getBackgroundColor()));

            holder.shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, mQuotes[position].getDisplayUri());
                    shareIntent.setType("text/plain");

                    View parentView = (View) holder.imageView.getParent();
                    Uri bmpUri = getLocalViewBitmapUri(parentView);
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
                    Boolean currentFavorite = mQuotes[position].getFavorite();
                    mQuotes[position].setFavorite(!currentFavorite);

                    // Execute the specified code on the worker thread
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            HashSet<String> favorites = new HashSet<String>();
                            for (int i = 0; i < mQuotes.length; i++) {
                                if (mQuotes[i].getFavorite() == true) {
                                    favorites.add(mQuotes[i].getDisplayUri());
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
                    });
                    notifyDataSetChanged();
                }
            });
        }

        public Uri getLocalViewBitmapUri(View aView){
            // Example: Extract Bitmap from ImageView drawable
            // final Bitmap bmp  = ((BitmapDrawable) networkImageview.getDrawable()).getBitmap();

            //Create a Bitmap with the same dimensions
            Bitmap image = Bitmap.createBitmap(aView.getWidth(),
                    aView.getHeight(),
                    Bitmap.Config.RGB_565);
            //Draw the view inside the Bitmap
            aView.draw(new Canvas(image));

            // Store image to default external storage directory
            Uri bmpUri = null;
            try {
                if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1 &&
                        getActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
                    return  null;
                }

                File file =  new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "share_image_" + System.currentTimeMillis() + ".png");
                file.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 90, out); //Output
                bmpUri = Uri.fromFile(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bmpUri;
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }

    // The types specified here are the input data type, the progress type, and the result type
    public class UpdateFeedTask extends AsyncTask<Void, Void, Quote[]> {

        protected Quote[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String JSONResponse = null;

            try {
                Log.v(TAG, "Start JSON Fetch");
                Uri.Builder ub = Uri.parse(MAIN_URL + "/items?offset=-1").buildUpon();
                URL url = new URL(ub.build().toString());

                Log.v(TAG, url.toString());

                // Create the request to OpenWeatherMap, and open the connection
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
                Log.v(TAG, JSONResponse);

            }catch (IOException e) {
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

            Quote[] quotes = null;
            try {
                quotes = getQuotesFromJson(JSONResponse);
            } catch(java.lang.Exception e) {
                Log.e(TAG, "Error from JSON parse operation ", e);
                e.printStackTrace();
            }

            if(quotes == null) {
                try {
                    quotes = getQuotesFromFile(MODEL_FILENAME);
                }catch (java.lang.Exception e) {
                    Log.e(TAG, "Error from file read operation ", e);
                    e.printStackTrace();
                }
            }
            else {
                try {
                    FileOutputStream fos = getActivity().openFileOutput(MODEL_FILENAME, Context.MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(quotes);
                    oos.close();
                } catch (java.io.FileNotFoundException fnf) {
                    Log.e(TAG, "Error from file stream open operation ", fnf);
                    fnf.printStackTrace();
                } catch (java.io.IOException ioe) {
                    Log.e(TAG, "Error from file write operation ", ioe);
                    ioe.printStackTrace();
                }
            }

            if(quotes != null) {
                try {
                    HashSet<String> favorites = getFavoritesFromFile(FAVORITE_FILENAME);
                    for(int i = 0; i < quotes.length; i++){
                        if(favorites != null && favorites.contains(quotes[i].getDisplayUri())) {
                            quotes[i].setFavorite(true);
                        }
                        else {
                            quotes[i].setFavorite(false);
                        }
                    }
                }catch (java.lang.Exception e) {
                    Log.e(TAG, "Error from file read operation ", e);
                    e.printStackTrace();
                }
            }

            return quotes;
        }

        public Quote[] getQuotesFromFile (String filePath) throws Exception {
            FileInputStream fis = getActivity().openFileInput(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Quote[] quotes = (Quote[]) ois.readObject();
            //Make sure you close all streams.
            ois.close();
            return quotes;
        }

        public HashSet<String> getFavoritesFromFile (String filePath) throws Exception {
            FileInputStream fis = getActivity().openFileInput(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashSet<String> favorites = (HashSet<String>) ois.readObject();
            //Make sure you close all streams.
            ois.close();
            return favorites;
        }

        private Quote[] getQuotesFromJson(String JSONStr) throws JSONException {
            if(JSONStr == null) return null;

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_TEXT = "text";
            final String OWM_IMAGEURI = "imageuri";
            final String OWM_ID = "id";
            final String OWM_BACKGROUNDCOLOR = "backgroundcolor";
            final String OWM_BODYTEXTCOLOR = "bodytextcolor";

            JSONObject entireJson = new JSONObject(JSONStr);
            JSONArray quoteArray = entireJson.getJSONArray(OWM_LIST);

            Quote[] result = new Quote[quoteArray.length()];
            for(int i = quoteArray.length() - 1; i >= 0 ; i--) {
                JSONObject jsonObject = quoteArray.getJSONObject(i);
                Quote q = new Quote();
                q.setSourceUri(jsonObject.getString(OWM_IMAGEURI));
                q.setQuoteText(jsonObject.getString(OWM_TEXT));
                q.setDisplayUri(MAIN_URL + "/v/" + jsonObject.getString(OWM_ID));
                q.setBodytextColor(jsonObject.getString(OWM_BODYTEXTCOLOR));
                q.setBackgroundColor(jsonObject.getString(OWM_BACKGROUNDCOLOR));
                result[quoteArray.length() - i - 1] = q;
            }

            return result;
        }

        protected void onPostExecute(Quote[] result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            if(result != null){
                // specify an adapter (see also next example)
                mQuotes = result;
                mAdapter = new MyAdapter(mQuotes);
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    }
}
