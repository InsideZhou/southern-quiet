package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lock.RedisDistributedLock;
import org.springframework.context.ApplicationContext;
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

    @Bean(name="redisTimeBaseThrottle")
    public RedisTimeBaseThrottle redisTimeBaseThrottle(RedisDistributedLock redisDistributedLock) {
        return new RedisTimeBaseThrottle(redisDistributedLock);
    }

    @Bean(name="redisCounterBaseThrottle")
    public RedisCounterBaseThrottle redisCounterBaseThrottle() {
        return new RedisCounterBaseThrottle();
    }

    @Bean
    public ThrottleManager redisThrottleManager(Throttle redisTimeBaseThrottle, ApplicationContext applicationContext) {
        return new RedisThrottleManager(redisTimeBaseThrottle, applicationContext);
    }

}
