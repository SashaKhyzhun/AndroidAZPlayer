package com.sashakhyzhun.androidazplayer.util;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class TextHelper {

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
}
