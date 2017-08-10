package com.ai.southernquiet.web.session.spring;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.InvalidFileException;
import com.ai.southernquiet.util.SerializationUtils;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.session.ExpiringSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 基于{@link com.ai.southernquiet.filesystem.FileSystem}的Spring Session持久化.
 */
public class FileSessionRepository implements SessionRepository<ExpiringSession> {
    private FileSystem fileSystem;
    private String workingRoot; //Session持久化在FileSystem中的路径

    public FileSessionRepository(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
        this.workingRoot = properties.getWorkingRoot();
        this.fileSystem = fileSystem;

        fileSystem.create(this.workingRoot);
    }

    @Override
    public ExpiringSession createSession() {
        return new FileSession();
    }

    @Override
    public void save(ExpiringSession session) {
        try {
            fileSystem.put(getFilePath(session.getId()), serialize(session));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExpiringSession getSession(String id) {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) return null;

        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            return deserialize(inputStream);
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String id) {
        fileSystem.delete(getFilePath(id));
    }

    private String getFilePath(String sessionId) {
        return workingRoot + FileSystem.PATH_SEPARATOR + sessionId;
    }

    private InputStream serialize(ExpiringSession session) {
        return new ByteArrayInputStream(SerializationUtils.serialize(session));
    }

    private ExpiringSession deserialize(InputStream stream) {
        try {
            return (ExpiringSession) SerializationUtils.deserialize(StreamUtils.copyToByteArray(stream));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.springframework.session.MapSession
     */
    public static class FileSession implements ExpiringSession, Serializable {
        private final static long serialVersionUID = -4029548868967931682L;

        private long creationTime;
        private long lastAccessedTime;
        private int maxInactiveIntervalInSeconds;
        private boolean expired;
        private String id;
        private Map<String, Object> attributes = new HashMap<>();

        public FileSession() {
            this(UUID.randomUUID().toString());
        }

        public FileSession(String id) {
            this.id = id;
        }

        public FileSession(ExpiringSession session) {
            this.creationTime = session.getCreationTime();
            this.lastAccessedTime = session.getLastAccessedTime();
            this.maxInactiveIntervalInSeconds = session.getMaxInactiveIntervalInSeconds();
            this.expired = session.isExpired();
            this.id = session.getId();

            for (String key : session.getAttributeNames()) {
                this.attributes.put(key, session.getAttribute(key));
            }
        }

        public boolean equals(Object obj) {
            return obj instanceof Session && this.id.equals(((Session) obj).getId());
        }

        public int hashCode() {
            return this.id.hashCode();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getAttribute(String attributeName) {
            return (T) attributes.get(attributeName);
        }

        @Override
        public Set<String> getAttributeNames() {
            return attributes.keySet();
        }

        @Override
        public void setAttribute(String attributeName, Object attributeValue) {
            attributes.put(attributeName, attributeValue);
        }

        @Override
        public void removeAttribute(String attributeName) {
            attributes.remove(attributeName);
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public long getCreationTime() {
            return creationTime;
        }

        public void setCreationTime(long creationTime) {
            this.creationTime = creationTime;
        }

        @Override
        public long getLastAccessedTime() {
            return lastAccessedTime;
        }

        @Override
        public void setLastAccessedTime(long lastAccessedTime) {
            this.lastAccessedTime = lastAccessedTime;
        }

        @Override
        public int getMaxInactiveIntervalInSeconds() {
            return maxInactiveIntervalInSeconds;
        }

        @Override
        public void setMaxInactiveIntervalInSeconds(int maxInactiveIntervalInSeconds) {
            this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
