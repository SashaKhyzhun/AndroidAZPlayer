package com.sashakhyzhun.androidazplayer.ui.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.sashakhyzhun.androidazplayer.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadCurrentFragment();
    }

    /**
     * Method which shows selected fragment from drawer and loads it in UI Thread, because we need to
     * avoid large object and freezes. In this method I also was set current title for toolbar.
     */
    private void loadCurrentFragment() {
        Fragment fragment = new MainFragment();
        FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
        fm.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        fm.replace(R.id.frame_main, fragment, null);
        fm.commitAllowingStateLoss();
    }


}
