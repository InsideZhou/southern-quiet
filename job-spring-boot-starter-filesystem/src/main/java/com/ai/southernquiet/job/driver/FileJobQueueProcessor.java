package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AbstractJobQueueProcessor;
import com.ai.southernquiet.job.SerializableJob;

public class FileJobQueueProcessor extends AbstractJobQueueProcessor<SerializableJob> {
    private FileJobQueue jobQueue;

    public FileJobQueueProcessor(FileJobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    protected SerializableJob getJobFromQueue() {
        return jobQueue.dequeue();
    }

    @Override
    public void onJobSuccess(SerializableJob job) {
    }

    @Override
    public void onJobFail(SerializableJob job, Exception e) {
        jobQueue.enqueue(job);
    }
}
