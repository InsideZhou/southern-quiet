package com.ai.southernquiet.web.session.spring;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.InvalidFileException;
import com.ai.southernquiet.filesystem.NormalizedPath;
import com.ai.southernquiet.util.SerializationUtils;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 基于{@link com.ai.southernquiet.filesystem.FileSystem}的Spring Session持久化.
 */
public class FileSessionRepository implements SessionRepository<Session> {
    private FileSystem fileSystem;
    private String workingRoot; //Session持久化在FileSystem中的路径

    public FileSessionRepository(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
        this.workingRoot = properties.getWorkingRoot();
        this.fileSystem = fileSystem;

        fileSystem.createDirectory(this.workingRoot);
    }

    @Override
    public Session createSession() {
        return new FileSession();
    }

    @Override
    public void save(Session session) {
        try {
            fileSystem.put(getFilePath(session.getId()), serialize(session));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Session findById(String id) {
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
    public void deleteById(String id) {
        fileSystem.delete(getFilePath(id));
    }

    private String getFilePath(String sessionId) {
        NormalizedPath path = new NormalizedPath(new String[]{workingRoot, sessionId});
        return path.toString();
    }

    private InputStream serialize(Session session) {
        return new ByteArrayInputStream(SerializationUtils.serialize(session));
    }

    private Session deserialize(InputStream stream) {
        try {
            return (Session) SerializationUtils.deserialize(StreamUtils.copyToByteArray(stream));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.springframework.session.MapSession
     */
    public static class FileSession implements Session, Serializable {
        private final static long serialVersionUID = -3973763939484496044L;

        private Instant creationTime;
        private Instant lastAccessedTime;
        private Duration maxInactiveInterval;
        private boolean expired;
        private String id;
        private Map<String, Object> attributes = new HashMap<>();

        public FileSession() {
            this(UUID.randomUUID().toString());
        }

        public FileSession(String id) {
            this.id = id;
        }

        public FileSession(Session session) {
            this.creationTime = session.getCreationTime();
            this.lastAccessedTime = session.getLastAccessedTime();
            this.maxInactiveInterval = session.getMaxInactiveInterval();
            this.expired = session.isExpired();
            this.id = session.getId();

            for (String key : session.getAttributeNames()) {
                this.attributes.put(key, session.getAttribute(key));
            }
        }

        @Override
        public String changeSessionId() {
            String newId = UUID.randomUUID().toString();
            setId(newId);
            return newId;
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

        @Override
        public Instant getCreationTime() {
            return creationTime;
        }

        public void setCreationTime(Instant creationTime) {
            this.creationTime = creationTime;
        }

        @Override
        public Instant getLastAccessedTime() {
            return lastAccessedTime;
        }

        @Override
        public void setLastAccessedTime(Instant lastAccessedTime) {
            this.lastAccessedTime = lastAccessedTime;
        }

        @Override
        public Duration getMaxInactiveInterval() {
            return maxInactiveInterval;
        }

        @Override
        public void setMaxInactiveInterval(Duration maxInactiveInterval) {
            this.maxInactiveInterval = maxInactiveInterval;
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

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
    }
}
