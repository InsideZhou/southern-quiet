package com.ai.southernquiet.cache;


/**
 * 带ttl(time to live，单位ms)控制的缓存。
 */
public interface Cache {
    /**
     * 设置缓存。
     *
     * @see #put(String, Object, int)
     */
    void put(String key, Object value);

    /**
     * 设置带ttl控制的缓存。
     *
     * @param ttl 必须大于等于0，0常驻不过期。
     */
    void put(String key, Object value, int ttl);

    /**
     * 设置常驻缓存。如果缓存已存在，仅更改缓存值。
     */
    void set(String key, Object value);

    /**
     * 获取缓存（未过期的）。
     */
    Object get(String key);

    /**
     * 刷新缓存创建时间，以及更改ttl。注意，不保证这是一个原子操作。
     *
     * @param key 缓存键
     * @param ttl 当不为null时，更改ttl。
     */
    void touch(String key, Integer ttl);

    /**
     * 移除指定缓存。
     */
    void remove(String... keys);
}
