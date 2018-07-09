package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AbstractJobQueueProcessor;

public class FileJobQueueProcessor extends AbstractJobQueueProcessor {
    private FileJobQueue jobQueue;

    public FileJobQueueProcessor(FileJobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    protected <T> T getJobFromQueue() {
        return jobQueue.dequeue();
    }

    @Override
    public <T> void onJobSuccess(T job) {
    }

    @Override
    public <T> void onJobFail(T job, Exception e) {
        jobQueue.enqueue(job);
    }
}
