package me.insidezhou.southernquiet.util;

/**
 * 框架的元数据
 */
public interface Metadata {
    /**
     * 框架运行时的id，必须唯一。
     */
    String getRuntimeId();

    int getCoreNumber();
}
