package com.sashakhyzhun.androidazplayer.ui.main;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import com.sashakhyzhun.androidazplayer.R;
import com.sashakhyzhun.androidazplayer.data.model.Chunk;
import com.sashakhyzhun.androidazplayer.ui.custom.PlayPauseView;

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
import java.util.HashMap;
import java.util.Map;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();

    private boolean isFetchingSong = false;
    private boolean songIsFetched = false;
    private int nextChunkArrive = 0;
    private int finishedParts = 0;

    private String urlStream;
    private ArrayList<Chunk> mChunks;

    private MediaPlayer mp;
    private VideoView myVideoView;
    private PlayPauseView btnPlay;
    private File downloadingMediaFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        btnPlay = (PlayPauseView) view.findViewById(R.id.playBtn);
        mp = new MediaPlayer();


        btnPlay.setMediaPlayer(mp);
        btnPlay.setClickedListener(v -> {
            if (isFetchingSong && songIsFetched) {
                return;
            } else if (!songIsFetched) {
                isFetchingSong = true;
                btnPlay.startFetching();
                startTasks();
            } else if (btnPlay.getState() == PlayPauseView.BUTTON_STATE.STATE_PLAYING) {
                btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PAUSE);
//                    mp.seekTo(mp.getDuration() - 10000);//TODO for testing only, its painful waiting song to end
                mp.pause();
            } else if (btnPlay.getState() == PlayPauseView.BUTTON_STATE.STATE_PAUSE) {
                btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PLAYING);
                mp.start();
            }

        });

        MediaController mc = new MediaController(getContext());


        urlStream = "http://pubcache1.arkiva.de/test/hls_index.m3u8";

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    public void startTasks() {

        new FetchAudio().execute();
    }


    class FetchAudio extends AsyncTask<Object, Object, File> {

        @Override
        protected File doInBackground(Object... params) {
            int count;

            try {
                // todo: google this...
                String baseUrl = "http://pubcache1.arkiva.de/test/";
                String hlsUrl = baseUrl + "hls_index.m3u8";

                String[] ext = getHLSFile(hlsUrl).split("\n");

                String bestaudio = "";
                for (String s : ext) {
                    if (s.contains("EXTM3U")) {
                        continue;
                    }
                    if (s.contains("TYPE=AUDIO")) {
                        s = s.replace("#EXT-X-MEDIA:", "");
                        s = s.replace("\"", "");
                        Map<String, String> audio = splitToMap(s, ",", "=");
                        Log.d(TAG, s);
                        bestaudio = audio.get("URI");
                        Log.d(TAG, audio.get("URI"));
                    } else {
                        if (!bestaudio.equals("")) {
                            break;
                        }
                    }
                }

                hlsUrl = baseUrl + bestaudio;
                ext = getHLSFileForBestQuality(hlsUrl).split("\n");

                mChunks = getAllChunks(ext);
                nextChunkArrive = 0;


                for (int i = 0; i < mChunks.size(); i = i + 2) {
                    mChunks.get(i).setFilename("filename_" + i + ".mp3");
                    mChunks.get(i).setPos(i);
                    Chunk c2 = null;
                    if (mChunks.size() - 1 > i + 1) {
                        mChunks.get(i + 1).setFilename("filename_" + (i + 1) + ".mp3");
                        mChunks.get(i + 1).setPos(i + 1);
                        c2 = mChunks.get(i + 1);
                    }
                    downloadAudio(mChunks.get(i), c2);
                }

                return null;
//                playSong(cDir.getPath() + "/" + "piece_1.mp3");
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


    private ArrayList<Chunk> getAllChunks(String[] ext) {
        ArrayList<Chunk> m = new ArrayList<>();
        int count = 0;
        while (count < ext.length) {
            String s = ext[count];
            if (s.contains("EXT-X-ENDLIST")) {
                break;
            } else if (!s.contains("#EXTINF")) {
                count++;
                continue;
            } else {
                count++;
                Chunk cc = new Chunk();
                s = ext[count];
                String[] timer = s.split(":")[1].split("@");
                cc.setOffset(Integer.parseInt(timer[1]));
                cc.setLength(Integer.parseInt(timer[0]));
                count++;
                s = ext[count];
                cc.setName(s);
                m.add(cc);
                count++;
            }

        }

        return m;
    }

    private File downloadAudio(final Chunk chunkFirst, final Chunk chunkSecond) {

        int count;
        final String _url = "http://pubcache1.arkiva.de/test/" + chunkFirst.getName();


        Thread t1 = new Thread(new Runnable() {

            @Override
            public void run() {
                int count = 0;
                int total = 0;
                Log.i(TAG, "Started T" + chunkFirst.getPos() + ".:" + total);

                Log.i(TAG, "Starting T1.");
                try {
                    URL url = new URL(_url);
                    URLConnection con = url.openConnection();
                    con.setRequestProperty("Range", "bytes=" + chunkFirst.getOffset() + "-" + (chunkFirst.getLength() + chunkFirst.getOffset()));

                    HttpURLConnection connection = (HttpURLConnection) con;
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int lenghtOfFile = connection.getContentLength();
                    Log.d(TAG, connection.getHeaderField("Content-Range"));
                    Log.d(TAG, String.valueOf(lenghtOfFile));

                    final InputStream input = connection.getInputStream();

                    File cDir = getContext().getExternalFilesDir(null);
                    String filename = chunkFirst.getFilename();
                    final File downloadingMediaFile = new File(getContext().getCacheDir(), filename);
                    if (downloadingMediaFile.exists()) {
                        downloadingMediaFile.delete();
                    }


                    final OutputStream output = new FileOutputStream(downloadingMediaFile);

                    final byte data[] = new byte[1024];

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();

                    chunkFirst.setFile(downloadingMediaFile);
                    chunkFirst.setFullPath(downloadingMediaFile.getAbsolutePath());
                    threadsHaveFinish(chunkFirst);


                } catch (Exception e) {

                }

                Log.i(TAG, "Finished T" + chunkFirst.getPos() + ".:" + total);
            }
        });


        Thread t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                int total = 0;
                int count = 0;
                Log.i(TAG, "Started T" + chunkSecond.getPos() + ".:" + total);


                try {
                    URL url = new URL(_url);
                    URLConnection con = url.openConnection();
                    con.setRequestProperty("Range", "bytes=" + chunkSecond.getOffset() + "-" + (chunkSecond.getLength() + chunkSecond.getOffset()));

                    HttpURLConnection connection = (HttpURLConnection) con;
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int lenghtOfFile = connection.getContentLength();
                    Log.d(TAG, connection.getHeaderField("Content-Range"));
                    Log.d(TAG, String.valueOf(lenghtOfFile));

                    final InputStream input = connection.getInputStream();

                    File cDir = getContext().getExternalFilesDir(null);
                    String filename = chunkSecond.getFilename();
                    final File downloadingMediaFile = new File(getContext().getCacheDir(), filename);
                    if (downloadingMediaFile.exists()) {
                        downloadingMediaFile.delete();
                    }


                    final OutputStream output = new FileOutputStream(downloadingMediaFile);

                    final byte data[] = new byte[1024];

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();


                    chunkSecond.setFile(downloadingMediaFile);
                    chunkSecond.setFullPath(downloadingMediaFile.getAbsolutePath());

                    threadsHaveFinish(chunkSecond);

//                    threadsHaveFinish(1, downloadingMediaFile);

                } catch (Exception e) {
                }

                Log.i(TAG, "Finishing T" + chunkSecond.getPos() + ".:" + total);
            }
        });

        t1.start();
        if (chunkSecond != null) {
            t2.start();
        }


        return null;
    }

    private void threadsHaveFinish(Chunk chunk) {
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


    public static Map<String, String> splitToMap(String source, String separator, String mapSeparator) {
        Map<String, String> map = new HashMap<>();
        String[] entries = source.split(separator);
        for (String entry : entries) {
            if (!TextUtils.isEmpty(entry) && entry.contains(mapSeparator)) {
                String[] keyValue = entry.split(mapSeparator);
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }


    private void playSong(File mediaFile) {
//    private void playSong(String songPath) {
        Log.d(TAG, String.valueOf(mediaFile.getAbsolutePath()));
        try {


            FileInputStream fileInputStream = new FileInputStream(mediaFile);
            Log.d(TAG, String.valueOf(fileInputStream.available()));


            mp.reset();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mp.setDataSource(urlStream);
            mp.setDataSource(fileInputStream.getFD());
            fileInputStream.close();
//            mp.setDataSource(getApplicationContext(), Uri.parse(songPath));
//            mp.setDataSource("http://pubcache1.arkiva.de/test/hls_index.m3u8");

            // Setup listener so next song starts automatically
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer mp) {
                    btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_COMPLETED);
                    songIsFetched = false;
                    isFetchingSong = false;
                    if (downloadingMediaFile != null) {
                        downloadingMediaFile.delete();
                    }

//                    nextSong();
                }

            });
            mp.setOnPreparedListener(mp -> {
                btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PLAYING);
                mp.start();
            });

            mp.prepareAsync();

            myVideoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });


        } catch (IOException e) {
            e.printStackTrace();
            Log.v(getString(R.string.app_name), e.getMessage());
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        isFetchingSong = false;
        songIsFetched = true;

    }


    public String getHLSFile(String _url) throws Exception {
        int count;

        URL url = new URL(_url);
        URLConnection con = url.openConnection();

        HttpURLConnection connection = (HttpURLConnection) con;

//                connection.setChunkedStreamingMode(0);
//                connection.setAllowUserInteraction(false);
//                con.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.connect();


        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());

        }

        int lenghtOfFile = connection.getContentLength();


        InputStream input = new BufferedInputStream(url.openStream());
        long total = 0;

        String tt = "";
        byte _data[] = new byte[1024];

        while ((count = input.read(_data)) != -1) {
            tt += new String(_data);
        }

        return tt;

    }


    public String getHLSFileForBestQuality(String _url) throws Exception {
        int count;

        URL url = new URL(_url);
        URLConnection con = url.openConnection();

        HttpURLConnection connection = (HttpURLConnection) con;

//                connection.setChunkedStreamingMode(0);
//                connection.setAllowUserInteraction(false);
//                con.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.connect();


        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());

        }

        int lenghtOfFile = connection.getContentLength();


        InputStream input = new BufferedInputStream(url.openStream());
        long total = 0;

        StringBuilder tt = new StringBuilder();
        byte _data[] = new byte[1024];

        while ((count = input.read(_data)) != -1) {
            tt.append(new String(_data));
        }

        return tt.toString();

    }









}
