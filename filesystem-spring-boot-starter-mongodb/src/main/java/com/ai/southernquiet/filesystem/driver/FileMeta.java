package com.ai.southernquiet.filesystem.driver;

import com.ai.southernquiet.filesystem.PathMeta;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import java.util.Map;

public class FileMeta extends MongoPathMeta {
    private ObjectId fileId;
    private Binary fileData;

    public FileMeta() {}

    public FileMeta(PathMeta pathMeta) {
        if (pathMeta.isDirectory()) {
            throw new RuntimeException(String.format("该路径%s指向的是目录而不是文件，无法生成FileMeta。", pathMeta.getPath()));
        }

        setParent(pathMeta.getParent());
        setName(pathMeta.getName());
        setDirectory(false);
        setCreationTime(pathMeta.getCreationTime());
        setLastAccessTime(pathMeta.getLastAccessTime());
        setLastModifiedTime(pathMeta.getLastModifiedTime());
        setSize(pathMeta.getSize());
    }

    public FileMeta(FileMeta meta) {
        this((PathMeta) meta);

        setFileId(meta.getFileId());
    }

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
