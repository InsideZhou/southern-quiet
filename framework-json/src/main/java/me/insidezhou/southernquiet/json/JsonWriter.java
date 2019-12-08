package me.insidezhou.southernquiet.json;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface JsonWriter {
    /**
     * 将JavaBean JSON序列化成字节流。
     */
    byte[] write(Object object);

    /**
     * 将JavaBean JSON序列化成字符串。
     */
    default String writeAsString(Object object, Charset charset) {
        return new String(write(object), charset);
    }

    /**
     * 将JavaBean JSON序列化成字符串。
     */
    default String writeAsString(Object object) {
        return writeAsString(object, StandardCharsets.UTF_8);
    }
}
