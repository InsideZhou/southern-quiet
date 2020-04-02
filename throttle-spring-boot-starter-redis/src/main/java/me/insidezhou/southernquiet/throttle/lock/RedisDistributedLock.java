package me.insidezhou.southernquiet.throttle.lock;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class RedisDistributedLock {

    private StringRedisTemplate stringRedisTemplate;

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String LOCK_KEY_PRE = "LOCK_";

    private String getLockKey(String key) {
        return LOCK_KEY_PRE + key;
    }

    public boolean lock(String key, Duration timeout) {
        String lockKey = getLockKey(key);
        Boolean getLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, LOCK_KEY_PRE, timeout);
        return getLock == null ? false : getLock;
    }

    public void unlock(String key) {
        String lockKey = getLockKey(key);
        stringRedisTemplate.delete(lockKey);
    }

}
