package com.dynu.vijay.myapplication;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.dynu.vijay.myapplication.VideoPlayer.CURRENT_STATE_PAUSE;

public class MyFragment2 extends Fragment {

    VideoPlayer jcVideoPlayerStandard;

    public static MyFragment2 newInstance() {
        return new MyFragment2();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_fragment, container, false);
        jcVideoPlayerStandard = (VideoPlayer) view.findViewById(R.id.videoplayer);
        jcVideoPlayerStandard.setUp("https://view.vzaar.com/5822839/video"
                , VideoPlayer.SCREEN_LAYOUT_NORMAL, "嫂子闭眼睛");
        jcVideoPlayerStandard.startVideo();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        jcVideoPlayerStandard.PauseMediaPlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        VideoPlayerManager.getCurrentJcvd().setPlayImage();
        VideoPlayerManager.getCurrentJcvd().currentState=CURRENT_STATE_PAUSE;
    }

    public boolean isFullScreen() {
        if (jcVideoPlayerStandard != null && VideoPlayerManager.getCurrentJcvd().currentScreen == 2) {
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


}