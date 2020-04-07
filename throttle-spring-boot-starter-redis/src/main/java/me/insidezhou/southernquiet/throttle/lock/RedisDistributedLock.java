package me.insidezhou.southernquiet.throttle.lock;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class RedisDistributedLock {

    private StringRedisTemplate stringRedisTemplate;

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean lock(String key, Duration timeout) {
        Boolean getLock = stringRedisTemplate.opsForValue().setIfAbsent(key, key, timeout);
        return getLock == null ? false : getLock;
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

}
