package com.ai.southernquiet.job;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractJob implements Job {
    protected Instant lastExecutionTime;

    protected int executionCount;

    protected int retryLimit;

    protected String id = UUID.randomUUID().toString();

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

    public abstract void execute();

    public abstract void onFail();
}
