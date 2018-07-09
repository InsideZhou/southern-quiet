package com.ai.southernquiet.job;

import java.util.function.Consumer;

/**
 * 任务队列处理器。
 */
public interface JobQueueProcessor {
    /**
     * 处理队列中的任务。
     */
    <T> void process();

    /**
     * 注册Job处理器。
     *
     * @param cls      Job类型
     * @param consumer Job处理器
     */
    <T> void registerJobConsumer(Class<T> cls, Consumer<T> consumer);
}
