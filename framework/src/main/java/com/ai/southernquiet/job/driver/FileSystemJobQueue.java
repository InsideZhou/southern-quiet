package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.job.Job;
import com.ai.southernquiet.job.JobAutoConfiguration;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class FileSystemJobQueue implements JobQueue {
    private static Logger logger = LoggerFactory.getLogger(FileSystemJobQueue.class);

    private FileSystem fileSystem;
    private String workingRoot; //队列持久化在FileSystem中的路径

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    public FileSystemJobQueue(FileSystem fileSystem, JobAutoConfiguration.Properties properties) {
        this.workingRoot = properties.getFileSystem().getWorkingRoot();

        fileSystem.createDirectory(this.workingRoot);
        this.fileSystem = fileSystem;
    }

    @Override
    public void enqueue(Job job) {
        synchronized (this) {
            try {
                fileSystem.put(getFilePath(job.getId()), serialize(job));
            }
            catch (InvalidFileException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Job dequeue() {
        synchronized (this) {
            try {
                Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, "", false, 0, 1, PathMetaSort.CreationTime).findFirst();

                if (opt.isPresent()) {
                    try (InputStream inputStream = fileSystem.openReadStream(opt.get().getPath())) {
                        byte[] bytes = StreamUtils.copyToByteArray(inputStream);

                        Job job = deserialize(bytes);
                        remove(job);
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
    }

    @Override
    public void remove(Job job) {
        synchronized (this) {
            try {
                fileSystem.files(workingRoot, job.getId())
                    .findFirst()
                    .ifPresent(meta -> fileSystem.delete(meta.getPath()));
            }
            catch (PathNotFoundException e) {
                logger.info(String.format("没有找到要删除的Job %s", job.getId()), e);
            }
        }
    }

    private String getFilePath(String jobId) {
        return workingRoot + FileSystem.PATH_SEPARATOR + jobId;
    }

    private InputStream serialize(Job data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(data));
    }

    private Job deserialize(byte[] bytes) {
        return ((Job) SerializationUtils.deserialize(bytes));
    }
}
