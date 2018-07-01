package com.sashakhyzhun.androidazplayer.ui.main;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
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
import com.snatik.storage.Storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

import static com.sashakhyzhun.androidazplayer.util.Constants.EXTM3U;
import static com.sashakhyzhun.androidazplayer.util.Constants.EXT_X_MEDIA;
import static com.sashakhyzhun.androidazplayer.util.Constants.TYPE_AUDIO;
import static com.sashakhyzhun.androidazplayer.util.Constants.URL_BASE;
import static com.sashakhyzhun.androidazplayer.util.Constants.URL_FILE;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();
    private static final String FULL_SONG_FILE_NAME = "fullSong.mp3";
    private static final String FIRST_CHUNK = "chunkFirst.mp3";
    private static final String SECOND_CHUNK = "chunkSecond.mp3";
    private static final String LAST_CHUNK = "chunkLast.mp3";

    private String storagePath;

    private boolean isFetching = false;
    private boolean isFetched = false;

    private LinkedList<Pair<Chunk, Chunk>> mChunkLinkedList;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private ExecutorService executorService;
    private AzButton buttonPlay;
    private MediaPlayer mp;

    private Chunk mPairlessChunk;
    private Storage storage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mp = new MediaPlayer();
        executorService = Executors.newFixedThreadPool(2);
        deleteSongFileIfExists();
        storage = new Storage(getContext());
        storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
        storage.createDirectory(storagePath);
        storagePath += "/";
    }

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


    private void deleteSongFileIfExists() {
        File songFile = getFullSongFile();
        if (songFile.exists()) {
           songFile.delete();
        }
    }

    private void processChunks(List<Chunk> chunks) {
        isFetching = true;
        boolean isEven = chunks.size() % 2 == 0;
        if (!isEven) {
            mPairlessChunk = chunks.get(chunks.size() - 1);
            chunks = chunks.subList(0, chunks.size() - 1);
        }
        mChunkLinkedList = new LinkedList<>();
        for (int i = 0; i < chunks.size(); i+= 2) {
            Pair<Chunk, Chunk> chunkPair = new Pair<>(chunks.get(i), chunks.get(i + 1));
            mChunkLinkedList.add(chunkPair);
        }
        //mChunkLinkedList.add(new Pair<>(chunks.get(0), chunks.get(1)));
        //mChunkLinkedList.add(new Pair<>(chunks.get(10), chunks.get(11)));
        Pair<Chunk, Chunk> chunkPair = mChunkLinkedList.getFirst();
        Observable<ResponseBody> firstChunkObservable = downloadChunkObservable(chunkPair.first)
                .subscribeOn(Schedulers.from(executorService));
        Observable<ResponseBody> secondChunkObservable = downloadChunkObservable(chunkPair.second)
                .subscribeOn(Schedulers.from(executorService));

        parallelChunkDownload(firstChunkObservable, secondChunkObservable);
    }

    private void parallelChunkDownload(Observable<ResponseBody> firstChunkObservable, Observable<ResponseBody> secondChunkObservable) {

        Disposable d = Observable
                .zip(firstChunkObservable, secondChunkObservable, (responseBody, responseBody2) -> {
                    saveAndAppendChunk(responseBody, FIRST_CHUNK);
                    saveAndAppendChunk(responseBody2, SECOND_CHUNK);

                    Log.d(TAG, "apply: appended to file success");

                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        file -> {
                            Log.d(TAG, "parallelChunkDownload: onNext");
                            mChunkLinkedList.removeFirst();
                            if (!mChunkLinkedList.isEmpty()) {
                                Pair<Chunk, Chunk> chunkPair = mChunkLinkedList.getFirst();
                                Observable<ResponseBody> a = downloadChunkObservable(chunkPair.first)
                                        .subscribeOn(Schedulers.from(executorService));
                                Observable<ResponseBody> b = downloadChunkObservable(chunkPair.second)
                                        .subscribeOn(Schedulers.from(executorService));
                                parallelChunkDownload(a, b);
                            } else {
                                checkIfNeedToDownloadPairLessChunk();
                            }

                            if (buttonPlay.getState() == AzButton.PLAYER_STATE.FETCHING) {
                                //File finalFile = getFullSongFile();
                                play(storage.getFile(storagePath + FULL_SONG_FILE_NAME));
                            }
                        },
                        error -> Log.d(TAG, "parallelChunkDownload: onError", error),
                        () -> Log.d(TAG, "parallelChunkDownload: onComplete")
                );
        mCompositeDisposable.add(d);
    }

    private void saveAndAppendChunk(ResponseBody responseBody, String chunkName) {
        try {
            storage.createFile(storagePath + chunkName, responseBody.bytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkFullSongExist();
        try {
            mergeMp3Files(storage.getFile(storagePath + chunkName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void mergeMp3Files(File chunk) throws IOException {
        FileInputStream chunkStream = new FileInputStream(chunk); // first source file

        FileInputStream fullSongStream = new FileInputStream(
                storage.getFile(storagePath + FULL_SONG_FILE_NAME)
        ); //second source file

        SequenceInputStream SIStream = new SequenceInputStream(fullSongStream, chunkStream);

        FileOutputStream FOStream = new FileOutputStream(
                storage.getFile(storagePath + FULL_SONG_FILE_NAME)
        ); //destination file

        int temp;
        while ((temp = SIStream.read()) != -1) {
            FOStream.write(temp); // to write to file
        }

        FOStream.close();
        SIStream.close();
        chunkStream.close();
        fullSongStream.close();
    }

    private void checkIfNeedToDownloadPairLessChunk() {
        if (mPairlessChunk != null) {
            Disposable d = downloadChunkObservable(mPairlessChunk)
                    .subscribeOn(Schedulers.from(executorService))
                    .doOnNext(responseBody -> {
                        saveAndAppendChunk(responseBody, LAST_CHUNK);
                    })
                    .subscribe(result -> {
                        mPairlessChunk = null;
                    }, error -> {
                        Log.d(TAG, "parallelChunkDownload: error", error);
                    });
            mCompositeDisposable.add(d);
        }

    }

    private void checkFullSongExist() {
        boolean exist = storage.getFile(storagePath + FULL_SONG_FILE_NAME).exists();
        if (!exist) storage.createFile(storagePath + FULL_SONG_FILE_NAME, "");
    }

    private void play(File mediaFile) {
        Log.d(TAG, "play: " + mediaFile.length());

        try {
            FileInputStream fileInputStream = new FileInputStream(mediaFile);
            //fileInputStream.getFD().sync();
            mp.reset();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(fileInputStream.getFD());
            fileInputStream.close();
            mp.setOnCompletionListener(mp -> {
                Log.d(TAG, "play: OnCompletionListener | fileSize = " + mediaFile.length());
                buttonPlay.setState(AzButton.PLAYER_STATE.COMPLETED);
                isFetched = false;
                isFetching = false;

                //todo: uncomment in the end:
                //deleteSongFileIfExists();
            });
            mp.setOnPreparedListener(mp -> {
                buttonPlay.setState(AzButton.PLAYER_STATE.PLAYING);
                mp.start();
            });
            mp.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "onError: mp=" + mp + ", what=" + what + ", extras=" + extra);
                return false;
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

    public byte[] convertFleToBytes(File file) throws IOException {

        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        }finally {
            try {
                if (ous != null) ous.close();
            } catch (IOException e) {}

            try {
                if (ios != null) ios.close();
            } catch (IOException e) { }
        }
        return ous.toByteArray();
    }

    @NonNull
    private File getFullSongFile() {
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        //File folder = getContext().getCacheDir();
        if (!folder.exists()) folder.mkdir();
        return new File(folder, FULL_SONG_FILE_NAME);
    }

    @NonNull
    private Observable<ResponseBody> downloadChunkObservable(Chunk chunk) {
        HlsRequests retrofitInterface = RetrofitClient.getRetrofit();
        Log.d(TAG, "downloadChunkObservable: " + getRange(chunk));
        return retrofitInterface.downloadChunk(chunk.getName(), getRange(chunk))
                .doOnSubscribe(d -> {
                    Log.d(TAG, "downloadChunkObservable: doOnSubscribe() " + chunk.getOffset());
                })
                .doOnNext(responseBody -> {
                    Log.d(TAG, "downloadChunkObservable: " + responseBody.contentLength());
                });
    }

    @NonNull
    private String getRange(Chunk chunk) {
        return "bytes=" + chunk.getOffset() + "-" + (chunk.getLength() + chunk.getOffset());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
        mCompositeDisposable.dispose();
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchHLSFileAsync extends AsyncTask<Object, Object, File> {

        @Override
        protected File doInBackground(Object... params) {
            try {
                String fullUrl = URL_BASE + URL_FILE;
                String[] exts = HlsHelper.retrieveHLS(fullUrl).split("\n");
                Log.d(TAG, "doInBackground: " + Arrays.toString(exts));
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

                ArrayList<Chunk> mChunks = HlsHelper.getAllChunks(exts);
                processChunks(mChunks);

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

}
