package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lock.RedisDistributedLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class RedisThrottleAutoConfiguration {

    @Bean
    public RedisDistributedLock redisDistributedLock(StringRedisTemplate stringRedisTemplate) {
        return new RedisDistributedLock(stringRedisTemplate);
    }

    @Bean
    public ThrottleManager redisThrottleManager(RedisDistributedLock redisDistributedLock,StringRedisTemplate stringRedisTemplate) {
        return new RedisThrottleManager(stringRedisTemplate, redisDistributedLock);
    }

}
