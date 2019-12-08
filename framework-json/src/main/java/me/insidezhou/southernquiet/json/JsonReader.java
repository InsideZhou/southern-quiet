package me.insidezhou.southernquiet.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public interface JsonReader {
    /**
     * 把字节流转换为指定的JavaBean。
     */
    <T> T read(InputStream inputStream, Class<T> cls);

    default <T> T read(byte[] bytes, Class<T> cls) {
        return read(new ByteArrayInputStream(bytes), cls);
    }

    default <T> T read(String json, Class<T> cls) {
        return read(new ByteArrayInputStream(json.getBytes()), cls);
    }
}
