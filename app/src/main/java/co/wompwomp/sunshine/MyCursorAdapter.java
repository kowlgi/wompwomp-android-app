package co.wompwomp.sunshine;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
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
import co.wompwomp.sunshine.video.WompwompPlayer;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import timber.log.Timber;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.plattysoft.leonids.ParticleSystem;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

public class MyCursorAdapter extends BaseCursorAdapter<MyCursorAdapter.ViewHolder>  implements SurfaceHolder.Callback {

    private ImageFetcher mImageFetcher = null;
    private Context mContext = null;
    private ShareDialog mShareDialog = null;
    private HashSet<String> mLikes = null;
    private HashSet<String> mVideoDownloadsInProgress = null;

    /* the downloaded videos hashset serves two purposes: 1. addition of an item to this hashset is
       an indication that the video file was downloaded successfully 2. by storing hashset to file,
       we can compare entries in the hashset to entries in the db. For hashset entries that don't
       have a corresponding db entry, delete those entries from the hashset and
       corresponding files from the filesystem (helps save storage on user's phone)
     */
    private HashSet<String> mDownloadedVideos = null;

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
        mVideoDownloadsInProgress = new HashSet<>();
        if (fileExists(mContext, WompWompConstants.LIKES_FILENAME)) {
            try {
                mLikes =  getKeysFromFile(WompWompConstants.LIKES_FILENAME);
            } catch (java.lang.Exception e) {
                Timber.e("Error from file read operation ", e);
                mLikes = new HashSet<>();
                e.printStackTrace();
            }
        } else mLikes = new HashSet<>();

        if (fileExists(mContext, WompWompConstants.VIDEO_DOWNLOADS_FILENAME)) {
            try {
                mDownloadedVideos = getKeysFromFile(WompWompConstants.VIDEO_DOWNLOADS_FILENAME);
            } catch (java.lang.Exception e) {
                Timber.e("Error from file read operation ", e.toString());
                e.printStackTrace();
                mDownloadedVideos = new HashSet<>();
            }
        } else {
            mDownloadedVideos = new HashSet<>();
        }
    }

    private boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        return file != null && file.exists();
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
        mVideoDownloadsInProgress.clear();
        try {
            FileOutputStream likesfos = mContext.openFileOutput(WompWompConstants.LIKES_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream likesoos = new ObjectOutputStream(likesfos);
            likesoos.writeObject(mLikes);
            likesoos.close();
            likesfos.close();

            FileOutputStream videosfos = mContext.openFileOutput(WompWompConstants.VIDEO_DOWNLOADS_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream videosoos = new ObjectOutputStream(videosfos);
            videosoos.writeObject(mDownloadedVideos);
            videosoos.close();
            videosfos.close();

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
        public TextView viewsText;
        public TextView numViews;
        public SurfaceView surfaceView;
        public String videoUri;
        public ImageView playButton;
        public ProgressBar videoProgress;

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
            facebookshareButton = (ImageButton) itemView.findViewById(R.id.facebook_share_button);
            viewsText = (TextView) itemView.findViewById(R.id.viewsText);
            numViews = (TextView) itemView.findViewById(R.id.numViews);
            surfaceView = (SurfaceView) itemView.findViewById(R.id.surfaceView);
            surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            surfaceView.setZOrderOnTop(true);
            videoUri = "";
            playButton = (ImageView) itemView.findViewById(R.id.playButton);
            videoProgress = (ProgressBar) itemView.findViewById(R.id.video_progress);

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
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            lp.width = displayMetrics.widthPixels;
            lp.height = displayMetrics.widthPixels * height / width;

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
        if(mDownloadedVideos.contains(filename) && fileExists(mContext, filename)) {
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

                if(cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                    holder.videoUri = myListItem.videoUri;
                    String filename = URLUtil.guessFileName(holder.videoUri, null, null);

                    if(mDownloadedVideos.contains(filename) || mVideoDownloadsInProgress.contains(filename)) {
                        // video's either already downloaded or a download is in progress
                    } else {
                        DownloadTask downloadTask = new DownloadTask();
                        DownloadTaskParams params = new DownloadTaskParams(myListItem.videoUri, myListItem.fileSize);
                        if(Utils.hasHoneycomb()) downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
                        else downloadTask.execute(params);
                        mVideoDownloadsInProgress.add(filename);
                    }

                    holder.viewsText.setVisibility(View.VISIBLE);
                    holder.numViews.setVisibility(View.VISIBLE);
                    holder.playButton.setVisibility(View.VISIBLE);
                    if(!mViewCounts.containsKey(myListItem.id)){
                        mViewCounts.put(myListItem.id, myListItem.numPlays);
                    }
                    holder.numViews.setText(MyListItem.format(mViewCounts.get(myListItem.id)));
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
                if(!mShareCounts.containsKey(myListItem.id)){
                    mShareCounts.put(myListItem.id, myListItem.numShares);
                }
                holder.numsharesView.setText(MyListItem.format(mShareCounts.get(myListItem.id)));

                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            playVideo(holder);
                            holder.videoProgress.setVisibility(View.VISIBLE);
                            Integer updatedViewCount = mViewCounts.get(myListItem.id) + 1;
                            mViewCounts.put(myListItem.id, updatedViewCount);
                            holder.numViews.setText(MyListItem.format(updatedViewCount));
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
                            if (!isVideoReadyForSharing(filename)) {
                                Utils.showVideoNotReadyForSharingYetToast(mContext);
                                return;
                            }
                        }

                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: unspecified")
                                .putContentId(myListItem.id));

                        Integer updatedShareCount = mShareCounts.get(myListItem.id) + 1;
                        mShareCounts.put(myListItem.id, updatedShareCount);
                        holder.numsharesView.setText(MyListItem.format(updatedShareCount));
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, mContext);

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
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, mContext);

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
                        String filename = null;
                        if (cardType == WompWompConstants.TYPE_VIDEO_CONTENT_CARD) {
                            filename = URLUtil.guessFileName(myListItem.videoUri, null, null);
                            if (!isVideoReadyForSharing(filename)) {
                                Utils.showVideoNotReadyForSharingYetToast(mContext);
                                return;
                            }
                        }

                        Answers.getInstance().logShare(new ShareEvent().putMethod("Destination: whatsapp")
                                .putContentId(myListItem.id));

                        Integer updatedShareCount = mShareCounts.get(myListItem.id) + 1;
                        mShareCounts.put(myListItem.id, updatedShareCount);
                        holder.numsharesView.setText(MyListItem.format(updatedShareCount));
                        Utils.postToWompwomp(FeedContract.ITEM_SHARE_URL + myListItem.id, mContext);

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
                    }
                });

                holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String URL;
                        if (myListItem.favorite) {
                            // she likes me not :(
                            if (myListItem.numFavorites > 0) {
                                holder.numfavoritesView.setText(MyListItem.format(myListItem.numFavorites));
                            }
                            mLikes.remove(myListItem.id);
                            holder.favoriteButton.setImageResource(R.drawable.ic_favorite_lightred_24dp);
                            myListItem.favorite = false;

                            URL = FeedContract.ITEM_UNFAVORITE_URL;
                            Answers.getInstance().logCustom(new CustomEvent("Unlike button clicked")
                                    .putCustomAttribute("itemid", myListItem.id));
                        } else {
                            // she likes me :)
                            holder.numfavoritesView.setText(MyListItem.format(myListItem.numFavorites + 1));
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

    private static class DownloadTaskParams {
        String url;
        Integer fileSize;

        DownloadTaskParams(String url, Integer fileSize) {
            this.url = url;
            this.fileSize = fileSize;
        }
    }

    // usually, subclasses of AsyncTask are declared inside the activity class.
// that way, you can easily modify the UI thread from here
    private class DownloadTask extends AsyncTask<DownloadTaskParams, Integer, String> {

        private String filename;
        private Integer fileSize;

        public DownloadTask() {
            this.filename = "";
            this.fileSize = 0;
        }

        @Override
        protected String doInBackground(DownloadTaskParams... params) {
            OkHttpClient client = new OkHttpClient();
            String result = null;
            fileSize = params[0].fileSize;
            ResponseBody body = null;
            try {
                URL url = new URL(params[0].url);
                Call call = client.newCall(new Request.Builder().url(url).get().build());
                Response response = call.execute();
                body = response.body();

                filename = URLUtil.guessFileName(url.toString(), null, null);
                Timber.d("Video download in progress. filename: " + filename);
                FileOutputStream output = mContext.openFileOutput(filename, Context.MODE_PRIVATE);

                BufferedSink sink = Okio.buffer(Okio.sink(output));
                sink.writeAll(body.source());
                sink.close();
            } catch (Exception e) {
                result = e.toString();
            } finally {
                if(body != null) body.close();
            }
            return result;
        }

        protected void onPostExecute(String result) {
            if(result == null) {
                File videofile = new File(mContext.getFilesDir(), filename);
                if(fileSize == videofile.length()) {
                    mDownloadedVideos.add(filename);
                    mVideoDownloadsInProgress.remove(filename);
                }
            }
        }
    }

    private boolean isVideoReadyForSharing(String filename) {
        File videofile =  new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), filename);

        return videofile.exists() || mDownloadedVideos.contains(filename);
    }

}