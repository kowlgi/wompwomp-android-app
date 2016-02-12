package co.wompwomp.sunshine.video;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.android.exoplayer.util.Util;

import co.wompwomp.sunshine.MyCursorAdapter;
import timber.log.Timber;

/**
 * Created by kowlgi on 2/8/16.
 */
public class WompwompPlayer implements ExoPlayer.Listener, MediaCodecVideoTrackRenderer.EventListener {
    public static final int RENDERER_COUNT = 1;
    public static final int TYPE_VIDEO = 0;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 10;
    private Context mContext;
    private MediaCodecVideoTrackRenderer mVideoRenderer;
    private final Handler mHandler;
    private final ExoPlayer player;
    private final PlayerControl playerControl;
    private int mRendererBuildingState;
    private Surface mSurface;
    MyCursorAdapter mAdapter;

    public WompwompPlayer(Context context, MyCursorAdapter adapter) {
        mContext = context;
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        player.addListener(this);
        playerControl = new PlayerControl(player);
        mHandler = new Handler();
        mRendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        mAdapter = adapter;

        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                String text = "playWhenReady=" + playWhenReady + ", playbackState=";
                switch (playbackState) {
                    case ExoPlayer.STATE_BUFFERING:
                        text += "buffering";
                        break;
                    case ExoPlayer.STATE_ENDED:
                        text += "ended";
                        player.seekTo(0);
                        break;
                    case ExoPlayer.STATE_IDLE:
                        text += "idle";
                        break;
                    case ExoPlayer.STATE_PREPARING:
                        text += "preparing";
                        break;
                    case ExoPlayer.STATE_READY:
                        text += "ready";
                        player.seekTo(0);
                        break;
                    default:
                        text += "unknown";
                        break;
                }
                Log.d("20672067", text);
            }

            @Override
            public void onPlayWhenReadyCommitted() {
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.d("20672067", "somethingwrong:" + "onPlayerError:" + error.toString());

            }
        });
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        // do nothing
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        // do nothing
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        // Do nothing.
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        Timber.d("Video drawn to surface");
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        mAdapter.resizeSurface(width, height);
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        Timber.d("Video dropped frames");
    }

    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
        // do nothing
    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        // do nothing
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                     long initializationDurationMs) {
        //do nothing
    }

    public void prepare(Uri videoUri) {
        if (mRendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        mVideoRenderer = null;
        mRendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        buildRenderers(videoUri);
    }

    private void buildRenderers(Uri videoUri) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        String userAgent = Util.getUserAgent(mContext, "WompwompPlayer");
        DataSource dataSource = new DefaultUriDataSource(mContext, null, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(videoUri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        mVideoRenderer = new MediaCodecVideoTrackRenderer(
                mContext,
                sampleSource,
                MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                -1,
                mHandler,
                this,
                -1);
        TrackRenderer[] renderers = new TrackRenderer[RENDERER_COUNT];
        renderers[TYPE_VIDEO] = mVideoRenderer;
        player.prepare(renderers);
        mRendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }

    private void pushSurface(boolean blockForSurfacePush) {
        if (mVideoRenderer == null) {
            return;
        }

        if (blockForSurfacePush) {
            player.blockingSendMessage(
                    mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
        } else {
            player.sendMessage(
                    mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
        }
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void release() {
        mRendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        mSurface = null;
        player.release();
        mVideoRenderer = null;
    }

    public void blockingClearSurface() {
        mSurface = null;
        pushSurface(true);
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
        pushSurface(false);
    }

    public void stop() {
        player.stop();
    }
}

