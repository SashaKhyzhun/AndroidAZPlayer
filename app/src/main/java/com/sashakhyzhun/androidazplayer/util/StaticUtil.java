package com.sashakhyzhun.androidazplayer.util;

import android.text.TextUtils;

import com.sashakhyzhun.androidazplayer.data.model.Chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.sashakhyzhun.androidazplayer.util.Constants.EXTINF;
import static com.sashakhyzhun.androidazplayer.util.Constants.EXT_X_ENDLIST;

public class StaticUtil {

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

    public static ArrayList<Chunk> getAllChunks(String[] ext) {
        ArrayList<Chunk> m = new ArrayList<>();
        int count = 0;
        while (count < ext.length) {
            String s = ext[count];
            if (s.contains(EXT_X_ENDLIST)) {
                break;
            } else if (!s.contains(EXTINF)) {
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

}
