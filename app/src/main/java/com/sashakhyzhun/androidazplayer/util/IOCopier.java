package com.sashakhyzhun.androidazplayer.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOCopier {

    public static void joinFiles(File destination, InputStream data)
            throws IOException {
        OutputStream output = null;
        try {
            output = createAppendableStream(destination);

            appendFile(output, data);

        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private static BufferedOutputStream createAppendableStream(File destination)
            throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, true));
    }

    private static void appendFile(OutputStream output, InputStream source)
            throws IOException {
        InputStream input = null;
        try {
            input = new BufferedInputStream(source);
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

}
