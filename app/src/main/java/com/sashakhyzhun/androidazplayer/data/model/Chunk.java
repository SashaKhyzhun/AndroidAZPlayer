package com.sashakhyzhun.androidazplayer.data.model;

import java.io.File;


public class Chunk {

    private int offset;
    private int length;

    private String name;
    private String filename;
    private String fullPath;

    private File file;


    /**
     * Setters
     */

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public void setFile(File file) {
        this.file = file;
    }


    /**
     * Getters
     */

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public String getFullPath() {
        return fullPath;
    }

    public File getFile() {
        return file;
    }
}
