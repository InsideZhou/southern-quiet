package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.job.FileSystemJobAutoConfiguration;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

public class FileJobQueue<T extends Serializable> implements JobQueue<T> {
    public static <T extends Serializable> InputStream serialize(T data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(data));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(byte[] bytes) {
        return (T) SerializationUtils.deserialize(bytes);
    }

    private FileSystem fileSystem;
    private String workingRoot; //队列持久化在FileSystem中的路径
    private ThreadLocal<String> lastDequeuedJobId = new ThreadLocal<>();

    public ThreadLocal<String> getLastDequeuedJobId() {
        return lastDequeuedJobId;
    }

    public void setLastDequeuedJobId(ThreadLocal<String> lastDequeuedJobId) {
        this.lastDequeuedJobId = lastDequeuedJobId;
    }

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    public FileJobQueue(FileSystem fileSystem, FileSystemJobAutoConfiguration.Properties properties) {
        this.workingRoot = properties.getWorkingRoot();

        fileSystem.createDirectory(this.workingRoot);
        this.fileSystem = fileSystem;
    }

    @Override
    public void enqueue(T job) {
        try {
            fileSystem.put(getFilePath(UUID.randomUUID().toString()), serialize(job));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T dequeue() {
        try {
            Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, "", false, 0, 1, PathMetaSort.CreationTime).findFirst();

            if (opt.isPresent()) {
                String path = opt.get().getPath();

                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    byte[] bytes = StreamUtils.copyToByteArray(inputStream);

                    T job = deserialize(bytes);
                    fileSystem.delete(path);
                    return job;
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

    private String getFilePath(String jobId) {
        return workingRoot + FileSystem.PATH_SEPARATOR + jobId;
    }
}
