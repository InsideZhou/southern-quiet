package me.insidezhou.southernquiet.throttle;

import org.springframework.context.ApplicationContext;

public class RedisThrottleManager implements ThrottleManager {

    private Throttle redisTimeBaseThrottle;

    private ApplicationContext applicationContext;

    public RedisThrottleManager(Throttle redisTimeBaseThrottle,ApplicationContext applicationContext) {
        this.redisTimeBaseThrottle = redisTimeBaseThrottle;
        this.applicationContext = applicationContext;
    }

    @Override
    public Throttle getThrottle() {
        return redisTimeBaseThrottle;
    }

    @Override
    public Throttle getThrottle(String name) {
        return applicationContext.getBean(name, Throttle.class);
    }

}
