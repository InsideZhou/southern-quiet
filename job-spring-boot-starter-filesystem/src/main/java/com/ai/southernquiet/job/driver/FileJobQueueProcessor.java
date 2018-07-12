package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AbstractJobQueueProcessor;

import java.io.Serializable;

public class FileJobQueueProcessor<T extends Serializable> extends AbstractJobQueueProcessor<T> {
    private FileJobQueue<T> jobQueue;

    public FileJobQueueProcessor(FileJobQueue<T> jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    protected T getJobFromQueue() {
        return jobQueue.dequeue();
    }

    @Override
    public void onJobSuccess(T job) {
    }

    @Override
    public void onJobFail(T job, Exception e) {
        jobQueue.enqueue(job);
    }
}
