package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.constants.RedisThrottleBeanName;
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

    @Bean(name = RedisThrottleBeanName.redisTimeBaseThrottle)
    public RedisTimeBaseThrottle redisTimeBaseThrottle(RedisDistributedLock redisDistributedLock) {
        return new RedisTimeBaseThrottle(redisDistributedLock);
    }

    @Bean(name = RedisThrottleBeanName.redisCounterBaseThrottle)
    public RedisCounterBaseThrottle redisCounterBaseThrottle(StringRedisTemplate stringRedisTemplate) {
        return new RedisCounterBaseThrottle(stringRedisTemplate);
    }

    @Bean
    public ThrottleManager redisThrottleManager(Throttle redisTimeBaseThrottle, ApplicationContext applicationContext) {
        return new RedisThrottleManager(redisTimeBaseThrottle, applicationContext);
    }

}
