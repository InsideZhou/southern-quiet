package com.ai.southernquiet.cache.driver;

import com.ai.southernquiet.cache.Cache;
import com.ai.southernquiet.filesystem.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于 {@link FileSystem} 的缓存驱动.
 */
@Component
public class FileSystemCache implements Cache {
    public final static String DEFAULT_ROOT = "CACHE";
    public final static String NAME_SEPARATOR = "__";

    private FileSystem fileSystem;
    private String workingRoot;

    public FileSystemCache() {
        this(DEFAULT_ROOT);
    }

    /**
     * @param workingRoot 在哪个目录下工作。
     */
    public FileSystemCache(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Autowired
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    @Override
    public void put(String key, String value, int ttl) {
        try {
            fileSystem.put(getFilePath(key, ttl), value);
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(String key, String value) {
        try {
            fileSystem.put(getFilePath(key, -1), value);
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String key) {
        try {
            Optional<PathMeta> opt = fileSystem.files(getWorkingRoot(), getKeyPrefix(key)).stream().findFirst();
            if (opt.isPresent()) {
                PathMeta meta = opt.get();
                String filename = meta.getName();
                long now = System.currentTimeMillis();
                long creationTime = meta.getCreationTime().toEpochMilli();
                int ttl = getTTLFromFileName(filename);

                if (creationTime + ttl <= now) {
                    return null;
                }

                return fileSystem.readString(meta.getPath());
            }
        }
        catch (PathNotFoundException e) {
            return null;
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void touch(String key, Integer ttl) {
        try {
            Optional<PathMeta> opt = fileSystem.files(getWorkingRoot(), getKeyPrefix(key)).stream().findFirst();
            if (opt.isPresent()) {
                PathMeta meta = opt.get();

                fileSystem.touchCreation(meta.getPath());
                if (null != ttl) {
                    fileSystem.move(meta.getPath(), getFilePath(key, ttl));
                }
            }

            throw new InvalidFileException("找不到cache key：" + key);
        }
        catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> getAlive() {
        long now = System.currentTimeMillis();
        Stream<PathMeta> stream = getMetaStream();

        return stream.filter(meta -> {
            String filename = meta.getName();
            long creationTime = meta.getCreationTime().toEpochMilli();
            int ttl = getTTLFromFileName(filename);

            return creationTime + ttl > now;
        }).collect(collector);
    }

    @Override
    public Map<String, String> getExpired() {
        long now = System.currentTimeMillis();
        Stream<PathMeta> stream = getMetaStream();

        return stream.filter(meta -> {
            String filename = meta.getName();
            long creationTime = meta.getCreationTime().toEpochMilli();
            int ttl = getTTLFromFileName(filename);

            return creationTime + ttl <= now;
        }).collect(collector);
    }

    @Override
    public void remove(String... keys) {
        Stream.of(keys).forEach(key -> {
            Optional<PathMeta> opt = null;
            try {
                opt = fileSystem.files(getWorkingRoot(), getKeyPrefix(key)).stream().findFirst();
            }
            catch (PathNotFoundException e) {
                return;
            }

            if (opt.isPresent()) {
                fileSystem.delete(opt.get().getPath());
            }
        });
    }

    @Override
    public Map<String, String> find(String search) {
        try {
            return fileSystem.files(getWorkingRoot()).stream()
                .filter(meta -> meta.getName().contains(search))
                .collect(collector);
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String getKeyPrefix(String key) {
        return key + NAME_SEPARATOR;
    }

    private String getFilePath(String key, int ttl) {
        return getWorkingRoot() + FileSystem.PATH_SEPARATOR + key + NAME_SEPARATOR + ttl;
    }

    private int getTTLFromFileName(String name) {
        return Integer.parseInt(name.substring(name.indexOf(NAME_SEPARATOR) + NAME_SEPARATOR.length()));
    }

    private String getKeyFromFileName(String name) {
        return name.substring(0, name.indexOf(NAME_SEPARATOR));
    }

    private Stream<PathMeta> getMetaStream() {
        Stream<PathMeta> stream;

        try {
            stream = fileSystem.files(getWorkingRoot()).stream();
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }

        return stream;
    }

    private Collector<PathMeta, ?, Map<String, String>> collector =
        Collectors.toMap(
            meta -> getKeyFromFileName(meta.getName()),
            meta -> {
                try {
                    return fileSystem.readString(meta.getPath());
                }
                catch (InvalidFileException e) {
                    throw new RuntimeException(e);
                }
            }
        );
}
