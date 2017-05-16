package com.ai.southernquiet.cache;


import java.util.Map;

/**
 * 带ttl(time to live，单位ms)控制的缓存。
 * <p>
 * 缓存的合法key格式目前只考虑数字、字母、下划线。
 */
public interface GenericCache<T> {
    /**
     * 设置缓存。
     *
     * @param ttl 如果小于0，则为常驻缓存。
     */
    void put(String key, T value, int ttl);

    /**
     * 设置常驻缓存。如果缓存已存在，仅更改缓存值。
     */
    void set(String key, T value);

    /**
     * 获取未过期缓存。
     */
    T get(String key);

    /**
     * 刷新缓存创建时间，以及更改ttl。注意，不保证这是一个原子操作。
     *
     * @param key 缓存键
     * @param ttl 当为null时，忽略改动。
     */
    void touch(String key, Integer ttl);

    /**
     * 获取所有未过期缓存。
     */
    Map<String, T> getAlive();

    /**
     * 获取所有已过期缓存。
     */
    Map<String, T> getExpired();

    /**
     * 移除指定缓存。
     */
    void remove(String... keys);

    /**
     * 查找缓存。
     *
     * @param search 以contains方式查找的名称。如果为空，返回所有结果。
     */
    Map<String, T> find(String search);
}
