package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lock.RedisDistributedLock;

import java.time.Duration;

/**
 * 使用redis实现的基于时间的节流器
 */
public class RedisTimeBaseThrottle implements Throttle {

    private RedisDistributedLock redisDistributedLock;

    public RedisTimeBaseThrottle(RedisDistributedLock redisDistributedLock) {
        this.redisDistributedLock = redisDistributedLock;
    }

    @Override
    public boolean open(String key, long threshold) {
        return redisDistributedLock.lock(key, Duration.ofMillis(threshold));
    }

}
