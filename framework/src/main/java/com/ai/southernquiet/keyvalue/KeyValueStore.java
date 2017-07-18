package com.ai.southernquiet.keyvalue;


/**
 * 带ttl(time to live，单位ms)控制的键值对。
 */
public interface KeyValueStore {
    /**
     * 设置键值对。
     *
     * @see #put(String, Object, int)
     */
    void put(String key, Object value);

    /**
     * 设置带ttl控制的键值对。
     *
     * @param ttl 必须大于等于0，0为常驻不过期。
     */
    void put(String key, Object value, int ttl);

    /**
     * 设置常驻键值对。如果键值对已存在，仅更改键值对值。
     */
    void set(String key, Object value);

    /**
     * 获取键值对（未过期的）。
     */
    Object get(String key);

    /**
     * 刷新键值对创建时间。
     */
    void touch(String key);

    /**
     * 刷新键值对创建时间，以及更改ttl。注意，不保证这是一个原子操作。
     *
     * @param key 键值
     * @param ttl 当不为null时，更改ttl。
     */
    void touch(String key, Integer ttl);

    /**
     * 移除指定键值对。
     */
    void remove(String... keys);
}
