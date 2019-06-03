package me.insidezhou.southernquiet.job;

public interface JobProcessor<T> {
    void process(T job) throws Exception;

    Class<T> getJobClass();
}
