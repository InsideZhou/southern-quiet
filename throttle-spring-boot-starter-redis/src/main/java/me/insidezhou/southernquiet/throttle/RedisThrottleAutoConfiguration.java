package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lua.RedisLuaThrottleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class RedisThrottleAutoConfiguration {

    @Bean
    public ThrottleManager redisThrottleManager(StringRedisTemplate stringRedisTemplate) {
        return new RedisLuaThrottleManager(stringRedisTemplate);
    }

}
