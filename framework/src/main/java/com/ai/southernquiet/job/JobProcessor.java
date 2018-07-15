package com.ai.southernquiet.job;

public interface JobProcessor<T> {
    void process(T job) throws Exception;

    Class<T> getJobClass();
}
