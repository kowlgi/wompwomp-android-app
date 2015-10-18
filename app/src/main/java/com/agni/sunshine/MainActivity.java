package com.agni.sunshine;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity{

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agni_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.quotesRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

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

        new UpdateFeedTask().execute();
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


            // We also create a constructor that accepts the entire item row
            // and does the view lookups to find each subview
            public ViewHolder(View itemView, String dUri) {
                // Stores the itemView in a public final member variable that can be used
                // to access the context from any ViewHolder instance.
                super(itemView);

                imageView = (SquareNetworkImageView) itemView.findViewById(R.id.imageView);
                textView = (TextView) itemView.findViewById(R.id.textView);
                displayUri = dUri;
                View buttonView = (View) itemView.findViewById(R.id.share_button);
                buttonView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        //Uri bmpUri = getLocalImageBitmapUri(imageView);
                        View linearView = (View) imageView.getParent();
                        Uri bmpUri = getLocalViewBitmapUri(linearView);

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out the original home of this cool picture -- " + displayUri);
                        shareIntent.setType("text/plain");
                        if (bmpUri != null) {
                            // Construct a ShareIntent with link to image
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                            shareIntent.setType("image/*");
                        }
                        else {
                            shareIntent.putExtra(Intent.EXTRA_TEXT, textView.getText());
                            shareIntent.setType("text/plain");
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share"));
                    }
                });
            }
        }

        // Returns the URI path to the Bitmap displayed in specified ImageView
        public Uri getLocalImageBitmapUri(SquareNetworkImageView networkImageview) {
            // Extract Bitmap from ImageView drawable
            final Bitmap bmp  = ((BitmapDrawable) networkImageview.getDrawable()).getBitmap();
            if (bmp == null) {
                Log.v(TAG, "Unable to get Bitmap");
                return null;
            }
            // Store image to default external storage directory
            Uri bmpUri = null;
            try {
                if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1 &&
                        checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
                    return  null;
                }
                File file =  new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "share_image_" + System.currentTimeMillis() + ".png");
                file.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.close();
                bmpUri = Uri.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmpUri;
        }

        public Uri getLocalViewBitmapUri(View aView){
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
                        checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
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

            MyAdapter.ViewHolder vh = new MyAdapter.ViewHolder(v, "http://45.55.216.153:3000"/* default display URL*/);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

            holder.imageView.setDefaultImageResId(R.drawable.geometry2);
            holder.imageView.setErrorImageResId(R.drawable.geometry2);
            holder.imageView.setImageUrl(mDataset[position].getSourceUri(), VolleySingleton.getInstance().getImageLoader());
            holder.textView.setMinHeight((int) Math.round(dpHeight * 0.20)); //min 20% of height
            holder.textView.setText(mDataset[position].getQuoteText());
            holder.displayUri = mDataset[position].getDisplayUri();
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // The types specified here are the input data type, the progress type, and the result type
    public class UpdateFeedTask extends AsyncTask<Void, Void, Quote[]> {

        private final String LOG_TAG = UpdateFeedTask.class.getSimpleName();

        protected Quote[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String JSONResponse = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri.Builder ub = Uri.parse("http://45.55.216.153:3000/items?offset=-1").buildUpon();
                URL url = new URL(ub.build().toString());

                Log.v(LOG_TAG, url.toString());

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
            }catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally
            {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getQuotesFromJson(JSONResponse);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If there was an error in parsin json STOP
                e.printStackTrace();
            }

            return null;
        }

        private Quote[] getQuotesFromJson(String JSONStr) throws JSONException {
            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_TEXT = "text";
            final String OWM_IMAGEURI = "imageuri";
            final String OWM_ID = "id";

            JSONObject entireJson = new JSONObject(JSONStr);
            JSONArray quoteArray = entireJson.getJSONArray(OWM_LIST);

            Quote[] result = new Quote[quoteArray.length()];
            for(int i = quoteArray.length() - 1; i >= 0 ; i--) {
                JSONObject jsonObject = quoteArray.getJSONObject(i);
                result[quoteArray.length() - i - 1] = new Quote(
                        jsonObject.getString(OWM_IMAGEURI),
                        jsonObject.getString(OWM_TEXT),
                        "http://45.55.216.153:3000/v/" + jsonObject.getString(OWM_ID));
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
}