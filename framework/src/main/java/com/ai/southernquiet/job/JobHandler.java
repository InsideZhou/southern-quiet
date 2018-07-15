package com.ai.southernquiet.job;

public interface JobHandler<T> {
    void handle(T job) throws Exception;

    Class<T> getJobClass();
}
