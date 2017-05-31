package com.ai.southernquiet.job;

/**
 * 任务队列，供调度器使用。
 */
public interface JobQueue {
    /**
     * 将Job加入队列。
     */
    void enqueue(Job job);

    /**
     * 从队列中获取并移除Job。获取规则由子类决定。
     */
    Job dequeue();

    /**
     * 从队列中移除指定Job。
     */
    void remove(Job job);
}
