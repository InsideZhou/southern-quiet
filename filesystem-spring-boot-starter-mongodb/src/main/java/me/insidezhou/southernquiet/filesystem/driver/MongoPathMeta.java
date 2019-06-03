package me.insidezhou.southernquiet.filesystem.driver;

import me.insidezhou.southernquiet.filesystem.NormalizedPath;
import me.insidezhou.southernquiet.filesystem.PathMeta;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MongoPathMeta extends PathMeta implements Cloneable {
    public MongoPathMeta(NormalizedPath normalizedPath, InputStream stream) {
        super(normalizedPath, stream);
    }

    public MongoPathMeta(String path, InputStream stream) {
        super(new NormalizedPath(path), stream);
    }

    public MongoPathMeta(NormalizedPath normalizedPath) {
        super(normalizedPath, null);
    }

    public MongoPathMeta(String path) {
        super(new NormalizedPath(path), null);
    }

    @Override
    public MongoPathMeta clone() {
        try {
            return (MongoPathMeta) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("id", getId());
        map.put("parentId", getParentId());
        map.put("fileId", getFileId());
        map.put("fileData", getFileData());
        return map;
    }

    private String id;
    private String parentId;

    private ObjectId fileId;
    private Binary fileData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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
