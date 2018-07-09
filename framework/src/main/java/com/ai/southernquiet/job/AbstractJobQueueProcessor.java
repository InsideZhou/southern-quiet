package com.ai.southernquiet.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractJobQueueProcessor<T extends Job> implements JobQueueProcessor<T> {
    private Map<Class<T>, Consumer<T>> consumerMap = new ConcurrentHashMap<>();

    @Override
    public void registerJobConsumer(Class<T> cls, Consumer<T> consumer) {
        consumerMap.putIfAbsent(cls, consumer);
    }

    @SuppressWarnings("unchecked")
    public Consumer<T> getConsumer(Class<T> cls) {
        return consumerMap.get(cls);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process() {
        T job = getJobFromQueue();

        Consumer<T> consumer = getConsumer((Class<T>) job.getClass());
        if (null == consumer) return;

        try {
            consumer.accept(job);
        }
        catch (Exception e) {
            try {
                onJobFail(job, e);
            }
            catch (Exception e1) {
                throw new RuntimeException(e1);
            }

            return;
        }

        onJobSuccess(job);
    }

    protected abstract T getJobFromQueue();

    public abstract void onJobSuccess(T job);

    public abstract void onJobFail(T job, Exception e) throws Exception;
}
