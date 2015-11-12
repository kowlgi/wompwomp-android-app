package co.wompwomp.sunshine;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageFetcher;
import co.wompwomp.sunshine.util.Utils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.ocpsoft.pretty.time.PrettyTime;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import cz.msebera.android.httpclient.Header;

/**
 * Created by skyfishjy on 10/31/14.
 */
public class MyCursorAdapter extends BaseCursorAdapter<MyCursorAdapter.ViewHolder>{

    private ImageFetcher mImageFetcher = null;
    private final String TAG = "MyCursorAdapter";

    public MyCursorAdapter(Context context, Cursor cursor, ImageFetcher imageFetcher){
        super(context,cursor);
        mImageFetcher = imageFetcher;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ShareCardViewHolder extends ViewHolder {
        public ShareCardViewHolder (View itemView) {
            super(itemView);
        }
    }

    public class ContentCardViewHolder extends ViewHolder {
        public SquareImageView imageView;
        public TextView textView;
        public ImageButton shareButton;
        public ImageButton favoriteButton;
        public TextView createdOnView;
        public TextView numfavoritesView;
        public TextView numsharesView;

        public ContentCardViewHolder(View itemView) {
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

    @Override
    public int getItemViewType(int position) {
        // here your custom logic to choose the view type
        int initialPosition = getCursor().getPosition();
        getCursor().moveToPosition(position);
        int viewType = getCursor().getInt(WompWompConstants.COLUMN_CARD_TYPE);
        getCursor().moveToPosition(initialPosition); // restore cursor
        return viewType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder vh = null;
        switch(viewType) {
            case WompWompConstants.TYPE_CONTENT_CARD: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.content_card, parent, false);
                vh = new ContentCardViewHolder(itemView);
                break;
            }
            case WompWompConstants.TYPE_SHARE_CARD: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.share_card, parent, false);
                vh = new ShareCardViewHolder(itemView);
                break;
            }
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder VH, Cursor cursor) {

        switch(getItemViewType(cursor.getPosition())){
            // TODO: Use your own attributes to track content views in your app

            case WompWompConstants.TYPE_CONTENT_CARD: {
                final ContentCardViewHolder holder = (ContentCardViewHolder) VH;
                final MyListItem myListItem = MyListItem.fromCursor(cursor);
                Log.v(TAG, Integer.valueOf(cursor.getPosition()).toString());

                DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

                mImageFetcher.loadImage(myListItem.getImageSourceUri(), holder.imageView);

                holder.textView.setMinHeight((int) Math.round(dpHeight * 0.20)); //min 20% of height
                holder.textView.setText(myListItem.getQuoteText());

                if (myListItem.getFavorite()) {
                    holder.favoriteButton.setImageResource(R.drawable.ic_favorite_black_24dp);
                } else {
                    holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                }

                PrettyTime prettyTime = new PrettyTime();
                LocalDateTime createdOn = LocalDateTime.parse(myListItem.getCreatedOn(), ISODateTimeFormat.dateTime());
                //http://www.flowstopper.org/2012/11/prettytime-and-joda-playing-nice.html
                holder.createdOnView.setText(prettyTime.format(createdOn.toDateTime(DateTimeZone.UTC).toDate()));

                holder.numfavoritesView.setText(myListItem.getNumFavorites().toString());
                holder.numsharesView.setText(myListItem.getNumShares().toString());

                holder.shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: unspecified").putContentId(myListItem.getId()));

                        Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(myListItem.get_id().toString()).build();
                        ContentValues values = new ContentValues();
                        values.put(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, myListItem.getNumShares() + 1);
                        mContext.getContentResolver().update(updateUri, values, null, null);

                        AsyncHttpClient client = new AsyncHttpClient();
                        RequestParams params = new RequestParams();
                        client.post(FeedContract.BASE_URL + "/s/" + myListItem.getId(), params, new TextHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String res) {
                                // we received status 200 OK..wohoo!
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                                //do nothing
                            }
                        });

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, FeedContract.BASE_URL + "/v/" + myListItem.getId());
                        shareIntent.setType("text/plain");

                        View parentView = (View) holder.imageView.getParent();
                        Uri bmpUri = Utils.getLocalViewBitmapUri(parentView, mContext);
                        if (bmpUri != null) {
                            // Construct a ShareIntent with link to image
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                            shareIntent.setType("image/*");
                        }
                        mContext.startActivity(Intent.createChooser(shareIntent, "Share"));
                    }
                });

                holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String SUB_URL = "";
                        Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(myListItem.get_id().toString()).build();
                        ContentValues values = new ContentValues();
                        if (myListItem.getFavorite()) {
                            // she likes me not :(
                            if (myListItem.getNumFavorites() > 0) {
                                values.put(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, myListItem.getNumFavorites() - 1);
                            }

                            values.put(FeedContract.Entry.COLUMN_NAME_FAVORITE, 0);
                            SUB_URL = "/uf/";
                            Answers.getInstance().logCustom(new CustomEvent("Unlike button clicked").putCustomAttribute("id", myListItem.getId()));
                        } else {
                            // she likes me :)
                            values.put(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, myListItem.getNumFavorites() + 1);
                            values.put(FeedContract.Entry.COLUMN_NAME_FAVORITE, 1);
                            SUB_URL = "/f/";
                            Answers.getInstance().logCustom(new CustomEvent("Like button clicked").putCustomAttribute("id", myListItem.getId()));
                        }

                        mContext.getContentResolver().update(updateUri, values, null, null);
                        SUB_URL += myListItem.getId();

                        AsyncHttpClient client = new AsyncHttpClient();
                        RequestParams params = new RequestParams();
                        client.post(FeedContract.BASE_URL + SUB_URL, params, new TextHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String res) {
                                // called when response HTTP status is "200 OK"
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                                //do nothing
                            }
                        });
                    }
                });
                break;
            }
            case WompWompConstants.TYPE_SHARE_CARD: {
                //do nothing
                break;
            }
        }
    }
}