package me.insidezhou.southernquiet.filesystem;

import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static me.insidezhou.southernquiet.filesystem.FileSystem.PATH_SEPARATOR_STRING;

/**
 * 路径的元信息。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class PathMeta implements Serializable {
    private final static long serialVersionUID = 3373353696843442643L;

    /**
     * @param stream 输入流，当为null时，假设路径指向目录，isDirectory=true，size=-1。
     */
    public PathMeta(NormalizedPath normalizedPath, InputStream stream) {
        Assert.notNull(normalizedPath, "normalizedPath");
        parent = normalizedPath.getParent();
        name = normalizedPath.getName();

        if (null == stream) {
            isDirectory = true;
            size = -1;
        }
        else {
            isDirectory = false;

            try {
                size = stream.available();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public PathMeta(String path, InputStream stream) {
        this(new NormalizedPath(path), stream);
    }

    public PathMeta(NormalizedPath normalizedPath) {
        this(normalizedPath, null);
    }

    public PathMeta(String path) {
        this(new NormalizedPath(path), null);
    }

    public PathMeta() {}

    /**
     * 父路径名。
     */
    private String parent;
    /**
     * 当前路径元素名（文件或目录名）。
     */
    private String name;
    private boolean isDirectory;
    /**
     * 以当前时间为默认值。
     */
    private Instant creationTime;
    /**
     * 以当前时间为默认值。
     */
    private Instant lastModifiedTime;
    /**
     * 以当前时间为默认值。
     */
    private Instant lastAccessTime;
    /**
     * 如果路径指向文件的话，表示文件大小，单位：byte。
     */
    private long size;

    /**
     * 路径名
     */
    public String getPath() {
        return String.join(PATH_SEPARATOR_STRING, parent, name).replace(PATH_SEPARATOR_STRING + PATH_SEPARATOR_STRING, PATH_SEPARATOR_STRING);
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
        this.parent = parent;
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