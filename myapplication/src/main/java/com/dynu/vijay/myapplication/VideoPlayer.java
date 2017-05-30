package com.dynu.vijay.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;
import static com.dynu.vijay.myapplication.MyFragment2.CURRENT_PLAYING_URL;
import static com.dynu.vijay.myapplication.MyFragment2.CURRENT_PLING_LOOP;
import static com.dynu.vijay.myapplication.MyFragment2.MAP_HEADER_DATA;
import static com.dynu.vijay.myapplication.MyFragment2.mediaPlaying;


public class VideoPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PLAYING = 2;
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;
    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    public static final int FULLSCREEN_ID = 33797;
    public static final int SCREEN_LAYOUT_NORMAL = 0;
    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int TINY_ID = 33798;
    public static boolean SAVE_PROGRESS = true;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;
    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    protected static Timer UPDATE_PROGRESS_TIMER;
    public AccessMediaPlayer mAccessMediaPlayer;
    public ImageView startButton;
    public SeekBar progressBar;
    public ProgressBar progress;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup bottomContainer;
    public int currentState = -1;
    public int currentScreen = -1;
    public boolean loop = false;
    public Map<String, String> headData;
    public String url = "";
    public AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        if (mAccessMediaPlayer.getMediaPlayer() != null &&
                                mAccessMediaPlayer.getMediaPlayer().isPlaying()) {
                            mAccessMediaPlayer.getMediaPlayer().pause();
                            mediaPlaying = false;
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };
    public Object[] objects1 = null;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected Handler mHandler;
    protected boolean mTouchingProgressBar;
    protected ProgressTimerTask mProgressTimerTask;

    public VideoPlayer(Context context) {
        super(context);
        mAccessMediaPlayer = (AccessMediaPlayer) context;
        init(context);
    }


    public VideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAccessMediaPlayer = (AccessMediaPlayer) context;
        init(context);
    }

    public static void saveProgress(Context context, String url, int progress) {
        if (!VideoPlayer.SAVE_PROGRESS) return;
        SharedPreferences spn = context.getSharedPreferences("JCVD_PROGRESS",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spn.edit();
        editor.putInt(url, progress);
        editor.apply();
    }

    public static int getSavedProgress(Context context, String url) {
        if (!VideoPlayer.SAVE_PROGRESS) return 0;
        SharedPreferences spn;
        spn = context.getSharedPreferences("JCVD_PROGRESS",
                Context.MODE_PRIVATE);
        return spn.getInt(url, 0);
    }

    /**
     * if url == null, clear all progress
     *
     * @param context
     * @param url     if url!=null clear this url progress
     */
    public static void clearSavedProgress(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            SharedPreferences spn = context.getSharedPreferences("JCVD_PROGRESS",
                    Context.MODE_PRIVATE);
            spn.edit().clear().apply();
        } else {
            SharedPreferences spn = context.getSharedPreferences("JCVD_PROGRESS",
                    Context.MODE_PRIVATE);
            spn.edit().putInt(url, 0).apply();
        }
    }

    public void releaseAllVideos() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            Log.d(TAG, "releaseAllVideos");
            VideoPlayerManager.completeAll();
            mAccessMediaPlayer.getContextFragment().releaseMediaPlayer();
        }
    }

    public boolean backPress() {
        Log.i(TAG, "backPress");
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;
        if (VideoPlayerManager.getSecondFloor() != null) {
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            VideoPlayer jcVideoPlayer = VideoPlayerManager.getSecondFloor();
            Log.i("", "");
            if (MyFragment2.mediaPlaying) {
                VideoPlayerManager.getFirstFloor().startButton.setImageResource(R.drawable.jc_pause_normal);
            } else {
                VideoPlayerManager.getFirstFloor().startButton.setImageResource(R.drawable.jc_play_normal);
            }

            VideoPlayerManager.getFirstFloor().playOnThisJcvd();
            return true;
        } else if (VideoPlayerManager.getFirstFloor() != null &&
                (VideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN
                )) {
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            VideoPlayerManager.getCurrentJcvd().currentState = CURRENT_STATE_NORMAL;
            VideoPlayerManager.getFirstFloor().clearFloatScreen();
            mAccessMediaPlayer.getContextFragment().releaseMediaPlayer();
            VideoPlayerManager.setFirstFloor(null);
            return true;
        }
        return false;
    }

    public void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            getAppCompActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            getAppCompActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void init(Context context) {
        View.inflate(context, R.layout.jc_layout_standard, this);
        startButton = (ImageView) findViewById(R.id.start);
        fullscreenButton = (ImageView) findViewById(R.id.fullscreen);
        progress = (ProgressBar) findViewById(R.id.progress);
        progressBar = (SeekBar) findViewById(R.id.bottom_seek_progress);
        currentTimeTextView = (TextView) findViewById(R.id.current);
        totalTimeTextView = (TextView) findViewById(R.id.total);
        bottomContainer = (ViewGroup) findViewById(R.id.layout_bottom);
        textureViewContainer = (ViewGroup) findViewById(R.id.surface_container);

        startButton.setOnClickListener(this);

        if (mediaPlaying) {
            startButton.setImageResource(R.drawable.jc_pause_normal);
        } else {
            startButton.setImageResource(R.drawable.jc_play_normal);
        }

        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(getContext(), getResources().getString(R.string.app_name), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {
                prepareMediaPlayer();
                setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
            } else if (currentState == CURRENT_STATE_PLAYING) {
                mAccessMediaPlayer.getMediaPlayer().pause();
                setUiWitStateAndScreen(CURRENT_STATE_PAUSE);
                startButton.setImageResource(R.drawable.jc_play_normal);
                mediaPlaying = false;
            } else if (currentState == CURRENT_STATE_PAUSE) {
                mAccessMediaPlayer.getMediaPlayer().start();
                mediaPlaying = true;
                setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
                startButton.setImageResource(R.drawable.jc_pause_normal);
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                prepareMediaPlayer();
            }
        } else if (i == R.id.fullscreen) {
            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                backPress();
            } else {
                startWindowFullscreen();
            }
        } else if (i == R.id.surface_container && currentState == CURRENT_STATE_ERROR) {
            prepareMediaPlayer();
        } else if (i == R.id.surface_container) {
            bottomContainer.setVisibility(bottomContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }

    public void PauseMediaPlayer() {
        if (mAccessMediaPlayer.getContextFragment() != null && mAccessMediaPlayer.getMediaPlayer() != null) {
            mAccessMediaPlayer.getMediaPlayer().pause();
            setUiWitStateAndScreen(CURRENT_STATE_PAUSE);
            mediaPlaying = false;
            if (startButton != null) {
                startButton.setImageResource(R.drawable.jc_play_normal);
            }
        }

    }

    public void setPlayImage() {
        if (startButton != null)
            startButton.setImageResource(R.drawable.jc_play_normal);
    }

    public void clearFloatScreen() {
        getAppCompActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        showSupportActionBar(getContext());
        VideoPlayer currJcvd = VideoPlayerManager.getCurrentJcvd();
        currJcvd.textureViewContainer.removeView(MyFragment2.textureView);
        ViewGroup vp = (ViewGroup) (scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        vp.removeView(currJcvd);
        VideoPlayerManager.setSecondFloor(null);
    }

    public void playOnThisJcvd() {
        Log.i(TAG, "playOnThisJcvd " + " [" + this.hashCode() + "] ");
        currentState = VideoPlayerManager.getSecondFloor().currentState;
        clearFloatScreen();
        setUiWitStateAndScreen(currentState);
        addTextureView();
    }

    public void onPrepared() {
        currentState = CURRENT_STATE_PLAYING;
        startButton.setImageResource(R.drawable.jc_pause_normal);
        startProgressTimer();
    }

    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
        progress.bringToFront();
        startButton.setEnabled(false);
        fullscreenButton.setEnabled(false);
    }

    public void hideProgress() {
        progress.setVisibility(View.GONE);
        startButton.setEnabled(true);
        fullscreenButton.setEnabled(true);
    }

    public void startWindowFullscreen() {
        Log.i(TAG, "startWindowFullscreen " + " [" + this.hashCode() + "] ");
        hideSupportActionBar(getContext());
        getAppCompActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        ViewGroup vp = (ViewGroup) (scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
        ((ViewGroup) MyFragment2.textureView.getParent()).removeView(MyFragment2.textureView);
        textureViewContainer.removeView(MyFragment2.textureView);
        try {
            Constructor<VideoPlayer> constructor = (Constructor<VideoPlayer>) VideoPlayer.this.getClass().getConstructor(Context.class);
            VideoPlayer jcVideoPlayer = constructor.newInstance(getContext());
            jcVideoPlayer.setId(FULLSCREEN_ID);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jcVideoPlayer, lp);
            jcVideoPlayer.setUp(url, SCREEN_WINDOW_FULLSCREEN);
            jcVideoPlayer.setUiWitStateAndScreen(currentState);
            jcVideoPlayer.addTextureView();
            VideoPlayerManager.setSecondFloor(jcVideoPlayer);
            final Animation ra = AnimationUtils.loadAnimation(getContext(), R.anim.start_fullscreen);
            jcVideoPlayer.setAnimation(ra);
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initTextureView() {
        removeTextureView();
        MyFragment2.textureView = new ResizeTextureView(getContext());
        MyFragment2.textureView.setSurfaceTextureListener(mAccessMediaPlayer.getContextFragment());
    }

    public void addTextureView() {
        Log.d(TAG, "addTextureView [" + this.hashCode() + "] ");
        LayoutParams layoutParams =
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        textureViewContainer.addView(MyFragment2.textureView, layoutParams);
    }

    public void removeTextureView() {
        MyFragment2.savedSurfaceTexture = null;
        if (MyFragment2.textureView != null && MyFragment2.textureView.getParent() != null) {
            ((ViewGroup) MyFragment2.textureView.getParent()).removeView(MyFragment2.textureView);
        }
    }

    public void prepareMediaPlayer() {
        VideoPlayerManager.completeAll();
        Log.d(TAG, "prepareMediaPlayer [" + this.hashCode() + "] ");
        initTextureView();
        addTextureView();
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        CURRENT_PLAYING_URL = url;
        CURRENT_PLING_LOOP = loop;
        MAP_HEADER_DATA = headData;
        setUiWitStateAndScreen(CURRENT_STATE_PREPARING);
        VideoPlayerManager.setFirstFloor(this);

    }

    public void setUp(String url, int screen) {
        if (!TextUtils.isEmpty(this.url) && TextUtils.equals(this.url, url)) {
            return;
        }
        this.url = url;
        this.currentScreen = screen;
        this.headData = null;
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
    }

    public void startProgressTimer() {
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public boolean isCurrentJcvd() {
        return VideoPlayerManager.getCurrentJcvd() != null
                && VideoPlayerManager.getCurrentJcvd() == this;
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(stringForTime(0));
        totalTimeTextView.setText(stringForTime(0));
    }

    public void setUiWitStateAndScreen(int state) {
        currentState = state;
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                cancelProgressTimer();
                if (isCurrentJcvd()) {
                    mAccessMediaPlayer.getContextFragment().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_PREPARING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
                startProgressTimer();
                break;
            case CURRENT_STATE_PAUSE:
                startProgressTimer();
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                startProgressTimer();
                break;
            case CURRENT_STATE_ERROR:
                cancelProgressTimer();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                cancelProgressTimer();
                progressBar.setProgress(100);
                currentTimeTextView.setText(totalTimeTextView.getText());
                break;
        }
    }

    public int getDuration() {
        int duration = 0;
        if (mAccessMediaPlayer.getMediaPlayer() == null) return duration;
        try {
            duration = mAccessMediaPlayer.getMediaPlayer().getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    public void setProgressAndText() {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        if (!mTouchingProgressBar) {
            if (progress != 0) progressBar.setProgress(progress);
        }
        if (position != 0) currentTimeTextView.setText(stringForTime(position));
        totalTimeTextView.setText(stringForTime(duration));
    }

    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        Log.i("", "" + mAccessMediaPlayer);
        if (mAccessMediaPlayer.getMediaPlayer() == null)
            return position;
        if (currentState == CURRENT_STATE_PLAYING ||
                currentState == CURRENT_STATE_PAUSE ||
                currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            try {
                position = mAccessMediaPlayer.getMediaPlayer().getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public void startVideo() {
        prepareMediaPlayer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        showProgress();
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        //onEvent(JCUserAction.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING &&
                currentState != CURRENT_STATE_PAUSE) return;
        int time = seekBar.getProgress() * getDuration() / 100;
        mAccessMediaPlayer.getMediaPlayer().seekTo(time);
        mediaPlaying = false;
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    public void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(FULLSCREEN_ID);
        View oldT = vp.findViewById(TINY_ID);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext());
    }

    public void onCompletion() {
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            int position = getCurrentPositionWhenPlaying();
            saveProgress(getContext(), url, position);
        }
        cancelProgressTimer();
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
        textureViewContainer.removeView(MyFragment2.textureView);
        mAccessMediaPlayer.getContextFragment().currentVideoWidth = 0;
        mAccessMediaPlayer.getContextFragment().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearFullscreenLayout();
        getAppCompActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        MyFragment2.textureView = null;
        MyFragment2.savedSurfaceTexture = null;
        MyFragment2.mediaPlaying = false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    public void onVideoSizeChanged() {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (MyFragment2.textureView != null) {
            MyFragment2.textureView.setVideoSize(mAccessMediaPlayer.getContextFragment().getVideoSize());
        }
    }

    public void updateSeekProgress(int progress) {
        if (progressBar != null) {
            progressBar.setSecondaryProgress(progress);
        }
    }

    public String stringForTime(int timeMs) {
        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public Activity scanForActivity(Context context) {
        if (context == null) return null;

        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }

        return null;
    }

    public AppCompatActivity getAppCompActivity(Context context) {
        if (context == null) return null;
        if (context instanceof AppCompatActivity) {
            return (AppCompatActivity) context;
        } else if (context instanceof ContextThemeWrapper) {
            return getAppCompActivity(((ContextThemeWrapper) context).getBaseContext());
        }
        return null;
    }

    public interface AccessMediaPlayer {
        public MediaPlayer getMediaPlayer();
        public MyFragment2 getContextFragment();

    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setProgressAndText();
                    }
                });
            }
        }
    }

}
