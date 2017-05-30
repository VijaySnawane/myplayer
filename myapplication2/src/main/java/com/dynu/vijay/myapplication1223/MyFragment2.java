package com.dynu.vijay.myapplication;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dynu.vijay.myapplication.video.JCMediaManager;
import com.dynu.vijay.myapplication.video.JCVideoPlayerManager;
import com.dynu.vijay.myapplication.video.JCVideoPlayerStandard;

import static com.dynu.vijay.myapplication.video.JCMediaManager.isFullScreen;
import static com.dynu.vijay.myapplication.video.JCMediaManager.mPlaying;


public class MyFragment2 extends Fragment {
    JCVideoPlayerStandard jcVideoPlayerStandard;

    Button change;

    public static MyFragment2 newInstance() {
        return new MyFragment2();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_fragment, container, false);
        jcVideoPlayerStandard = (JCVideoPlayerStandard) view.findViewById(R.id.videoplayer);
        change=(Button)view.findViewById(R.id.change);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jcVideoPlayerStandard.changeUiToPauseShow();
            }
        });
        jcVideoPlayerStandard.setUp("https://view.vzaar.com/5822839/video"
                , JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL, "Back");
        jcVideoPlayerStandard.startVideo();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlaying && isFullScreen == 2) {
            JCMediaManager.instance().mediaPlayer.pause();
            jcVideoPlayerStandard.currentState=5;
            jcVideoPlayerStandard.changeUiToPauseShow();
            Log.i("", "");
        } else if (mPlaying && isFullScreen == 0) {
            Log.i("", "");
        }
    }
}