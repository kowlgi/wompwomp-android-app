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
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kowlgi on 10/21/15.
 */
public class MainFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private static final String TAG = "MainFragment";
    private static final String MAIN_URL = "http://45.55.216.153:3000";
    private ImageFetcher mImageFetcher;
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private static final String JSON_FILENAME = "agnijson";

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

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity(), Math.round(dpHeight));
        mImageFetcher.setLoadingImage(R.drawable.geometry2);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);

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
        Quote[] mDataset;


        // Provide a direct reference to each of the views within a data item
        // Used to cache the views within the item layout for fast access
        public class ViewHolder extends RecyclerView.ViewHolder {
            // Your holder should contain a member variable
            // for any view that will be set as you render a row
            public SquareNetworkImageView imageView;
            public TextView textView;
            public String displayUri;
            public TextView shareButton;

            // We also create a constructor that accepts the entire item row
            // and does the view lookups to find each subview
            public ViewHolder(View itemView, String dUri) {
                // Stores the itemView in a public final member variable that can be used
                // to access the context from any ViewHolder instance.
                super(itemView);
                imageView = (SquareNetworkImageView) itemView.findViewById(R.id.imageView);
                textView = (TextView) itemView.findViewById(R.id.textView);
                shareButton = (TextView) itemView.findViewById(R.id.share_button);
                displayUri = dUri;
                View buttonView = (View) itemView.findViewById(R.id.share_button);
                buttonView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, displayUri);
                        shareIntent.setType("text/plain");

                        View parentView = (View) imageView.getParent();
                        Uri bmpUri = getLocalViewBitmapUri(parentView);
                        if (bmpUri != null) {
                            // Construct a ShareIntent with link to image
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                            shareIntent.setType("image/*");
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share"));
                    }
                });
            }
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

            MyAdapter.ViewHolder vh = new MyAdapter.ViewHolder(v, MAIN_URL/* default display URL*/);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

            mImageFetcher.loadImage(mDataset[position].getSourceUri(), holder.imageView);

            holder.textView.setMinHeight((int) Math.round(dpHeight * 0.20)); //min 20% of height
            holder.textView.setText(mDataset[position].getQuoteText());
            holder.textView.setTextColor(Color.parseColor(mDataset[position].getBodytextColor()));

            holder.shareButton.setTextColor(Color.parseColor(mDataset[position].getBodytextColor()));
            holder.displayUri = mDataset[position].getDisplayUri();

            View parentView = (View) holder.imageView.getParent();
            parentView.setBackgroundColor(Color.parseColor(mDataset[position].getBackgroundColor()));

            CardView cardView = (CardView) holder.itemView;
            cardView.setCardBackgroundColor(Color.parseColor(mDataset[position].getBackgroundColor()));
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

                if(JSONResponse != null) {
                    FileOutputStream fos = getActivity().openFileOutput(JSON_FILENAME, Context.MODE_PRIVATE);
                    fos.write(JSONResponse.getBytes());
                    fos.close();
                }

            }catch (IOException e) {
                Log.e(TAG, "Error reading from URL or writing to file", e);
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

            try {
                if(JSONResponse == null) {
                    JSONResponse = getStringFromFile(JSON_FILENAME);
                }
                return getQuotesFromJson(JSONResponse);
            } catch(java.lang.Exception e) {
                Log.e(TAG, "Error most likely from File Read operation ", e);
                e.printStackTrace();
            }

            return null;
        }

        /* Borrowed from http://stackoverflow.com/questions/12910503/read-file-as-string */
        public String convertStreamToString(InputStream is) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        }

        public String getStringFromFile (String filePath) throws Exception {
            FileInputStream fin = getActivity().openFileInput(filePath);
            String ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            return ret;
        }

        private Quote[] getQuotesFromJson(String JSONStr) throws JSONException {
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
                q.setDisplayUri(MAIN_URL+"/v/" + jsonObject.getString(OWM_ID));
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

                mAdapter = new MyAdapter(result);
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    }


}
