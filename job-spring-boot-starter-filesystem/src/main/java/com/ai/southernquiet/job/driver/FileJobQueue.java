package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.job.FileSystemJobAutoConfiguration;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

public class FileJobQueue<T extends Serializable> extends OnSiteJobQueue<T> implements JobQueue<T> {
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
    protected void onJobFail(T job, Exception e) throws Exception {
        fileSystem.put(getFilePath(UUID.randomUUID().toString()), serialize(job));
    }

    private String getFilePath(String jobId) {
        return workingRoot + FileSystem.PATH_SEPARATOR + jobId;
    }
}
