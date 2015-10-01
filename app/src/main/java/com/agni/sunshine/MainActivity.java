package com.agni.sunshine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {

    private List<Quote> myQuotes = new ArrayList<Quote>();
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agni_main);
        mImageLoader = VolleySingleton.getInstance().getImageLoader();

        populateQuotesList();
        populateListView();
    }

    private void populateQuotesList() {
        myQuotes.add(new Quote("https://c2.staticflickr.com/2/1196/1065036310_d826f46270_b.jpg",
                "Do one thing every day that scares you. ― Eleanor Roosevelt") );
        myQuotes.add(new Quote("https://c1.staticflickr.com/1/192/504251019_ffc94c77b5_b.jpg",
                "We are what we pretend to be, so we must be careful about what we pretend to be.― Kurt Vonnegut, Mother Night"));
        myQuotes.add(new Quote("https://c1.staticflickr.com/1/17/92230866_713ae1eeef_b.jpg",
                "Sometimes you wake up. Sometimes the fall kills you. And sometimes, when you fall, you fly.― Neil Gaiman, The Sandman, Vol. 6: Fables and Reflections"));
        myQuotes.add(new Quote("https://c1.staticflickr.com/3/2140/2422023906_c6522c014d_b.jpg",
                "When we love, we always strive to become better than we are. When we strive to become better than we are, everything around us becomes better too.― Paulo Coelho, The Alchemist") );
        myQuotes.add(new Quote("https://c2.staticflickr.com/4/3140/2971576313_11f623b340_b.jpg",
                "What's meant to be will always find a way ― Trisha Yearwood"));
        myQuotes.add(new Quote("https://c2.staticflickr.com/6/5058/5543747770_7d37c98a54_b.jpg",
                "The flower that blooms in adversity is the rarest and most beautiful of all.― Walt Disney Company, Mulan"));
        myQuotes.add(new Quote("https://c1.staticflickr.com/3/2870/11335297394_21f07b19d7_b.jpg",
                "We delight in the beauty of the butterfly, but rarely admit the changes it has gone through to achieve that beauty.― Maya Angelou") );
        myQuotes.add(new Quote("https://c1.staticflickr.com/1/43/122094097_8175fdee9b_b.jpg",
                "You never have to change anything you got up in the middle of the night to write.― Saul Bellow"));
        myQuotes.add(new Quote("https://c1.staticflickr.com/9/8306/7989621033_721222caf2_b.jpg",
                "The unexamined life is not worth living.― Socrates"));
    }

    private void populateListView() {
        ArrayAdapter<Quote> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.quotesListView);
        list.setAdapter(adapter);
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
            Quote currentQuote = myQuotes.get(position);
            NetworkImageView networkImageView = (NetworkImageView) itemView.findViewById(R.id.imageView);

            networkImageView.setImageUrl(currentQuote.getUri(), mImageLoader);
            networkImageView.setDefaultImageResId(R.drawable.landscape27);
            networkImageView.setErrorImageResId(R.drawable.landscape27);

            TextView quotetextview = (TextView) itemView.findViewById(R.id.textView);
            quotetextview.setText(currentQuote.getQuotetext());

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
}
