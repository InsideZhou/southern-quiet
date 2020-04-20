package me.insidezhou.southernquiet.keyvalue.driver;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.filesystem.*;
import me.insidezhou.southernquiet.keyvalue.KeyValueStore;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 基于 {@link FileSystem} 的键值对驱动.
 */
public class FileSystemKeyValueStore implements KeyValueStore {
    private final FileSystem fileSystem;
    private final String workingRoot; //Store在FileSystem中的路径
    private final String nameSeparator; //文件名中不同部分的分隔

    public FileSystemKeyValueStore(FrameworkAutoConfiguration.KeyValueStoreProperties.FileSystem properties, FileSystem fileSystem) {
        this.workingRoot = properties.getWorkingRoot();
        this.nameSeparator = properties.getNameSeparator();

        this.fileSystem = fileSystem;
    }

    @Override
    public <T extends Serializable> void put(String key, T value, int ttl) {
        try {
            fileSystem.put(getFilePath(key, ttl), serialize(value));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends Serializable> void set(String key, T value) {
        try {
            fileSystem.put(getFilePath(key, -1), serialize(value));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T get(String key) {
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
                    return (T) deserialize(inputStream);
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
        return key + nameSeparator + (Math.max(ttl, 0));
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

    private Stream<? extends PathMeta> getMetaStream() {
        try {
            return fileSystem.files(workingRoot);
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream serialize(Object data) {
        byte[] bytes = SerializationUtils.serialize(data);
        return null == bytes ? null : new ByteArrayInputStream(bytes);
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
