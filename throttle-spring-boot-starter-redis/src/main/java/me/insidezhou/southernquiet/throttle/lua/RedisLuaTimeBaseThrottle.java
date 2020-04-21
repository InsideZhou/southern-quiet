package me.insidezhou.southernquiet.throttle.lua;

import me.insidezhou.southernquiet.throttle.Throttle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 使用redis lua脚本实现的基于时间的节流器
 */
public class RedisLuaTimeBaseThrottle implements Throttle {

    private StringRedisTemplate stringRedisTemplate;

    private static DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(
        LocalResourceUtil.getSource("/lua/RedisLuaTimeBaseThrottle.lua"),
        Boolean.class);

    private List<String> keys;

    public RedisLuaTimeBaseThrottle(StringRedisTemplate stringRedisTemplate, String throttleName) {
        this.stringRedisTemplate = stringRedisTemplate;

        String key = "Throttle_Time_" + throttleName;

        this.keys = Collections.singletonList(key);

        setLastOpenAtIfAbsent(key, System.currentTimeMillis());
    }

    @Override
    public boolean open(long threshold) {
        String now = Long.toString(System.currentTimeMillis());
        Boolean execute = stringRedisTemplate.execute(redisScript, keys, Long.toString(threshold), now);
        return execute == null ? false : execute;
    }

    private void setLastOpenAtIfAbsent(String key, long openAt) {
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(openAt));
    }

}
