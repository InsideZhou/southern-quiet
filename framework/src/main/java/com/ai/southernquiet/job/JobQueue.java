package com.ai.southernquiet.job;

/**
 * 任务队列。
 */
public interface JobQueue<T> {
    /**
     * 将Job加入队列。
     */
    void enqueue(T job);
}
