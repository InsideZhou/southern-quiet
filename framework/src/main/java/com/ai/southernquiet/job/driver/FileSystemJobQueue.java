package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.FrameworkProperties;
import com.ai.southernquiet.filesystem.*;
import com.ai.southernquiet.job.Job;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class FileSystemJobQueue implements JobQueue {
    private Logger logger = LoggerFactory.getLogger(FileSystemJobQueue.class);
    private FileSystem fileSystem;
    private String workingRoot = "JOB_QUEUE"; //队列持久化在FileSystem中的路径

    public FileSystemJobQueue(FileSystem fileSystem, FrameworkProperties properties) {
        String workingRoot = properties.getJob().getFileSystem().getDefaultDriver().getWorkingRoot();
        if (StringUtils.hasText(workingRoot)) {
            setWorkingRoot(workingRoot);
        }

        fileSystem.create(getWorkingRoot());
        setFileSystem(fileSystem);
    }

    @Override
    public void enqueue(Job job) {
        try {
            getFileSystem().put(getFilePath(job.getId()), serialize(job));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Job dequeue() {
        try {
            Optional<PathMeta> opt = getFileSystem().files(getWorkingRoot(), "", false, 0, 1, PathMetaSort.CreationTime).stream().findFirst();

            if (opt.isPresent()) {
                byte[] bytes = StreamUtils.copyToByteArray(getFileSystem().read(opt.get().getPath()));

                Job job = deserialize(bytes);
                remove(job);
                return job;
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
    public void remove(Job job) {
        try {
            getFileSystem().files(getWorkingRoot(), job.getId()).stream().findFirst().ifPresent(meta -> getFileSystem().delete(meta.getPath()));
        }
        catch (PathNotFoundException e) {
            logger.info(String.format("没有找到要删除的Job %s", job.getId()), e);
        }
    }

    private String getFileName(String jobId) {
        FileSystemHelper.assertFileNameValid(jobId);
        return jobId;
    }

    private String getFilePath(String jobId) {
        return getWorkingRoot() + FileSystem.PATH_SEPARATOR + getFileName(jobId);
    }

    private InputStream serialize(Job data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(data));
    }

    private Job deserialize(byte[] bytes) {
        return ((Job) SerializationUtils.deserialize(bytes));
    }

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
}
