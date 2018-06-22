package com.sashakhyzhun.androidazplayer.util;

import android.text.TextUtils;
import android.util.Log;

import com.sashakhyzhun.androidazplayer.data.model.Chunk;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.sashakhyzhun.androidazplayer.util.Constants.EXTINF;
import static com.sashakhyzhun.androidazplayer.util.Constants.EXT_X_ENDLIST;

public class HlsHelper {

    public static ArrayList<Chunk> getAllChunks(String[] ext) {
        ArrayList<Chunk> chunkList = new ArrayList<>();
        int count = 0;
        while (count < ext.length) {
            String temp = ext[count];
            if (temp.contains(EXT_X_ENDLIST)) {
                break;
            } else if (!temp.contains(EXTINF)) {
                count++;
                continue;
            } else {
                count++;
                Chunk chunk = new Chunk();
                temp = ext[count];
                String[] times = temp.split(":")[1].split("@");
                chunk.setOffset(Integer.parseInt(times[1]));
                chunk.setLength(Integer.parseInt(times[0]));
                count++;
                temp = ext[count];
                chunk.setName(temp);
                chunkList.add(chunk);
                count++;
            }

        }

        return chunkList;
    }

    public static String retrieveHLS(String link) throws Exception {
        URL url = new URL(link);
        URLConnection con = url.openConnection();

        HttpURLConnection connection = (HttpURLConnection) con;
        connection.setRequestMethod("GET");
        connection.connect();

        InputStream input = new BufferedInputStream(url.openStream());
        StringBuilder sb = new StringBuilder();
        byte data[] = new byte[1024];

        int count;
        while ((count = input.read(data)) != -1) {
            sb.append(new String(data));
        }

        return sb.toString();
    }

}
