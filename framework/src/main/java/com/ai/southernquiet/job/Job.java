package com.ai.southernquiet.job;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 表示一项任务，可以加入{@link JobScheduler}中，由队列调度并执行。
 *
 * @see JobScheduler
 */
public abstract class Job implements Serializable {
    protected Instant lastExecutionTime;

    /**
     * 该任务的已执行次数。
     */
    protected int executionCount;

    /**
     * 该任务的最大重试次数。
     */
    protected int retryLimit;

    /**
     * 该任务的标识。
     */
    protected String id = UUID.randomUUID().toString();

    /**
     * 进入延迟执行状态。
     */
    protected boolean postpone;

    public boolean isPostpone() {
        return postpone;
    }

    public void setPostpone(boolean postpone) {
        this.postpone = postpone;
    }

    public Instant getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(Instant lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public String getId() {
        return id;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    /**
     * 由调度器执行该任务。
     */
    public abstract void execute();

    /**
     * 当任务失败（且超过重试限制）时，由调度器调用。
     */
    public abstract void onFail();
}
