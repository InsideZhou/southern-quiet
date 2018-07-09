package com.ai.southernquiet.job;

/**
 * 任务队列。
 */
public interface JobQueue {
    /**
     * 将Job加入队列。
     */
    <T> void enqueue(T job);

    /**
     * 从队列中获取（并移除）Job。
     */
    <T> T dequeue();
}
