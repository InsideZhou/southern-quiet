package com.ai.southernquiet.filesystem;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 路径的元信息。
 */
public class PathMeta {
    private String parent;
    private String name;
    private boolean isDirectory;
    private Instant creationTime;
    private Instant lastModifiedTime;
    private Instant lastAccessTime;
    private long size;

    public PathMeta() {}

    public PathMeta(PathMeta meta) {
        setParent(meta.getParent());
        setName(meta.getName());
        setDirectory(meta.isDirectory());
        setCreationTime(meta.getCreationTime());
        setLastModifiedTime(meta.getLastModifiedTime());
        setLastAccessTime(meta.getLastAccessTime());
        setSize(meta.getSize());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("parent", getParent());
        map.put("name", getName());
        map.put("isDirectory", isDirectory());
        map.put("creationTime", getCreationTime());
        map.put("lastModifiedTime", getLastModifiedTime());
        map.put("lastAccessTime", getLastAccessTime());
        map.put("size", getSize());
        return map;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = FileSystem.normalizePath(parent);
    }

    public String getPath() {
        if (StringUtils.hasText(parent)) {
            return parent + FileSystem.PATH_SEPARATOR + getName();
        }

        return getName();
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