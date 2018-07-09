package com.ai.southernquiet.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractJobQueueProcessor implements JobQueueProcessor {
    private Map<Class<?>, Consumer<?>> consumerMap = new ConcurrentHashMap<>();

    @Override
    public <T> void registerJobConsumer(Class<T> cls, Consumer<T> consumer) {
        consumerMap.putIfAbsent(cls, consumer);
    }

    @SuppressWarnings("unchecked")
    public <T> Consumer<T> getConsumer(Class<T> cls) {
        return (Consumer<T>) consumerMap.get(cls);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void process() {
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

    protected abstract <T> T getJobFromQueue();

    public abstract <T> void onJobSuccess(T job);

    public abstract <T> void onJobFail(T job, Exception e) throws Exception;
}
