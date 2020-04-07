package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lock.RedisDistributedLock;

import java.time.Duration;

/**
 * 使用redis实现的基于时间的节流器
 */
public class RedisTimeBaseThrottle implements Throttle {

    private RedisDistributedLock redisDistributedLock;

    private String throttleName;

    public RedisTimeBaseThrottle(RedisDistributedLock redisDistributedLock, String throttleName) {
        this.throttleName = "Throttle_Time_"+throttleName;
        this.redisDistributedLock = redisDistributedLock;
    }

    @Override
    public boolean open(long threshold) {
        return redisDistributedLock.lock(throttleName, Duration.ofMillis(threshold));
    }

}
