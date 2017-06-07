package com.ai.southernquiet.filesystem.driver;

import com.ai.southernquiet.filesystem.PathMeta;

public class MongoPathMeta extends PathMeta {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
