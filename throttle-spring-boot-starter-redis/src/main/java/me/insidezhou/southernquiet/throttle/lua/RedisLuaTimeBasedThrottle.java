package me.insidezhou.southernquiet.throttle.lua;

import me.insidezhou.southernquiet.throttle.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 使用redis lua脚本实现的基于时间的节流器
 */
public class RedisLuaTimeBasedThrottle implements Throttle, DisposableBean {
    private final static Logger log = LoggerFactory.getLogger(RedisLuaTimeBasedThrottle.class);

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(
        LocalResourceUtil.getSource("/lua/RedisLuaTimeBasedThrottle.lua"),
        Boolean.class);

    private final List<String> keys;

    public RedisLuaTimeBasedThrottle(StringRedisTemplate stringRedisTemplate, String throttleName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keys = Collections.singletonList(throttleName);

        stringRedisTemplate.opsForValue().setIfAbsent(throttleName, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public boolean open(long threshold) {
        String now = Long.toString(System.currentTimeMillis());
        Boolean execute = stringRedisTemplate.execute(redisScript, keys, Long.toString(threshold), now);
        return execute == null ? false : execute;
    }

    @Override
    public void destroy() {
        Long count = stringRedisTemplate.delete(keys);
        log.debug("RedisThrottle的key已清理\tkey={}, count={}", keys, count);
    }
}
