package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lock.RedisDistributedLock;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisThrottleManager extends BaseThrottleManager {

    private StringRedisTemplate stringRedisTemplate;

    private RedisDistributedLock redisDistributedLock;

    public RedisThrottleManager(StringRedisTemplate stringRedisTemplate, RedisDistributedLock redisDistributedLock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisDistributedLock = redisDistributedLock;
    }

    @Override
    public Throttle getTimeBasedInternal(String throttleName) {
        return new RedisTimeBaseThrottle(redisDistributedLock,throttleName);
    }

    @Override
    public Throttle getCountBasedInternal(String throttleName) {
        return new RedisCountBaseThrottle(stringRedisTemplate,throttleName);
    }
}
