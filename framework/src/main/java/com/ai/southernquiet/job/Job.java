package com.ai.southernquiet.job;

import java.io.Serializable;

/**
 * 表示一项任务，可以加入{@link JobScheduler}中，由队列调度并执行。
 *
 * @see JobScheduler
 */
public interface Job extends Serializable {
    /**
     * 该任务的标识。
     */
    String getId();

    /**
     * 由调度器执行该任务。
     */
    void execute();

    /**
     * 当任务失败（且超过重试限制）时，由调度器调用。
     *
     * @param e 任务执行失败时产生的异常
     */
    void onFail(Exception e);
}
