package com.ai.southernquiet.job;

import java.util.UUID;

public abstract class AbstractJob implements Job {
    protected String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public abstract void onFail(Exception e);

    @Override
    public abstract void execute();
}
