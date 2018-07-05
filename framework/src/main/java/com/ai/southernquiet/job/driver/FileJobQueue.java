package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.job.Job;
import com.ai.southernquiet.job.JobAutoConfiguration;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public class FileJobQueue implements JobQueue {
    private static Log log = LogFactory.getLog(FileJobQueue.class);

    public static InputStream serialize(Job data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(data));
    }

    public static Job deserialize(byte[] bytes) {
        return ((Job) SerializationUtils.deserialize(bytes));
    }

    private FileSystem fileSystem;
    private String workingRoot; //队列持久化在FileSystem中的路径
    private ThreadLocal<String> lastDequeuedJobId = new ThreadLocal<>();

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    public FileJobQueue(FileSystem fileSystem, JobAutoConfiguration.Properties properties) {
        this.workingRoot = properties.getFileSystem().getWorkingRoot();

        fileSystem.createDirectory(this.workingRoot);
        this.fileSystem = fileSystem;
    }

    @Override
    public void enqueue(Job job) {
        try {
            fileSystem.put(getFilePath(UUID.randomUUID().toString()), serialize(job));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Job dequeue() {
        try {
            Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, "", false, 0, 1, PathMetaSort.CreationTime).findFirst();

            if (opt.isPresent()) {
                String path = opt.get().getPath();

                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    byte[] bytes = StreamUtils.copyToByteArray(inputStream);

                    Job job = deserialize(bytes);
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
