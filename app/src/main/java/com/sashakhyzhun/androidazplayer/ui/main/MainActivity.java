package com.sashakhyzhun.androidazplayer.ui.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.sashakhyzhun.androidazplayer.R;
import com.sashakhyzhun.androidazplayer.data.model.Chunk;
import com.sashakhyzhun.androidazplayer.network.HlsRequests;
import com.sashakhyzhun.androidazplayer.network.RetrofitClient;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadCurrentFragment();

        testZip();
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

    private void testZip() {

//        getOffset = 0
//        getLength = 363780
//        getOffset = 730944
//        getLength = 365096

        Chunk chunkA = new Chunk();
        chunkA.setOffset(0);
        chunkA.setLength(363780);
        Chunk chunkB = new Chunk();
        chunkB.setOffset(363780);
        chunkB.setLength(367164);

        String url = "http://pubcache1.arkiva.de/test/hls_a256K.ts/";
        HlsRequests client = RetrofitClient.getRetrofit(url).create(HlsRequests.class);

        Observable<ResponseBody> respA = client
                .downloadChunk("bytes=" + chunkA.getOffset() + "-" + (chunkA.getLength() + chunkA.getOffset()))
                .doOnTerminate(() -> Log.i("test", "respA called"));

        Observable<ResponseBody> respB = client
                .downloadChunk("bytes=" + chunkB.getOffset() + "-" + (chunkB.getLength() + chunkB.getOffset()))
                .doOnTerminate(() -> Log.i("test", "respB called"));

        Disposable result = Observable
                .zip(respA, respB, new BiFunction<ResponseBody, ResponseBody, File>() {
                    @Override
                    public File apply(ResponseBody responseBody, ResponseBody responseBody2) throws Exception {
                        Log.i("test", "apply");
                        return new File("");
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }



}
