package co.wompwomp.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.ImageFetcher;
import co.wompwomp.sunshine.util.Utils;
import co.wompwomp.sunshine.util.VideoFileInfo;
import co.wompwomp.sunshine.video.WompwompPlayer;
import timber.log.Timber;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.plattysoft.leonids.ParticleSystem;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MyCursorAdapter extends BaseCursorAdapter<MyCursorAdapter.ViewHolder>  implements SurfaceHolder.Callback {

    private ImageFetcher mImageFetcher = null;
    private Context mContext = null;
    private ShareDialog mShareDialog = null;
    private HashSet<String> mLikes = null;

    /* we maintain these counts in this adapter as starting with v1.2 we don't do a db update when
       the user favorites or likes an item. Avoiding a db update is 1. needed, because a db update
       causes a cursor update, which causes a re-bind view that'll end up redrawing the surface view
       a.k.a a playing video will be stopped. 2. useful, because it helps improve performance by
       avoiding a db update
     */
    private HashMap<String, Integer> mShareCounts = null;
    private HashMap<String, Integer> mViewCounts = null;

    private WompwompPlayer mPlayer = null;
    private ContentCardViewHolder mItemPlayingVideo = null;

    public MyCursorAdapter(Context context, Cursor cursor, ImageFetcher imageFetcher, ShareDialog shareDialog) {
        super(cursor);
        mContext = context;
        mImageFetcher = imageFetcher;
        mShareDialog = shareDialog;
        mShareCounts = new HashMap<>();
        mViewCounts = new HashMap<>();
        if (Utils.fileExists(mContext, WompWompConstants.LIKES_FILENAME)) {
            try {
                mLikes =  getKeysFromFile(WompWompConstants.LIKES_FILENAME);
            } catch (java.lang.Exception e) {
                Timber.e("Error from file read operation ", e);
                mLikes = new HashSet<>();
                e.printStackTrace();
            }
        } else mLikes = new HashSet<>();
    }

    private HashSet<String> getKeysFromFile (String filePath) throws Exception {
        FileInputStream fis = mContext.openFileInput(filePath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        HashSet<String> obj = (HashSet<String>) ois.readObject();
        ois.close();
        fis.close();
        return obj;
    }

    public void start() {
        mPlayer = new WompwompPlayer(mContext, this);
        mItemPlayingVideo = null;
    }

    public void stop(){
        mPlayer.release();
        mPlayer = null;

        /* we clear the share and like counts under the assumption that MainActivity::start() will
           sync items from the server, thus obtaining in the latest global values for these counts
         */
        mShareCounts.clear();
        mViewCounts.clear();

        try {
            FileOutputStream likesfos = mContext.openFileOutput(WompWompConstants.LIKES_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream likesoos = new ObjectOutputStream(likesfos);
            likesoos.writeObject(mLikes);
            likesoos.close();
            likesfos.close();

        } catch (java.io.FileNotFoundException fnf) {
            Timber.e("Error from file stream open operation ", fnf);
            fnf.printStackTrace();
        } catch (java.io.IOException ioe) {
            Timber.e("Error from file write operation ", ioe);
            ioe.printStackTrace();
        }
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
        public TextView numViews;
        public SurfaceView surfaceView;
        public String videoUri;
        public Integer videoFileSize;
        public ImageView playButton;
        public ProgressBar videoProgress;
        public TextView shareCountText;
        public TextView likesText;
        public TextView viewsText;
        public TextView dateHeader;
        public View allTimeTrending;
        public View lastWeekTrending;
        public View lastDayTrending;

        public ContentCardViewHolder(View itemView) {
            super(itemView);
            imageView = (SquareImageView) itemView.findViewById(R.id.imageView);
            textView = (TextView) itemView.findViewById(R.id.textView);
            shareButton = (ImageButton) itemView.findViewById(R.id.share_button);
            favoriteButton = (ImageButton) itemView.findViewById(R.id.favorite_button);
            whatsappshareButton = (ImageButton) itemView.findViewById(R.id.whatsapp_share_button);
            createdOnView = (TextView) itemView.findViewById(R.id.createdon);
            numfavoritesView = (TextView) itemView.findViewById(R.id.numLikes);
            numsharesView = (TextView) itemView.findViewById(R.id.numShares);
            facebookshareButton = (ImageButton) itemView.findViewById(R.id.facebook_share_button);
            viewsText = (TextView) itemView.findViewById(R.id.viewsText);
            numViews = (TextView) itemView.findViewById(R.id.numViews);
            surfaceView = (SurfaceView) itemView.findViewById(R.id.surfaceView);
            surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            surfaceView.setZOrderOnTop(true);
            videoUri = "";
            videoFileSize = 0;
            playButton = (ImageView) itemView.findViewById(R.id.playButton);
            videoProgress = (ProgressBar) itemView.findViewById(R.id.video_progress);
            shareCountText = (TextView) itemView.findViewById(R.id.shareCountText);
            viewsText = (TextView) itemView.findViewById(R.id.viewsText);
            likesText = (TextView) itemView.findViewById(R.id.likesText);
            dateHeader = (TextView) itemView.findViewById(R.id.dateHeader);
            allTimeTrending = itemView.findViewById(R.id.alltimepopularannotation);
            lastWeekTrending = itemView.findViewById(R.id.trendinglastweekannotation);
            lastDayTrending = itemView.findViewById(R.id.trendinglastdayannotation);

            int whatsappButtonVisibility = Utils.isPackageInstalled("com.whatsapp", mContext) ? View.VISIBLE : View.GONE;
            whatsappshareButton.setVisibility(whatsappButtonVisibility);
        }
    }

    // SurfaceHolder.Callback implementation
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
            resetSurfaceViewSize(mItemPlayingVideo.surfaceView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // here your custom logic to choose the view type
        MyListItem item = getListItem(position);
        if(item.cardType == WompWompConstants.TYPE_CONTENT_CARD &&
                item.videoUri != null &&
                item.videoUri.length() > 0) {
            return WompWompConstants.TYPE_VIDEO_CONTENT_CARD;
        }

        return item.cardType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder vh = null;
        switch(viewType) {
            case WompWompConstants.TYPE_CONTENT_CARD:
            case WompWompConstants.TYPE_VIDEO_CONTENT_CARD:{
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
            case WompWompConstants.TYPE_UPGRADE_CARD: {
                View itemView = LayoutInflater.from(mContext)
                        .inflate(R.layout.upgrade_card, parent, false);
                vh = new ViewHolder(itemView);
                break;
            }
        }
        return vh;
    }

    public void resizeSurface(int width, int height) {
        if(mItemPlayingVideo != null) {
            ViewGroup.LayoutParams lp = mItemPlayingVideo.surfaceView.getLayoutParams();
            View parent = (View)mItemPlayingVideo.surfaceView.getParent();
            int widthPixels = parent.getWidth();
            lp.width = widthPixels;
            lp.height = widthPixels * height / width;

            mItemPlayingVideo.videoProgress.setVisibility(View.GONE);
            mItemPlayingVideo.surfaceView.setLayoutParams(lp);
        }
    }

    private void resetSurfaceViewSize(SurfaceView sv) {
        ViewGroup.LayoutParams lp = sv.getLayoutParams();
        lp.width = 1;
        lp.height = 1;
        sv.setLayoutParams(lp);
    }

    private void playVideo(RecyclerView.ViewHolder VH) {
        if(mItemPlayingVideo != null) {
            mItemPlayingVideo.surfaceView.getHolder().removeCallback(this);
            mPlayer.blockingClearSurface();
            mItemPlayingVideo.videoProgress.setVisibility(View.GONE);
            resetSurfaceViewSize(mItemPlayingVideo.surfaceView);
        }

        mItemPlayingVideo = (ContentCardViewHolder) VH;

        Uri videoUri = Uri.parse(mItemPlayingVideo.videoUri);
        String filename = URLUtil.guessFileName(mItemPlayingVideo.videoUri, null, null);
        if(Utils.validVideoFile(mContext, filename, mItemPlayingVideo.videoFileSize)){
            videoUri = Uri.fromFile(mContext.getFileStreamPath(filename));
        }

        mPlayer.prepare(videoUri);
        mPlayer.setPlayWhenReady(true);
        if(mItemPlayingVideo.surfaceView.getHolder().getSurface() != null) {
            mPlayer.setSurface(mItemPlayingVideo.surfaceView.getHolder().getSurface());
        } else {
            mItemPlayingVideo.surfaceView.getHolder().addCallback(this);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder VH, Cursor cursor) {
        final Integer cardType = getItemViewType(cursor.getPosition());
        switch(cardType){
            case WompWompConstants.TYPE_CONTENT_CARD:
            case WompWompConstants.TYPE_VIDEO_CONTENT_CARD: {
                final ContentCardViewHolder holder = (ContentCardViewHolder) VH;
                final MyListItem myListItem = MyListItem.fromCursor(cursor);
                mImageFetcher.loadImage(myListItem.imageSourceUri, holder.imageView);

                DateTime currentItemDate = new DateTime(myListItem.createdOn);
                final int cursorPosition = cursor.getPosition();
                String dateHeader = "";
                if(cursor.moveToPrevious()) {
                    MyListItem previousListItem = MyListItem.fromCursor(cursor);
                    DateTime previousItemDate = new DateTime(previousListItem.createdOn);
                    if(previousItemDate.getDayOfYear() - currentItemDate.getDayOfYear() > 0) {
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("EEEE, MMMM d, yyyy");
                        dateHeader = currentItemDate.toString(fmt);
                    }
                    cursor.moveToPosition(cursorPosition);
                } else {
                    dateHeader = mContext.getResources().getString(R.string.today);
                }

                if(dateHeader.length() > 0) {
                    holder.dateHeader.setText(dateHeader.toUpperCase());
                    holder.dateHeader.setVisibility(View.VISIBLE);
                } else {
                    holder.dateHeader.setVisibility(View.GONE);
                }

                if(!myListItem.annotation.contains(WompWompConstants.ANNOTATION_ALL_TIME_POPULAR)) {
                    holder.allTimeTrending.setVisibility(View.GONE);
                } else {
                    holder.allTimeTrending.setVisibility(View.VISIBLE);
                }

                if(!myListItem.annotation.contains(WompWompConstants.ANNOTATION_TRENDING_THIS_WEEK)){
                    holder.lastWeekTrending.setVisibility(View.GONE);
                } else {
                    holder.lastWeekTrending.setVisibility(View.VISIBLE);
                }

                if(!myListItem.annotation.contains(WompWompConstants.ANNOTATION_TRENDING_TODAY)){
                    holder.lastDayTrending.setVisibility(View.GONE);
                } else {
                    holder.lastDayTrending.setVisibility(View.VISIBLE);
                }

                final ArrayList<VideoFileInfo> videoPrefetchList = new ArrayList<>();
                if(cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                    holder.videoUri = myListItem.videoUri;
                    holder.videoFileSize = myListItem.fileSize;
                    String filename = URLUtil.guessFileName(holder.videoUri, null, null);

                    if(Utils.validVideoFile(mContext, filename, myListItem.fileSize)) {
                        /* Create a prefetch list for WompWompConstants.MAX_NUM_PREFETCH_VIDEOS # of items
                           below current item */
                        int numVideos = 0;
                        while(cursor.moveToNext() && numVideos < WompWompConstants.MAX_NUM_PREFETCH_VIDEOS) {
                            numVideos++;
                            String videoUri = cursor.getString(WompWompConstants.COLUMN_VIDEOURI);
                            Integer fileSize = cursor.getInt(WompWompConstants.COLUMN_FILE_SIZE);
                            if(videoUri == null || videoUri.length() <= 0 ) continue;

                            String videofilename = URLUtil.guessFileName(videoUri, null, null);
                            if(Utils.validVideoFile(mContext, videofilename, fileSize)) continue;

                            VideoFileInfo vfi = new VideoFileInfo(videoUri, fileSize);
                            videoPrefetchList.add(vfi);
                        }
                        cursor.moveToPosition(cursorPosition);
                    } else {
                        ArrayList<VideoFileInfo> videoFileList = new ArrayList<>();
                        VideoFileInfo vfi = new VideoFileInfo(myListItem.videoUri, myListItem.fileSize);
                        videoFileList.add(vfi);
                        FileDownloaderService.startDownload(mContext, videoFileList);
                    }

                    holder.viewsText.setVisibility(View.VISIBLE);
                    holder.numViews.setVisibility(View.VISIBLE);
                    holder.playButton.setVisibility(View.VISIBLE);
                    if(!mViewCounts.containsKey(myListItem.id)){
                        mViewCounts.put(myListItem.id, myListItem.numPlays);
                    }
                    holder.numViews.setText(MyListItem.format(mViewCounts.get(myListItem.id)));
                    holder.viewsText.setText(mViewCounts.get(myListItem.id) == 1 ? R.string.view : R.string.views);
                    resetSurfaceViewSize(holder.surfaceView);
                } else {
                    holder.viewsText.setVisibility(View.GONE);
                    holder.numViews.setVisibility(View.GONE);
                    holder.playButton.setVisibility(View.GONE);
                }
                holder.videoProgress.setVisibility(View.GONE);

                if(myListItem.quoteText.isEmpty()) {
                    holder.textView.setVisibility(View.GONE);
                } else {
                    holder.textView.setVisibility(View.VISIBLE);
                    holder.textView.setText(myListItem.quoteText);
                }

                // In v1.1.6 we stored the likes info in the DB, in later versions we store them in a file
                myListItem.favorite = mLikes.contains(myListItem.id);

                if (myListItem.favorite) {
                    holder.favoriteButton.setImageResource(R.drawable.ic_favorite_red_24dp);
                } else {
                    holder.favoriteButton.setImageResource(R.drawable.ic_favorite_lightred_24dp);
                }

                String author;
                if(myListItem.author == null || myListItem.author.isEmpty()) {
                    author = mContext.getResources().getString(R.string.defaultAuthor);
                } else {
                    author = myListItem.author;
                }

                //http://www.flowstopper.org/2012/11/prettytime-and-joda-playing-nice.html
                PrettyTime prettyTime = new PrettyTime();
                LocalDateTime createdOn = LocalDateTime.parse(myListItem.createdOn, ISODateTimeFormat.dateTime());
                String timeAndAuthor = prettyTime.format(createdOn.toDateTime(DateTimeZone.UTC).toDate()) +
                        " by @" + author;
                holder.createdOnView.setText(timeAndAuthor);

                holder.numfavoritesView.setText(MyListItem.format(myListItem.numFavorites));
                holder.likesText.setText(myListItem.numFavorites == 1 ? R.string.like : R.string.likes);
                if(!mShareCounts.containsKey(myListItem.id)){
                    mShareCounts.put(myListItem.id, myListItem.numShares);
                }
                holder.numsharesView.setText(MyListItem.format(mShareCounts.get(myListItem.id)));
                holder.shareCountText.setText(mShareCounts.get(myListItem.id) == 1 ? R.string.share : R.string.shares);

                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            playVideo(holder);
                            holder.videoProgress.setVisibility(View.VISIBLE);
                            Integer updatedViewCount = mViewCounts.get(myListItem.id) + 1;
                            mViewCounts.put(myListItem.id, updatedViewCount);
                            holder.numViews.setText(MyListItem.format(updatedViewCount));
                            holder.viewsText.setText(updatedViewCount == 1 ? R.string.view : R.string.views);

                            // prefetch videos in the background
                            if(videoPrefetchList.size() > 0) {
                                FileDownloaderService.startDownload(mContext, videoPrefetchList);
                            }

                            Utils.postToWompwomp(FeedContract.ITEM_PLAY_URL + myListItem.id, mContext);
                            Answers.getInstance().logCustom(new CustomEvent("Video played")
                                    .putCustomAttribute("itemid", myListItem.id));

                        }else{
                            Intent zoomIntent = new Intent(mContext, ItemZoomActivity.class);
                            final Bitmap bmp = ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap();
                            ByteArrayOutputStream bs = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, bs);
                            zoomIntent.putExtra("byteArray", bs.toByteArray());
                            zoomIntent.putExtra("quoteText", myListItem.quoteText);
                            mContext.startActivity(zoomIntent);
                        }
                    }
                });

                holder.surfaceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            if(mItemPlayingVideo == holder) {
                                mPlayer.stop();
                                resetSurfaceViewSize(holder.surfaceView);
                            }
                        }
                    }
                });

                holder.shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String filename = null;
                        if (cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            filename = URLUtil.guessFileName(myListItem.videoUri, null, null);
                            if (!isVideoReadyForSharing(filename, myListItem.fileSize)) {
                                Utils.showVideoNotReadyForSharingYetToast(mContext);
                                return;
                            }
                        }

                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: unspecified")
                                .putContentId(myListItem.id));

                        Integer updatedShareCount = mShareCounts.get(myListItem.id) + 1;
                        mShareCounts.put(myListItem.id, updatedShareCount);
                        holder.numsharesView.setText(MyListItem.format(updatedShareCount));
                        holder.shareCountText.setText(updatedShareCount == 1 ? R.string.share : R.string.shares);
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, "general", mContext);

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        String caption;
                        if(myListItem.quoteText.length() > 0) {
                            caption = Utils.truncateAndAppendEllipsis(myListItem.quoteText, 80) + " - via " + FeedContract.ITEM_VIEW_URL + myListItem.id;
                        } else {
                            caption = "via " + FeedContract.ITEM_VIEW_URL + myListItem.id;
                        }
                        shareIntent.putExtra(Intent.EXTRA_TEXT, caption);
                        shareIntent.setType("text/plain");
                        if (cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            Uri videoUri = Utils.getLocalVideoUri(filename, mContext);
                            if (videoUri != null) {
                                shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
                                shareIntent.setType("video/*");
                            }
                        } else {
                            View parentView = (View) holder.imageView.getParent();
                            Uri bmpUri = Utils.getLocalViewBitmapUri(myListItem.id, parentView, mContext);
                            if (bmpUri != null) {
                                shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                shareIntent.setType("image/*");
                            }
                        }
                        Utils.showShareToast(mContext);
                        mContext.startActivity(Intent.createChooser(shareIntent,
                                mContext.getResources().getString(R.string.app_chooser_title)));
                        reinforceShareAndLikeIntent(myListItem.createdOn);
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

                        Integer updatedShareCount = mShareCounts.get(myListItem.id) + 1;
                        mShareCounts.put(myListItem.id, updatedShareCount);
                        holder.numsharesView.setText(MyListItem.format(updatedShareCount));
                        holder.shareCountText.setText(updatedShareCount == 1 ? R.string.share : R.string.shares);
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, "facebook", mContext);

                        ShareLinkContent content = new ShareLinkContent.Builder()
                                .setContentTitle(myListItem.quoteText)
                                .setContentUrl(Uri.parse(FeedContract.ITEM_VIEW_URL + myListItem.id))
                                .setImageUrl(Uri.parse(myListItem.imageSourceUri))
                                .setContentDescription("Install the app for your funniest minute every day")
                                .build();
                        mShareDialog.show(content);
                        Utils.showShareToast(mContext);
                        reinforceShareAndLikeIntent(myListItem.createdOn);
                    }
                });

                holder.whatsappshareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String filename = null;
                        if (cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            filename = URLUtil.guessFileName(myListItem.videoUri, null, null);
                            if (!isVideoReadyForSharing(filename, myListItem.fileSize)) {
                                Utils.showVideoNotReadyForSharingYetToast(mContext);
                                return;
                            }
                        }

                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: whatsapp")
                                .putContentId(myListItem.id));

                        Integer updatedShareCount = mShareCounts.get(myListItem.id) + 1;
                        mShareCounts.put(myListItem.id, updatedShareCount);
                        holder.numsharesView.setText(MyListItem.format(updatedShareCount));
                        holder.shareCountText.setText(updatedShareCount == 1 ? R.string.share : R.string.shares);
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, "whatsapp", mContext);

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        String caption;
                        if(myListItem.quoteText.length() > 0) {
                            caption = Utils.truncateAndAppendEllipsis(myListItem.quoteText, 80) + " - via " + FeedContract.ITEM_VIEW_URL + myListItem.id;
                        } else {
                            caption = "via " + FeedContract.ITEM_VIEW_URL + myListItem.id;
                        }
                        shareIntent.putExtra(Intent.EXTRA_TEXT, caption);
                        shareIntent.setType("text/plain");
                        if(cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            Uri videoUri = Utils.getLocalVideoUri(filename, mContext);
                            if (videoUri != null) {
                                shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
                                shareIntent.setType("video/*");
                            }
                        } else {
                            View parentView = (View) holder.imageView.getParent();
                            Uri bmpUri = Utils.getLocalViewBitmapUri(myListItem.id, parentView, mContext);
                            if (bmpUri != null) {
                                shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                shareIntent.setType("image/*");
                            }
                        }
                        shareIntent.setPackage("com.whatsapp");
                        Utils.showShareToast(mContext);
                        mContext.startActivity(shareIntent);
                        reinforceShareAndLikeIntent(myListItem.createdOn);
                    }
                });

                holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String URL;
                        if (myListItem.favorite) {
                            // she likes me not :(
                            holder.numfavoritesView.setText(MyListItem.format(myListItem.numFavorites));
                            holder.likesText.setText(myListItem.numFavorites == 1 ? R.string.like : R.string.likes);
                            mLikes.remove(myListItem.id);
                            holder.favoriteButton.setImageResource(R.drawable.ic_favorite_lightred_24dp);
                            myListItem.favorite = false;

                            URL = FeedContract.ITEM_UNFAVORITE_URL;
                            Answers.getInstance().logCustom(new CustomEvent("Unlike button clicked")
                                    .putCustomAttribute("itemid", myListItem.id));
                        } else {
                            // she likes me :)
                            int updatedLikeCount = myListItem.numFavorites + 1;
                            holder.numfavoritesView.setText(MyListItem.format(updatedLikeCount));
                            holder.likesText.setText(updatedLikeCount == 1 ? R.string.like : R.string.likes);
                            mLikes.add(myListItem.id);
                            holder.favoriteButton.setImageResource(R.drawable.ic_favorite_red_24dp);
                            myListItem.favorite = true;

                            URL = FeedContract.ITEM_FAVORITE_URL;
                            Answers.getInstance().logCustom(new CustomEvent("Like button clicked")
                                    .putCustomAttribute("itemid", myListItem.id));
                            new ParticleSystem((AppCompatActivity)mContext, 5, R.drawable.ic_favorite_red_12dp, 500)
                                    .setSpeedRange(0.2f, 0.5f)
                                    .oneShot(holder.favoriteButton, 5);
                        }

                        URL += myListItem.id;
                        Utils.postToWompwomp(URL, mContext);
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
            case WompWompConstants.TYPE_UPGRADE_CARD: {
                View rate_button = VH.itemView.findViewById(R.id.upgradeappbutton);
                rate_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Answers.getInstance().logCustom(new CustomEvent("Upgrade card clicked"));
                        Utils.showAppPageLaunchToast(mContext);
                        mContext.startActivity(Utils.getRateAppIntent(mContext));
                    }
                });
                break;
            }
        }
    }

    private MyListItem getListItem(int cursorPosition){
        int initialPosition = getCursor().getPosition();
        getCursor().moveToPosition(cursorPosition);
        MyListItem myListItem = MyListItem.fromCursor(getCursor());
        getCursor().moveToPosition(initialPosition); // restore cursor
        return myListItem;
    }

    private boolean isVideoReadyForSharing(String filename, Integer filesize) {
        File publicVideofile =  new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), filename);

        boolean validPublicVideoFile = publicVideofile.exists() &&
                publicVideofile.length() == filesize;

        return validPublicVideoFile || Utils.validVideoFile(mContext, filename, filesize);
    }

    private void reinforceShareAndLikeIntent(String contentTimestamp){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int shareCount = preferences.getInt(WompWompConstants.SHARE_LIKE_COUNTER, 0);
        preferences.edit().putInt(WompWompConstants.SHARE_LIKE_COUNTER, ++shareCount).apply();

        if(shareCount % WompWompConstants.DEFAULT_SHARE_APP_THRESHOLD == 0) {
            Uri uri = FeedContract.Entry.CONTENT_URI; // Get all entries
            String[] share_args = new String[] { WompWompConstants.WOMPWOMP_CTA_SHARE};
            mContext.getContentResolver().delete(uri, FeedContract.Entry.COLUMN_NAME_ENTRY_ID+"=?", share_args);

            DateTime ct = new DateTime(contentTimestamp);
            DateTime oneSecondBeforeContentTimestamp = ct.minusSeconds(1).withZone(DateTimeZone.UTC);
            mContext.getContentResolver().insert(FeedContract.Entry.CONTENT_URI, Utils.populateContentValues(WompWompConstants.TYPE_SHARE_CARD, oneSecondBeforeContentTimestamp.toString(), null));
        }
    }
}