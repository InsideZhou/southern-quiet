package com.ai.southernquiet.job;

import java.io.Serializable;

/**
 * 表示一项任务，可以加入{@link JobScheduler}中，由队列调度并执行。
 *
 * @see JobScheduler
 */
public interface Job extends Serializable {
    /**
     * 获取该任务的标识。
     */
    String getId();

    /**
     * 该任务的已执行次数。
     */
    int getExecutionCount();

    void setExecutionCount(int executionCount);

    /**
     * 该任务的最大重试次数。
     */
    int getRetryLimit();

    void setRetryLimit(int retryLimit);

    /**
     * 由调度器执行该任务。
     */
    void execute();
}
