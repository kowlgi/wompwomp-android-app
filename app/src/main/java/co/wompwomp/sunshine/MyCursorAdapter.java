package co.wompwomp.sunshine;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import co.wompwomp.sunshine.helper.ItemTouchHelperAdapter;
import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageFetcher;
import co.wompwomp.sunshine.util.Utils;
import timber.log.Timber;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.ocpsoft.pretty.time.PrettyTime;
import com.plattysoft.leonids.ParticleSystem;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.ByteArrayOutputStream;

public class MyCursorAdapter extends BaseCursorAdapter<MyCursorAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private ImageFetcher mImageFetcher = null;
    private Context mContext = null;
    private ShareDialog mShareDialog = null;

    public MyCursorAdapter(Context context, Cursor cursor, ImageFetcher imageFetcher, ShareDialog shareDialog){
        super(cursor);
        mContext = context;
        mImageFetcher = imageFetcher;
        mShareDialog = shareDialog;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ContentCardViewHolder extends ViewHolder {
        public SquareImageView imageView;
        public TextView textView;
        public ImageButton shareButton;
        public ImageButton favoriteButton;
        public ImageButton whatsappshareButton;
        public ImageButton facebookshareButton;
        public TextView createdOnView;
        public TextView numfavoritesView;
        public TextView numsharesView;

        public ContentCardViewHolder(View itemView) {
            super(itemView);
            imageView = (SquareImageView) itemView.findViewById(R.id.imageView);
            textView = (TextView) itemView.findViewById(R.id.textView);
            shareButton = (ImageButton) itemView.findViewById(R.id.share_button);
            favoriteButton = (ImageButton) itemView.findViewById(R.id.favorite_button);
            whatsappshareButton = (ImageButton) itemView.findViewById(R.id.whatsapp_share_button);
            createdOnView = (TextView) itemView.findViewById(R.id.createdon);
            numfavoritesView = (TextView) itemView.findViewById(R.id.favoriteCount);
            numsharesView = (TextView) itemView.findViewById(R.id.shareCount);

            int whatsappButtonVisibility = Utils.isPackageInstalled("com.whatsapp", mContext) ? View.VISIBLE : View.GONE;
            whatsappshareButton.setVisibility(whatsappButtonVisibility);

            facebookshareButton = (ImageButton) itemView.findViewById(R.id.facebook_share_button);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // here your custom logic to choose the view type
        MyListItem item = getListItem(position);
        return item.cardType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder vh = null;
        switch(viewType) {
            case WompWompConstants.TYPE_CONTENT_CARD: {
                View itemView = LayoutInflater.from(mContext)
                        .inflate(R.layout.content_card, parent, false);
                vh = new ContentCardViewHolder(itemView);
                break;
            }
            case WompWompConstants.TYPE_SHARE_CARD: {
                View itemView = LayoutInflater.from(mContext)
                        .inflate(R.layout.share_card, parent, false);
                vh = new ViewHolder(itemView);
                break;
            }
            case WompWompConstants.TYPE_RATE_CARD: {
                View itemView = LayoutInflater.from(mContext)
                        .inflate(R.layout.rate_card, parent, false);
                vh = new ViewHolder(itemView);
                break;
            }
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder VH, Cursor cursor) {

        switch(getItemViewType(cursor.getPosition())){

            case WompWompConstants.TYPE_CONTENT_CARD: {
                final ContentCardViewHolder holder = (ContentCardViewHolder) VH;
                final MyListItem myListItem = MyListItem.fromCursor(cursor);

                DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

                mImageFetcher.loadImage(myListItem.imageSourceUri, holder.imageView);

                holder.imageView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent zoomIntent = new Intent(mContext, ItemZoomActivity.class);
                        final Bitmap bmp  = ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap();
                        ByteArrayOutputStream bs = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bs);
                        zoomIntent.putExtra("byteArray", bs.toByteArray());
                        zoomIntent.putExtra("quoteText", myListItem.quoteText);
                        mContext.startActivity(zoomIntent);
                    }
                });

                if(myListItem.quoteText.isEmpty()) {
                    holder.textView.setVisibility(View.GONE);
                } else {
                    holder.textView.setMinHeight((int) Math.round(dpHeight * 0.20)); //min 20% of height
                    holder.textView.setText(myListItem.quoteText);
                    Timber.d("Quote: " + myListItem.quoteText + ", link: " + myListItem.imageSourceUri);
                }

                if (myListItem.favorite) {
                    holder.favoriteButton.setImageResource(R.drawable.ic_favorite_red_24dp);
                } else {
                    holder.favoriteButton.setImageResource(R.drawable.ic_favorite_lightred_24dp);
                }

                PrettyTime prettyTime = new PrettyTime();
                LocalDateTime createdOn = LocalDateTime.parse(myListItem.createdOn, ISODateTimeFormat.dateTime());
                String author;
                if(myListItem.author == null || myListItem.author.isEmpty()) {
                    author = mContext.getResources().getString(R.string.defaultAuthor);
                } else {
                    author = myListItem.author;
                }
                //http://www.flowstopper.org/2012/11/prettytime-and-joda-playing-nice.html
                String timeAndAuthor = prettyTime.format(createdOn.toDateTime(DateTimeZone.UTC).toDate()) +
                        " by @" + author;
                holder.createdOnView.setText(timeAndAuthor);

                holder.numfavoritesView.setText(MyListItem.format(myListItem.numFavorites));
                holder.numsharesView.setText(MyListItem.format(myListItem.numShares));

                holder.shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: unspecified")
                                .putContentId(myListItem.id));

                        Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(myListItem._id.toString()).build();
                        ContentValues values = new ContentValues();
                        values.put(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, myListItem.numShares + 1);
                        mContext.getContentResolver().update(updateUri, values, null, null);

                        WompWompHTTPParams params = new WompWompHTTPParams(mContext);
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, params);

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                Utils.truncateAndAppendEllipsis(myListItem.quoteText, 80) + " - via " + FeedContract.ITEM_VIEW_URL + myListItem.id);
                        shareIntent.setType("text/plain");

                        View parentView = (View) holder.imageView.getParent();
                        Uri bmpUri = Utils.getLocalViewBitmapUri(myListItem.id, parentView, mContext);
                        if (bmpUri != null) {
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                            shareIntent.setType("image/*");
                        }

                        Utils.showShareToast(mContext);
                        mContext.startActivity(Intent.createChooser(shareIntent,
                                mContext.getResources().getString(R.string.app_chooser_title)));
                    }
                });

                holder.facebookshareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!ShareDialog.canShow(ShareLinkContent.class)) {
                            Utils.showCannotShareToast(mContext);
                            return;
                        }

                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: facebook")
                                .putContentId(myListItem.id));
                        Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(myListItem._id.toString()).build();
                        ContentValues values = new ContentValues();
                        values.put(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, myListItem.numShares + 1);
                        mContext.getContentResolver().update(updateUri, values, null, null);

                        WompWompHTTPParams params = new WompWompHTTPParams(mContext);
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, params);

                        ShareLinkContent content = new ShareLinkContent.Builder()
                                .setContentTitle(myListItem.quoteText)
                                .setContentUrl(Uri.parse(FeedContract.ITEM_VIEW_URL + myListItem.id))
                                .setImageUrl(Uri.parse(myListItem.imageSourceUri))
                                .setContentDescription("Install the app for your funniest minute every day")
                                .build();
                        mShareDialog.show(content);
                        Utils.showShareToast(mContext);
                    }
                });

                holder.whatsappshareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: whatsapp")
                                .putContentId(myListItem.id));

                        Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(myListItem._id.toString()).build();
                        ContentValues values = new ContentValues();
                        values.put(FeedContract.Entry.COLUMN_NAME_NUM_SHARES, myListItem.numShares + 1);
                        mContext.getContentResolver().update(updateUri, values, null, null);

                        WompWompHTTPParams params = new WompWompHTTPParams(mContext);
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, params);

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                Utils.truncateAndAppendEllipsis(myListItem.quoteText, 80) + " - via " + FeedContract.ITEM_VIEW_URL + myListItem.id);
                        shareIntent.setType("text/plain");

                        View parentView = (View) holder.imageView.getParent();
                        Uri bmpUri = Utils.getLocalViewBitmapUri(myListItem.id, parentView, mContext);
                        if (bmpUri != null) {
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                            shareIntent.setType("image/*");
                        }
                        shareIntent.setPackage("com.whatsapp");
                        Utils.showShareToast(mContext);
                        mContext.startActivity(shareIntent);
                    }
                });

                holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String URL;
                        Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(myListItem._id.toString()).build();
                        ContentValues values = new ContentValues();
                        if (myListItem.favorite) {
                            // she likes me not :(
                            if (myListItem.numFavorites > 0) {
                                values.put(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, myListItem.numFavorites - 1);
                            }

                            values.put(FeedContract.Entry.COLUMN_NAME_FAVORITE, 0);
                            URL = FeedContract.ITEM_UNFAVORITE_URL;
                            Answers.getInstance().logCustom(new CustomEvent("Unlike button clicked")
                                    .putCustomAttribute("itemid", myListItem.id));
                        } else {
                            // she likes me :)
                            values.put(FeedContract.Entry.COLUMN_NAME_NUM_FAVORITES, myListItem.numFavorites + 1);
                            values.put(FeedContract.Entry.COLUMN_NAME_FAVORITE, 1);
                            URL = FeedContract.ITEM_FAVORITE_URL;
                            Answers.getInstance().logCustom(new CustomEvent("Like button clicked")
                                    .putCustomAttribute("itemid", myListItem.id));
                            new ParticleSystem((AppCompatActivity)mContext, 5, R.drawable.ic_favorite_red_12dp, 500)
                                    .setSpeedRange(0.2f, 0.5f)
                                    .oneShot(holder.favoriteButton, 5);
                        }

                        mContext.getContentResolver().update(updateUri, values, null, null);
                        URL += myListItem.id;

                        WompWompHTTPParams params = new WompWompHTTPParams(mContext);
                        Utils.postToWompwomp(URL, params);
                    }
                });
                break;
            }
            case WompWompConstants.TYPE_SHARE_CARD: {
                View share_button = VH.itemView.findViewById(R.id.shareappbutton);
                share_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Answers.getInstance().logCustom(new CustomEvent("Share card clicked"));
                        Utils.showShareToast(mContext);
                        mContext.startActivity(Intent.createChooser(Utils.getShareAppIntent(mContext),
                                mContext.getResources().getString(R.string.app_chooser_title)));
                    }
                });
                break;
            }
            case WompWompConstants.TYPE_RATE_CARD: {
                View rate_button = VH.itemView.findViewById(R.id.rateappbutton);
                rate_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Answers.getInstance().logCustom(new CustomEvent("Rate card clicked"));
                        Utils.showAppPageLaunchToast(mContext);
                        mContext.startActivity(Utils.getRateAppIntent(mContext));
                    }
                });
                break;
            }
        }
    }

    @Override
    public void onItemDismiss(int position, final RecyclerView rv) {
        MyListItem item = getListItem(position);
        final Uri updateUri = FeedContract.Entry.CONTENT_URI.buildUpon()
                .appendPath(item._id.toString()).build();
        ContentValues values = new ContentValues();

        values.put(FeedContract.Entry.COLUMN_NAME_DISMISS_ITEM, 1);
        mContext.getContentResolver().update(updateUri, values, null, null);

        final String undoMessage = mContext.getResources().getString(R.string.undoPostMessage);
        final String restoreMessage = mContext.getResources().getString(R.string.restorePostMessage);
        Snackbar snackbar = Snackbar
                .make(rv, undoMessage, Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ContentValues values = new ContentValues();
                        values.put(FeedContract.Entry.COLUMN_NAME_DISMISS_ITEM, 0);
                        mContext.getContentResolver().update(updateUri, values, null, null);
                        Snackbar snackbar1 = Snackbar.make(rv, restoreMessage, Snackbar.LENGTH_SHORT);
                        snackbar1.show();
                    }
                });

        snackbar.show();

        WompWompHTTPParams params = new WompWompHTTPParams(mContext);
        Utils.postToWompwomp(FeedContract.ITEM_DISMISS_URL + item.id, params);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        //Don't implement this one
        return true;
    }

    private MyListItem getListItem(int cursorPosition){
        int initialPosition = getCursor().getPosition();
        getCursor().moveToPosition(cursorPosition);
        MyListItem myListItem = MyListItem.fromCursor(getCursor());
        getCursor().moveToPosition(initialPosition); // restore cursor
        return myListItem;
    }
}