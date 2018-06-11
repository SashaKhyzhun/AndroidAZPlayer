package com.sashakhyzhun.androidazplayer.ui.main;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sashakhyzhun.androidazplayer.R;
import com.sashakhyzhun.androidazplayer.data.model.Chunk;
import com.sashakhyzhun.androidazplayer.ui.custom.PlayPauseView;
import com.sashakhyzhun.androidazplayer.util.StaticUtil;

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
import java.util.Map;

import static com.sashakhyzhun.androidazplayer.util.Constants.EXTM3U;
import static com.sashakhyzhun.androidazplayer.util.Constants.EXT_X_MEDIA;
import static com.sashakhyzhun.androidazplayer.util.Constants.MP3;
import static com.sashakhyzhun.androidazplayer.util.Constants.TYPE_AUDIO;
import static com.sashakhyzhun.androidazplayer.util.Constants.URL_BASE;
import static com.sashakhyzhun.androidazplayer.util.Constants.URL_FILE;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();

    private boolean isFetchingSong = false;
    private boolean songIsFetched = false;
    private int nextChunkArrive = 0;
    private ArrayList<Chunk> mChunks;

    private MediaPlayer mp;
    private PlayPauseView buttonPlay;
    private File downloadingMediaFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mp = new MediaPlayer();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        buttonPlay = (PlayPauseView) view.findViewById(R.id.playBtn);
        buttonPlay.setMediaPlayer(mp);
        buttonPlay.setClickedListener(v -> {
             if (!songIsFetched) {
                isFetchingSong = true;
                buttonPlay.startFetching();
                new FetchFile().execute();
                return;
            }
            switch (buttonPlay.getState()) {
                case STATE_PLAYING:
                    buttonPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PAUSE);
                    mp.pause();
                    break;
                case STATE_PAUSE:
                    buttonPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PLAYING);
                    mp.start();
                    break;
            }
        });

        return view;
    }


    class FetchFile extends AsyncTask<Object, Object, File> {

        @Override
        protected File doInBackground(Object... params) {
            try {
                String fullUrl = URL_BASE + URL_FILE;
                String[] ext = getHLSFileForBestQuality(fullUrl).split("\n");
                String bestAudio = "";
                for (String s : ext) {
                    if (s.contains(EXTM3U)) {
                        continue;
                    }
                    if (s.contains(TYPE_AUDIO)) {
                        s = s.replace(EXT_X_MEDIA, "");
                        s = s.replace("\"", "");
                        Map<String, String> audio = StaticUtil.splitToMap(s, ",", "=");
                        Log.d(TAG, s);
                        bestAudio = audio.get("URI");
                        Log.d(TAG, audio.get("URI"));
                    } else {
                        if (!bestAudio.equals("")) {
                            break;
                        }
                    }
                }

                fullUrl = URL_BASE + bestAudio;
                ext = getHLSFileForBestQuality(fullUrl).split("\n");

                mChunks = StaticUtil.getAllChunks(ext);
                nextChunkArrive = 0;


                for (int i = 0; i < mChunks.size(); i = i + 2) {
                    mChunks.get(i).setFilename("filename_" + i + MP3);
                    mChunks.get(i).setPos(i);
                    Chunk c2 = null;
                    if (mChunks.size() - 1 > i + 1) {
                        mChunks.get(i + 1).setFilename("filename_" + (i + 1) + MP3);
                        mChunks.get(i + 1).setPos(i + 1);
                        c2 = mChunks.get(i + 1);
                    }
                    downloadAudio(mChunks.get(i), c2);
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
            if (downloadingMediaFile == null)
                return;
        }
    }




    private void downloadAudio(final Chunk chunkFirst, final Chunk chunkSecond) {
        final String fullUrl = URL_BASE + chunkFirst.getName();

        Thread t1 = new Thread(() -> handleThread(chunkFirst, fullUrl));
        Thread t2 = new Thread(() -> handleThread(chunkSecond, fullUrl));

        t1.start();
        if (chunkSecond != null) {
            t2.start();
        }


    }

    private File handleThread(final Chunk chunk, final String fullUrl) {
        int count = 0;
        int total = 0;
        Log.i(TAG, "Started T" + chunk.getPos() + ".:" + total);
        File result = null;
        Log.i(TAG, "Starting T1.");
        try {
            URL url = new URL(fullUrl);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Range", "bytes=" + chunk.getOffset() + "-" + (chunk.getLength() + chunk.getOffset()));
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestMethod("GET");
            connection.connect();

            int lengthOfFile = connection.getContentLength();

            Log.d(TAG, connection.getHeaderField("Content-Range"));
            Log.d(TAG, String.valueOf(lengthOfFile));

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
        Log.i(TAG, "Finished T" + chunk.getPos() + ".:" + total);
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
                    Log.d(TAG, c.getFullPath());
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

                Log.d(TAG, String.valueOf(downloadingMediaFile.length()));


                playSong(downloadingMediaFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void playSong(File mediaFile) {
        Log.d(TAG, String.valueOf(mediaFile.getAbsolutePath()));
        try {
            FileInputStream fileInputStream = new FileInputStream(mediaFile);
            Log.d(TAG, String.valueOf(fileInputStream.available()));

            mp.reset();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(fileInputStream.getFD());
            fileInputStream.close();
            mp.setOnCompletionListener(mp -> {
                buttonPlay.setState(PlayPauseView.BUTTON_STATE.STATE_COMPLETED);
                songIsFetched = false;
                isFetchingSong = false;
                if (downloadingMediaFile != null) {
                    downloadingMediaFile.delete();
                }
            });
            mp.setOnPreparedListener(mp -> {
                buttonPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PLAYING);
                mp.start();
            });
            mp.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
            Log.v(getString(R.string.app_name), e.getMessage());
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        isFetchingSong = false;
        songIsFetched = true;
    }

    public String getHLSFileForBestQuality(String _url) throws Exception {
        URL url = new URL(_url);
        URLConnection con = url.openConnection();

        HttpURLConnection connection = (HttpURLConnection) con;
        connection.setRequestMethod("GET");
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());
        }

        InputStream input = new BufferedInputStream(url.openStream());
        StringBuilder tt = new StringBuilder();
        byte _data[] = new byte[1024];

        int count;
        while ((count = input.read(_data)) != -1) {
            tt.append(new String(_data));
        }

        return tt.toString();
    }









}
