package com.sashakhyzhun.androidazplayer.ui.main;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sashakhyzhun.androidazplayer.R;
import com.sashakhyzhun.androidazplayer.data.model.Chunk;
import com.sashakhyzhun.androidazplayer.network.HlsRequests;
import com.sashakhyzhun.androidazplayer.network.RetrofitClient;
import com.sashakhyzhun.androidazplayer.ui.custom.AzButton;
import com.sashakhyzhun.androidazplayer.util.HlsHelper;
import com.sashakhyzhun.androidazplayer.util.TextHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

import static com.sashakhyzhun.androidazplayer.util.Constants.EXTM3U;
import static com.sashakhyzhun.androidazplayer.util.Constants.EXT_X_MEDIA;
import static com.sashakhyzhun.androidazplayer.util.Constants.FILENAME_PREF;
import static com.sashakhyzhun.androidazplayer.util.Constants.MP3;
import static com.sashakhyzhun.androidazplayer.util.Constants.TYPE_AUDIO;
import static com.sashakhyzhun.androidazplayer.util.Constants.URL_BASE;
import static com.sashakhyzhun.androidazplayer.util.Constants.URL_FILE;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();
    private boolean isFetching = false;
    private boolean isFetched = false;
    private int nextChunkArrive = 0;
    private ArrayList<Chunk> mChunks;

    private MediaPlayer mp;
    private AzButton buttonPlay;
    private File downloadingMediaFile;

    ExecutorService executorService;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mp = new MediaPlayer();

        executorService = Executors.newFixedThreadPool(2);
        Chunk a = new Chunk();
        a.setOffset(0);
        a.setLength(363780);
        a.setName("hls_a256K.ts");
        Chunk b = new Chunk();
        b.setOffset(363780);
        b.setLength(367164);
        b.setName("hls_a256K.ts");
        test(a, b);
    }

    @SuppressLint("CheckResult")
    private void test(Chunk chunkA, Chunk chunkB) {
        HlsRequests retrofitInterface = RetrofitClient.getRetrofit();

        Observable<ResponseBody> a = retrofitInterface
                .downloadChunk(chunkA.getName(), getRange(chunkA))
                .doOnSubscribe(d -> Log.d(TAG, "test: a"))
                .doOnTerminate(() -> Log.d(TAG, "test: a terminate"));

        Observable<ResponseBody> b = retrofitInterface
                .downloadChunk(chunkB.getName(), getRange(chunkB))
                .doOnSubscribe(d -> Log.d(TAG, "test: b"))
                .doOnTerminate(() -> Log.d(TAG, "test: b terminate"));
        Log.d(TAG, "test: ");

        List<ObservableSource<ResponseBody>> observableSources = new ArrayList<>();
        observableSources.add(a);
        observableSources.add(b);
        Observable.concatEager(observableSources)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> Log.d(TAG, "test: success: " + result),
                        error -> Log.d(TAG, "test: error", error),
                        () -> Log.d(TAG, "test: complete"));
    }

    @NonNull
    private String getRange(Chunk chunkA) {
        return "bytes=" + chunkA.getOffset() + "-" + (chunkA.getLength() + chunkA.getOffset());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        buttonPlay = (AzButton) view.findViewById(R.id.playBtn);
        buttonPlay.setMediaPlayer(mp);
        buttonPlay.setClickedListener(v -> {
            if (!isFetched && !isFetching) {
                isFetching = true;
                buttonPlay.startFetching();
                new FetchHLSFileAsync().execute();
                return;
            }
            switch (buttonPlay.getState()) {
                case PLAYING:
                    buttonPlay.setState(AzButton.PLAYER_STATE.PAUSE);
                    mp.pause();
                    break;
                case PAUSE:
                    buttonPlay.setState(AzButton.PLAYER_STATE.PLAYING);
                    mp.start();
                    break;
            }
        });

        return view;
    }


    @SuppressLint("StaticFieldLeak")
    private class FetchHLSFileAsync extends AsyncTask<Object, Object, File> {

        @Override
        protected File doInBackground(Object... params) {
            try {
                String fullUrl = URL_BASE + URL_FILE;
                String[] exts = HlsHelper.retrieveHLS(fullUrl).split("\n");
                String bestAudio = "-1";
                for (String ext : exts) {
                    if (ext.contains(EXTM3U)) {
                        continue;
                    }
                    if (ext.contains(TYPE_AUDIO)) {
                        ext = ext.replace(EXT_X_MEDIA, "");
                        ext = ext.replace("\"", "");
                        Map<String, String> audio = TextHelper.splitToMap(ext, ",", "=");
                        bestAudio = audio.get("URI");
                    } else {
                        if (!bestAudio.equals("-1")) {
                            break;
                        }
                    }
                }

                fullUrl = URL_BASE + bestAudio;
                exts = HlsHelper.retrieveHLS(fullUrl).split("\n");

                mChunks = HlsHelper.getAllChunks(exts);
                nextChunkArrive = 0;


                for (int i = 0; i < mChunks.size(); i = i + 2) {
                    mChunks.get(i).setFilename(FILENAME_PREF + i + MP3);
                    Chunk chunk = null;
                    if (mChunks.size() - 1 > i + 1) {
                        mChunks.get(i + 1).setFilename(FILENAME_PREF + (i + 1) + MP3);
                        chunk = mChunks.get(i + 1);
                    }
                    downloadAudio(mChunks.get(i), chunk);
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(File downloadingMediaFile) {
            super.onPostExecute(downloadingMediaFile);
            if (downloadingMediaFile == null) {
                return;
            }
        }
    }




    private void downloadAudio(final Chunk chunkFirst, @Nullable final Chunk chunkSecond) {
        final String fullUrl = URL_BASE + chunkFirst.getName();
        Log.d("ASD", "chunkFirst getOffset = " + chunkFirst.getOffset());
        Log.d("ASD", "chunkFirst getLength = " + chunkFirst.getLength());

        Log.d("ASD", "chunkSecond getOffset = " + chunkSecond.getOffset());
        Log.d("ASD", "chunkSecond getLength = " + chunkSecond.getLength());

        Log.d("ASD", "*********************************************");


        // here I need to download 1 chunk with 2 threads.
        // now it 's 1 chunk with 1 thread.
        Thread t1 = new Thread(() -> downloadTheFile(chunkFirst, fullUrl));
        Thread t2 = new Thread(() -> downloadTheFile(chunkSecond, fullUrl));

        t1.start();
        if (chunkSecond != null) {
            t2.start();
        }

    }

    private File downloadTheFile(final Chunk chunk, final String fullUrl) {
        int count = 0;
        int total = 0;
        File result = null;
        try {
            URL url = new URL(fullUrl);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Range", "bytes=" + chunk.getOffset() + "-" + (chunk.getLength() + chunk.getOffset()));

            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestMethod("GET");
            connection.connect();

            final InputStream input = connection.getInputStream();
            String filename = chunk.getFilename();
            result = new File(getContext().getCacheDir(), filename);
            if (result.exists()) {
                result.delete();
            }

            final OutputStream output = new FileOutputStream(result);
            final byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();

            chunk.setFile(result);
            chunk.setFullPath(result.getAbsolutePath());
            threadsHaveFinish();


        } catch (Exception ignored) {

        }
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void threadsHaveFinish() {
        if (mChunks == null) {
            return;
        }
        nextChunkArrive++;
        if (nextChunkArrive == mChunks.size()) {
            try {
                String filename = "full_song.mp3";
                downloadingMediaFile = new File(getContext().getCacheDir(), filename);
                if (downloadingMediaFile.exists()) {
                    downloadingMediaFile.delete();
                }

                OutputStream output = new FileOutputStream(downloadingMediaFile);
                for (Chunk c : mChunks) {
                    File file = new File(c.getFullPath());
                    int size = (int) file.length() - 1;
                    byte[] bytes = new byte[size];

                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();

                    output.write(bytes, 0, size);

                    file.delete();
                }
                output.flush();
                output.close();

                concatenateAndPlay(downloadingMediaFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void concatenateAndPlay(File mediaFile) {
        try {
            FileInputStream fileInputStream = new FileInputStream(mediaFile);
            mp.reset();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(fileInputStream.getFD());
            fileInputStream.close();
            mp.setOnCompletionListener(mp -> {
                buttonPlay.setState(AzButton.PLAYER_STATE.COMPLETED);
                isFetched = false;
                isFetching = false;
                if (downloadingMediaFile != null) {
                    downloadingMediaFile.delete();
                }
            });
            mp.setOnPreparedListener(mp -> {
                buttonPlay.setState(AzButton.PLAYER_STATE.PLAYING);
                mp.start();
            });
            mp.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        isFetching = false;
        isFetched = true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
    }

}
