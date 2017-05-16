package com.ai.southernquiet.filesystem;

import java.time.Instant;

/**
 * 路径的元信息。
 */
public class PathMeta {
    private String path;
    private String name;
    private boolean isDirectory;
    private Instant creationTime;
    private Instant lastModifiedTime;
    private Instant lastAccessTime;
    private long size;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }

    public Instant getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Instant lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Instant getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Instant lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}