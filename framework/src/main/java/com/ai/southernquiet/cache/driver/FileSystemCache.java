package com.ai.southernquiet.cache.driver;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.cache.Cache;
import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.util.SerializationUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于 {@link FileSystem} 的缓存驱动.
 */
public class FileSystemCache implements Cache {
    private FileSystem fileSystem;
    private String workingRoot = "CACHE"; //Cache在FileSystem中的路径
    private String nameSeparator = "__"; //文件名中不同部分的分隔

    public FileSystemCache(FrameworkAutoConfiguration.FileSystemCacheProperties properties, FileSystem fileSystem) {
        String workingRoot = properties.getWorkingRoot();
        if (StringUtils.hasText(workingRoot)) {
            this.workingRoot = workingRoot;
        }

        String sep = properties.getNameSeparator();
        if (StringUtils.hasText(sep)) {
            this.nameSeparator = sep;
        }

        this.fileSystem = fileSystem;
    }

    @Override
    public void put(String key, Object value, int ttl) {
        try {
            fileSystem.put(getFilePath(key, ttl), serialize(value));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(String key, Object value) {
        try {
            fileSystem.put(getFilePath(key, -1), serialize(value));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(String key) {
        try {
            Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, getKeyPrefix(key)).findFirst();

            if (opt.isPresent()) {
                PathMeta meta = opt.get();
                String filename = meta.getName();
                long now = System.currentTimeMillis();
                long creationTime = meta.getCreationTime().toEpochMilli();
                int ttl = getTTLFromFileName(filename);

                if (creationTime + ttl <= now) {
                    return null;
                }

                try (InputStream inputStream = fileSystem.openReadStream(meta.getPath())) {
                    return deserialize(inputStream);
                }
            }
        }
        catch (PathNotFoundException e) {
            return null;
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void touch(String key, Integer ttl) {
        try {
            Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, getKeyPrefix(key)).findFirst();

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
    public Map<String, Object> getAlive() {
        long now = System.currentTimeMillis();
        Stream<? extends PathMeta> stream = getMetaStream();

        return stream.filter(meta -> {
            String filename = meta.getName();
            long creationTime = meta.getCreationTime().toEpochMilli();
            int ttl = getTTLFromFileName(filename);

            return creationTime + ttl > now;
        }).collect(collector);
    }

    @Override
    public Map<String, Object> getExpired() {
        long now = System.currentTimeMillis();
        Stream<? extends PathMeta> stream = getMetaStream();

        return stream.filter(meta -> {
            String filename = meta.getName();
            long creationTime = meta.getCreationTime().toEpochMilli();
            int ttl = getTTLFromFileName(filename);

            return creationTime + ttl <= now;
        }).collect(collector);
    }

    @Override
    @SuppressWarnings("all")
    public void remove(String... keys) {
        Stream.of(keys).forEach(key -> {
            try {
                fileSystem.files(workingRoot, getKeyPrefix(key))
                    .findFirst()
                    .ifPresent(meta -> fileSystem.delete(meta.getPath()));
            }
            catch (PathNotFoundException e) {
                return;
            }
        });
    }

    @Override
    public Map<String, Object> find(String search) {
        try {
            return fileSystem.files(workingRoot, search).collect(collector);
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getFileName(String key, int ttl) {
        return key + nameSeparator + ttl;
    }

    private String getKeyPrefix(String key) {
        return key + nameSeparator;
    }

    private String getFilePath(String key, int ttl) {
        return workingRoot + FileSystem.PATH_SEPARATOR + getFileName(key, ttl);
    }

    private int getTTLFromFileName(String name) {
        return Integer.parseInt(name.substring(name.indexOf(nameSeparator) + nameSeparator.length()));
    }

    private String getKeyFromFileName(String name) {
        return name.substring(0, name.indexOf(nameSeparator));
    }

    private Stream<? extends PathMeta> getMetaStream() {
        try {
            return fileSystem.files(workingRoot);
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream serialize(Object data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(data));
    }

    private Object deserialize(InputStream stream) {
        try {
            return SerializationUtils.deserialize(StreamUtils.copyToByteArray(stream));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Collector<PathMeta, ?, Map<String, Object>> collector =
        Collectors.toMap(
            meta -> getKeyFromFileName(meta.getName()),
            meta -> {
                try (InputStream inputStream = fileSystem.openReadStream(meta.getPath())) {
                    return deserialize(inputStream);
                }
                catch (InvalidFileException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
}
