package com.ai.southernquiet.web.session.jetty;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.InvalidFileException;
import com.ai.southernquiet.filesystem.PathMeta;
import com.ai.southernquiet.filesystem.PathNotFoundException;
import com.ai.southernquiet.util.SerializationUtils;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于{@link com.ai.southernquiet.filesystem.FileSystem}的Jetty Session持久化.
 */
public class FileSessionDataStore extends AbstractSessionDataStore {
    private FileSystem fileSystem;
    private String workingRoot; //Session持久化在FileSystem中的路径

    public FileSessionDataStore(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
        this.workingRoot = properties.getWorkingRoot();
        this.fileSystem = fileSystem;

        fileSystem.create(this.workingRoot);
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        fileSystem.put(getFilePath(id), serialize(data));
    }

    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        long now = System.currentTimeMillis();

        return candidates.stream()
            .map(id -> {
                try {
                    return fileSystem.files(workingRoot, id);
                }
                catch (PathNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .flatMap(metas -> metas)
            .filter(meta -> getByMeta(meta).getExpiry() <= now)
            .map(meta -> meta.getName())
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isPassivating() {
        return true;
    }

    @Override
    public boolean exists(String id) throws Exception {
        long now = System.currentTimeMillis();

        return fileSystem.files(workingRoot, id).anyMatch(meta -> getByMeta(meta).getExpiry() > now);
    }

    @Override
    public SessionData load(String id) throws Exception {
        Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, id).findFirst();
        if (opt.isPresent()) {
            try (InputStream inputStream = fileSystem.openReadStream(opt.get().getPath())) {
                return deserialize(inputStream);
            }
        }

        return null;
    }

    @Override
    public boolean delete(String id) throws Exception {
        Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, id).findFirst();
        opt.ifPresent(meta -> fileSystem.delete(meta.getPath()));

        return true;
    }

    private String getFilePath(String sessionId) {
        return workingRoot + FileSystem.PATH_SEPARATOR + sessionId;
    }

    private SessionData getByMeta(PathMeta meta) {
        try (InputStream inputStream = fileSystem.openReadStream(meta.getPath())) {
            return deserialize(inputStream);
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private InputStream serialize(SessionData data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(new SessionJSON(data)));
    }

    private SessionData deserialize(InputStream stream) {
        try {
            return ((SessionJSON) SerializationUtils.deserialize(StreamUtils.copyToByteArray(stream))).toData();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
