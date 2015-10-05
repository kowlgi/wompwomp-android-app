package com.agni.sunshine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private List<Quote> myQuotes = new ArrayList<Quote>();
    private ImageLoader mImageLoader;
    private SwipeRefreshLayout mSwipeContainer;
    ArrayAdapter<Quote> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agni_main);
        mImageLoader = VolleySingleton.getInstance().getImageLoader();
        populateListView();
        new UpdateFeedTask().execute();
    }

    private void populateListView() {
        mAdapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.quotesListView);
        list.setAdapter(mAdapter);
    }

    private class MyListAdapter extends ArrayAdapter<Quote> {

        public MyListAdapter() {
            super(MainActivity.this, R.layout.image_main, myQuotes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Make sure we have a view to work with
            View itemView = convertView;
            if(itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.image_main, parent, false);

            }

            // Find the quote to work with
            Quote currentQuote = getItem(position);
            if(currentQuote != null) {
                NetworkImageView networkImageView = (NetworkImageView) itemView.findViewById(R.id.imageView);

                networkImageView.setImageUrl(currentQuote.getUri(), mImageLoader);
                networkImageView.setDefaultImageResId(R.drawable.landscape27);
                networkImageView.setErrorImageResId(R.drawable.landscape27);

                TextView quotetextview = (TextView) itemView.findViewById(R.id.textView);
                quotetextview.setText(currentQuote.getQuotetext());
            }
            return itemView;
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
                Uri.Builder ub = Uri.parse("http://192.168.0.9:3000/items?offset=-1&limit=10").buildUpon();
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

            JSONObject forecastJson = new JSONObject(JSONStr);
            JSONArray quoteArray = forecastJson.getJSONArray(OWM_LIST);

            Quote[] result = new Quote[quoteArray.length()];
            for(int i = 0; i < quoteArray.length(); i++) {
                result[i] = new Quote(quoteArray.getJSONObject(i).getString(OWM_IMAGEURI),
                        quoteArray.getJSONObject(i).getString(OWM_TEXT));
            }

            return result;
        }

        protected void onPostExecute(Quote[] result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            if(result != null){
                mAdapter.clear();
                for  (Quote quote : result) {
                    mAdapter.insert(quote, 0);
                }
            }
        }
    }
}