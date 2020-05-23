package me.insidezhou.southernquiet.throttle.lua;

import me.insidezhou.southernquiet.throttle.BaseThrottleManager;
import me.insidezhou.southernquiet.throttle.Throttle;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisLuaThrottleManager extends BaseThrottleManager implements DisposableBean {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLuaThrottleManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Throttle createTimeBased(String throttleName) {
        return new RedisLuaTimeBasedThrottle(stringRedisTemplate, throttleName);
    }

    @Override
    public Throttle createCountBased(String throttleName) {
        return new RedisLuaCountBasedThrottle(stringRedisTemplate, throttleName);
    }

    @Override
    public void destroy() {
        timeBaseThrottleMap.values().forEach(throttle -> ((RedisLuaTimeBasedThrottle) throttle).destroy());
        countBaseThrottleMap.values().forEach(throttle -> ((RedisLuaCountBasedThrottle) throttle).destroy());
    }
}
