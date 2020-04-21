package me.insidezhou.southernquiet.throttle.lua;

import me.insidezhou.southernquiet.throttle.Throttle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 使用redis lua脚本实现的计数器节流器
 */
public class RedisLuaCountBaseThrottle implements Throttle {

    private StringRedisTemplate stringRedisTemplate;

    private static DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(
        LocalResourceUtil.getSource("/lua/RedisLuaCountBaseThrottle.lua"),
        Boolean.class);

    private List<String> keys;

    public RedisLuaCountBaseThrottle(StringRedisTemplate stringRedisTemplate, String throttleName) {
        this.stringRedisTemplate = stringRedisTemplate;

        this.keys = Collections.singletonList("Throttle_Count_" + throttleName);
    }

    /**
     * 以次数为依据打开节流器，上次打开之后必须至少节流了指定次数才能再次打开，如果打开失败返回false。
     */
    @Override
    public boolean open(long threshold) {
        Boolean execute = stringRedisTemplate.execute(redisScript, keys, String.valueOf(threshold));
        return execute == null ? false : execute;
    }

}
