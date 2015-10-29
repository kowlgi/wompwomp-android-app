package com.agni.sunshine;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.agni.sunshine.util.ImageFetcher;
import com.agni.sunshine.util.Utils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.ocpsoft.pretty.time.PrettyTime;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by skyfishjy on 10/31/14.
 */
public class MyCursorAdapter extends BaseCursorAdapter<MyCursorAdapter.ViewHolder>{

    private ImageFetcher mImageFetcher = null;

    public MyCursorAdapter(Context context, Cursor cursor, ImageFetcher imageFetcher){
        super(context,cursor);
        mImageFetcher = imageFetcher;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public SquareImageView imageView;
        public TextView textView;
        public ImageButton shareButton;
        public ImageButton favoriteButton;
        public TextView createdOnView;
        public TextView numfavoritesView;
        public TextView numsharesView;


        public ViewHolder(View itemView) {
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_main, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        MyListItem myListItem = MyListItem.fromCursor(cursor);

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
    }
}