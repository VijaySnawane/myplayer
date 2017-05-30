package com.dynu.vijay.myapplication;

import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;
import java.util.Map;

import static com.dynu.vijay.myapplication.VideoPlayer.CURRENT_STATE_AUTO_COMPLETE;
import static com.dynu.vijay.myapplication.VideoPlayer.CURRENT_STATE_ERROR;
import static com.dynu.vijay.myapplication.VideoPlayer.CURRENT_STATE_PAUSE;

public class MyFragment2 extends Fragment implements TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {


    public static String TAG = "JieCaoVideoPlayer";
    public static SurfaceTexture savedSurfaceTexture;
    public static String CURRENT_PLAYING_URL;
    public static boolean CURRENT_PLING_LOOP;
    public static Map<String, String> MAP_HEADER_DATA;
    public static ResizeTextureView textureView;
    public static boolean mediaPlaying = false;
    public MediaPlayer mediaPlayer = new MediaPlayer();
    public int currentVideoWidth = 0;
    public int currentVideoHeight = 0;
    VideoPlayer jcVideoPlayerStandard;
    Handler mainThreadHandler;

    public static MyFragment2 newInstance() {
        return new MyFragment2();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_fragment, container, false);
        mainThreadHandler = new Handler();
        jcVideoPlayerStandard = (VideoPlayer) view.findViewById(R.id.videoplayer);
        jcVideoPlayerStandard.setUp("https://view.vzaar.com/5822839/video"
                , VideoPlayer.SCREEN_LAYOUT_NORMAL);
        jcVideoPlayerStandard.startVideo();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (jcVideoPlayerStandard != null) {
            jcVideoPlayerStandard.PauseMediaPlayer();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (VideoPlayerManager.getCurrentJcvd() != null) {
            VideoPlayerManager.getCurrentJcvd().setPlayImage();
            VideoPlayerManager.getCurrentJcvd().currentState = CURRENT_STATE_PAUSE;
        }
    }

    public boolean isFullScreen() {
        if (jcVideoPlayerStandard != null && VideoPlayerManager.getCurrentJcvd() != null && VideoPlayerManager.getCurrentJcvd().currentScreen == 2) {
            return true;
        } else {
            return false;
        }
    }

    public void setHaifScreen() {
        if (VideoPlayerManager.getCurrentJcvd() != null) {
            VideoPlayerManager.getCurrentJcvd().backPress();
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        mediaPlaying = true;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (VideoPlayerManager.getCurrentJcvd() != null) {
                    VideoPlayerManager.getCurrentJcvd().onPrepared();
                    VideoPlayerManager.getCurrentJcvd().hideProgress();
                }
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlaying = true;
        Runtime.getRuntime().gc();
        jcVideoPlayerStandard.cancelProgressTimer();
        jcVideoPlayerStandard.setUiWitStateAndScreen(CURRENT_STATE_AUTO_COMPLETE);
        if (isFullScreen()) {
            setHaifScreen();
        }
        jcVideoPlayerStandard.setPlayImage();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer,final int bufferProgress) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (VideoPlayerManager.getCurrentJcvd() != null) {
                    VideoPlayerManager.getCurrentJcvd().updateSeekProgress(bufferProgress);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mediaPlaying = true;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (VideoPlayerManager.getCurrentJcvd() != null) {
                    VideoPlayerManager.getCurrentJcvd().hideProgress();
                }
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, final int what, int i1) {
        mediaPlaying = false;
        if (what != 38 && what != -38) {
            showAlert();
        }
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (VideoPlayerManager.getCurrentJcvd() != null) {
                    if (what != 38 && what != -38) {
                        jcVideoPlayerStandard.hideProgress();
                        jcVideoPlayerStandard.setUiWitStateAndScreen(CURRENT_STATE_ERROR);
                        if (VideoPlayerManager.getCurrentJcvd().isCurrentJcvd()) {
                            releaseMediaPlayer();
                        }
                    }
                }
            }
        });
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        currentVideoWidth = width;
        currentVideoHeight = height;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (VideoPlayerManager.getCurrentJcvd() != null) {
                    VideoPlayerManager.getCurrentJcvd().onVideoSizeChanged();
                }
            }
        });
    }

    public Point getVideoSize() {
        if (currentVideoWidth != 0 && currentVideoHeight != 0) {
            return new Point(currentVideoWidth, currentVideoHeight);
        } else {
            return null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG, "onSurfaceTextureAvailable [" + this.hashCode() + "] ");
        if (savedSurfaceTexture == null) {
            savedSurfaceTexture = surfaceTexture;
            prepare();
            VideoPlayerManager.getCurrentJcvd().showProgress();
        } else {
            textureView.setSurfaceTexture(savedSurfaceTexture);
        }
    }

    public void prepare() {
        releaseMediaPlayer();
        try {
            mediaPlaying = false;
            currentVideoWidth = 0;
            currentVideoHeight = 0;
            mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Class<MediaPlayer> clazz = MediaPlayer.class;
            Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
            method.invoke(mediaPlayer, CURRENT_PLAYING_URL, MAP_HEADER_DATA);
            mediaPlayer.setLooping(CURRENT_PLING_LOOP);
            mediaPlayer.setOnPreparedListener(MyFragment2.this);
            mediaPlayer.setOnCompletionListener(MyFragment2.this);
            mediaPlayer.setOnBufferingUpdateListener(MyFragment2.this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnSeekCompleteListener(MyFragment2.this);
            mediaPlayer.setOnErrorListener(MyFragment2.this);
            mediaPlayer.setOnInfoListener(MyFragment2.this);
            mediaPlayer.setOnVideoSizeChangedListener(MyFragment2.this);
            mediaPlayer.prepareAsync();
            mediaPlayer.setSurface(new Surface(savedSurfaceTexture));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlaying = false;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return savedSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public void showAlert() {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();

        // Setting Dialog Title
        alertDialog.setTitle("Alert Dialog");

        // Setting Dialog Message
        alertDialog.setMessage("Welcome to Android Application");

        // Setting Icon to Dialog
        alertDialog.setIcon(R.drawable.jc_error_pressed);

        // Setting OK Button
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
}