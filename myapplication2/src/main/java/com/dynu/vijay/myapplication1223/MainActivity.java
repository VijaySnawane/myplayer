package com.dynu.vijay.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "";
    MyFragment2 mMyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            if (mMyFragment == null)
                mMyFragment = MyFragment2.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, mMyFragment, "myfargmant")
                    .commit();
        }
    }
}
