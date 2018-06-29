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
    public static final String TAG = MainFragment.class.getSimpleName();
    private boolean isFetching = false;
    private boolean isFetched = false;
    private ArrayList<Chunk> mChunks;

    private MediaPlayer mp;
    private AzButton buttonPlay;

    ExecutorService executorService;
    public static final String FULL_SONG_FILE_NAME = "full_song.mp3";
    private LinkedList<Pair<Chunk, Chunk>> mChunkLinkedList;
    private Chunk mPairlessChunk;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mp = new MediaPlayer();
        executorService = Executors.newFixedThreadPool(2);
        deleteSongFileIfExists();
    }

    private void deleteSongFileIfExists() {
        File songFile = getFullSongFile();
        Log.d(TAG, "deleteSongFileIfExists: exists before trying to delete? " + songFile.exists());
        if (songFile.exists()) {
            boolean deleted = songFile.delete();
            Log.d(TAG, "deleteSongFileIfExists: hasDeleted: " + deleted);
            Log.d(TAG, "deleteSongFileIfExists: hasDeleted: " + getFullSongFile().exists());
        }
    }

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private void processChunks(List<Chunk> chunks) {
        isFetching = true;
        boolean even = chunks.size() % 2 == 0;
        if (!even) {
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
                    appendToFile(responseBody);
                    appendToFile(responseBody2);
                    Log.d(TAG, "apply: appended to file success");
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(file -> {
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
                        //TODO download last chunk mParilessChunk
                        checkIfNeedToDownloadPairlessChunk();
                    }
                    File finalFile = getFullSongFile();
                    if (buttonPlay.getState() == AzButton.PLAYER_STATE.FETCHING) {
                        play(finalFile);
                    }
                }, error -> {
                    Log.d(TAG, "parallelChunkDownload: error", error);
                });
        mCompositeDisposable.add(d);
    }

    /*private void parallelChunkDownload(Observable<ResponseBody> firstChunkObservable, Observable<ResponseBody> secondChunkObservable) {
        Disposable d = Observable
                .zip(firstChunkObservable, secondChunkObservable, (responseBody, responseBody2) -> {
                    appendToFile(responseBody);
                    appendToFile(responseBody2);
                    Log.d(TAG, "apply: appended to file success");
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(file -> {
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
                        //TODO download last chunk mParilessChunk
                        checkIfNeedToDownloadPairlessChunk();
                    }
                    File finalFile = getFullSongFile();
                    if (buttonPlay.getState() == AzButton.PLAYER_STATE.FETCHING) {
                        play(finalFile);
                    }
                    //play(finalFile);
                }, error -> {
                    Log.d(TAG, "parallelChunkDownload: error", error);
                });
        mCompositeDisposable.add(d);
    }*/

    private void checkIfNeedToDownloadPairlessChunk() {
        if (mPairlessChunk != null) {
            Disposable d = downloadChunkObservable(mPairlessChunk)
                    .subscribeOn(Schedulers.from(executorService))
                    .doOnNext(this::appendToFile)
                    .subscribe(result -> {
                        mPairlessChunk = null;
                    }, error -> {
                        Log.d(TAG, "parallelChunkDownload: error", error);
                    });
            mCompositeDisposable.add(d);
        }
    }
    @NonNull
    private File getFullSongFile() {
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        //File folder = getContext().getCacheDir();
        if (!folder.exists()) folder.mkdir();
        return new File(folder, FULL_SONG_FILE_NAME);
    }

    private Observable<ResponseBody> downloadChunkObservable(Chunk chunk) {
        HlsRequests retrofitInterface = RetrofitClient.getRetrofit();
        Log.d(TAG, "downloadChunkObservable: " + chunk);
        Log.d(TAG, "downloadChunkObservable: " + getRange(chunk));
        return retrofitInterface.downloadChunk(chunk.getName(), getRange(chunk))
                .doOnSubscribe(d -> {
                    Log.d(TAG, "downloadChunkObservable: doOnSubscribe() " + chunk.getOffset());
                })
                .doOnNext(responseBody -> {
                    Log.d(TAG, "downloadChunkObservable: " + responseBody.contentLength());
                });
    }

    /*private Observable<InputStream> downloadChunkObservable(Chunk chunk) {
        Observable<InputStream> o = Observable.create(e -> {
            InputStream inputStream = downloadTheFile(chunk, "http://pubcache1.arkiva.de/test/" + chunk.getName());
            if (!e.isDisposed()) e.onNext(inputStream);
        });
        return o
                .doOnSubscribe(d -> {
                    Log.d(TAG, "downloadChunkObservable: doOnSubscribe() " + chunk.getOffset());
                });
    }*/

    private InputStream downloadTheFile(final Chunk chunk, final String fullUrl) {
        try {
            URL url = new URL(fullUrl);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Range", "bytes=" + chunk.getOffset() + "-" + (chunk.getLength() + chunk.getOffset()));

            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestMethod("GET");
            connection.connect();

            return connection.getInputStream();
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private String getRange(Chunk chunk) {
        return "bytes=" + chunk.getOffset() + "-" + (chunk.getLength() + chunk.getOffset());
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

                mChunks = HlsHelper.getAllChunks(exts);
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

    private void appendToFile(ResponseBody responseBody) {
        File finalFile = getFullSongFile();
        Log.d(TAG, "appendToFile: " + finalFile.exists());
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            boolean exists = finalFile.exists();
            if (!finalFile.exists()) {
                finalFile.createNewFile();
            }
            outputStream = new FileOutputStream(finalFile, exists);
            inputStream = responseBody.byteStream();
            byte[] fileReader = new byte[4096];
            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                outputStream.write(fileReader, 0, read);
            }
            outputStream.flush();
        } catch (IOException e) {
            Log.d(TAG, "appendToFile: error", e);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*private void appendToFile(InputStream inputStream) {
        File finalFile = getFullSongFile();
        Log.d(TAG, "appendToFile: " + finalFile.exists());
        OutputStream outputStream = null;
        try {
            boolean exists = finalFile.exists();
            if (!finalFile.exists()) {
                finalFile.createNewFile();
            }
            outputStream = new FileOutputStream(finalFile, exists);
            byte[] fileReader = new byte[4096];
            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                outputStream.write(fileReader, 0, read);
            }
        } catch (IOException e) {
            Log.d(TAG, "appendToFile: error", e);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    private void play(File mediaFile) {
        Log.d(TAG, "play: " + mediaFile.length());
        try {
            FileInputStream fileInputStream = new FileInputStream(mediaFile);
            mp.reset();
            mp.pause();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(fileInputStream.getFD());
            fileInputStream.close();
            mp.setOnCompletionListener(mp -> {
                Log.d(TAG, "play: onCompleteionListener");
                buttonPlay.setState(AzButton.PLAYER_STATE.COMPLETED);
                isFetched = false;
                isFetching = false;
                //deleteSongFileIfExists();
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
        mCompositeDisposable.dispose();
    }

}
