package me.insidezhou.southernquiet.throttle.lua;

import me.insidezhou.southernquiet.throttle.Throttle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 使用redis lua脚本实现的基于时间的节流器
 */
public class RedisLuaTimeBasedThrottle implements Throttle {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(
        LocalResourceUtil.getSource("/lua/RedisLuaTimeBasedThrottle.lua"),
        Boolean.class);

    private final List<String> keys;

    private long openedCount = 0;

    private final long countDelay;

    public RedisLuaTimeBasedThrottle(StringRedisTemplate stringRedisTemplate, String throttleName, long countDelay) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keys = Collections.singletonList(throttleName);
        this.countDelay = countDelay;

        if (0 == countDelay) {
            setLastOpenAtIfAbsent(keys.get(0), String.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    public boolean open(long threshold) {
        String now = Long.toString(System.currentTimeMillis());

        if (openedCount++ < countDelay) {

            if (openedCount == countDelay) {
                setLastOpenAtIfAbsent(keys.get(0), now);
            }

            return true;
        }

        Boolean execute = stringRedisTemplate.execute(redisScript, keys, Long.toString(threshold), now);
        return execute == null ? false : execute;
    }

    private void setLastOpenAtIfAbsent(String key, String openAt) {
        stringRedisTemplate.opsForValue().setIfAbsent(key, openAt);
    }
}
