package com.ai.southernquiet.keyvalue.driver;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.keyvalue.KeyValueStore;
import com.ai.southernquiet.util.SerializationUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 基于 {@link FileSystem} 的键值对驱动.
 */
public class FileSystemKeyValueStore implements KeyValueStore {
    private FileSystem fileSystem;
    private String workingRoot; //Store在FileSystem中的路径
    private String nameSeparator; //文件名中不同部分的分隔

    public FileSystemKeyValueStore(FrameworkAutoConfiguration.FileSystemKeyValueStoreProperties properties, FileSystem fileSystem) {
        this.workingRoot = properties.getWorkingRoot();
        this.nameSeparator = properties.getNameSeparator();

        this.fileSystem = fileSystem;
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, 0);
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
    public void touch(String key) {
        touch(key, null);
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
            else {
                throw new InvalidFileException("找不到key：" + key);
            }
        }
        catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
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

    protected String getFileName(String key, int ttl) {
        return key + nameSeparator + (ttl < 0 ? 0 : ttl);
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
}
