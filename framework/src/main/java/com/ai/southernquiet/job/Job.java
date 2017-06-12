package com.ai.southernquiet.job;

import java.io.Serializable;
import java.time.Instant;

/**
 * 表示一项任务，可以加入{@link JobScheduler}中，由队列调度并执行。
 *
 * @see JobScheduler
 */
public interface Job extends Serializable {
    /**
     * 该任务的上次执行时间。
     */
    Instant getLastExecutionTime();

    void setLastExecutionTime(Instant lastExecutionTime);

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
     * 进入延迟执行状态，当任务出于此状态时，调度器会延迟当前任务处理，重新排队。
     */
    boolean isPostpone();

    void setPostpone(boolean postpone);

    /**
     * 该任务的标识。
     */
    String getId();

    void setId(String id);

    /**
     * 由调度器执行该任务。
     */
    void execute();

    /**
     * 当任务失败（且超过重试限制）时，由调度器调用。
     */
    void onFail();
}
