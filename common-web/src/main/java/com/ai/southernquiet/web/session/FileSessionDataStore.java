package com.ai.southernquiet.web.session;

import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.util.SerializationUtils;
import com.ai.southernquiet.web.CommonWebProperties;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

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
    private String workingRoot = "SESSION"; //Session持久化在FileSystem中的路径

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    public FileSessionDataStore(FileSystem fileSystem, CommonWebProperties properties) {
        String workingRoot = properties.getSession().getFileSystem().getWorkingRoot();
        if (StringUtils.hasText(workingRoot)) {
            setWorkingRoot(workingRoot);
        }

        fileSystem.create(getWorkingRoot());
        setFileSystem(fileSystem);

    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        getFileSystem().put(getFilePath(id), serialize(data));
    }

    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        long now = System.currentTimeMillis();

        return candidates.stream()
            .map(id -> {
                try {
                    return getFileSystem().files(getWorkingRoot(), id);
                }
                catch (PathNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .flatMap(metas -> metas.stream())
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

        return getFileSystem().files(getWorkingRoot(), id).stream()
            .anyMatch(meta -> getByMeta(meta).getExpiry() > now);
    }

    @Override
    public SessionData load(String id) throws Exception {
        Optional<PathMeta> opt = getFileSystem().files(getWorkingRoot(), id).stream().findFirst();
        if (opt.isPresent()) {
            InputStream stream = getFileSystem().read(opt.get().getPath());
            return deserialize(stream);
        }

        return null;
    }

    @Override
    public boolean delete(String id) throws Exception {
        Optional<PathMeta> opt = getFileSystem().files(getWorkingRoot(), id).stream().findFirst();
        opt.ifPresent(meta -> getFileSystem().delete(meta.getPath()));

        return true;
    }

    private String getFileName(String sessionId) {
        FileSystemHelper.assertFileNameValid(sessionId);
        return sessionId;
    }

    private String getFilePath(String sessionId) {
        return getWorkingRoot() + FileSystem.PATH_SEPARATOR + getFileName(sessionId);
    }

    private SessionData getByMeta(PathMeta meta) {
        InputStream inputStream;
        try {
            inputStream = getFileSystem().read(meta.getPath());
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }

        return deserialize(inputStream);
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
