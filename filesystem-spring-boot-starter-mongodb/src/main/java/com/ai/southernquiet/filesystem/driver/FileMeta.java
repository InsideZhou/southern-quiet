package com.ai.southernquiet.filesystem.driver;

import org.bson.types.Binary;
import org.bson.types.ObjectId;

import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class FileMeta extends MongoPathMeta {
    private ObjectId fileId;
    private Binary fileData;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("fileId", getFileId());
        map.put("fileData", getFileData());
        return map;
    }

    public ObjectId getFileId() {
        return fileId;
    }

    public void setFileId(ObjectId fileId) {
        this.fileId = fileId;
    }

    public Binary getFileData() {
        return fileData;
    }

    public void setFileData(Binary fileData) {
        this.fileData = fileData;
    }
}
