package com.ai.southernquiet.job;

import java.util.UUID;

public abstract class AbstractJob implements Job {
    protected int executionCount;
    protected int retryLimit;
    protected String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getExecutionCount() {
        return executionCount;
    }

    @Override
    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    @Override
    public int getRetryLimit() {
        return retryLimit;
    }

    @Override
    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    @Override
    public void execute() {}
}
