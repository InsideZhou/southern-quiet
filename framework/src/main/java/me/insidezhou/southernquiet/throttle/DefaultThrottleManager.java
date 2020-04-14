package me.insidezhou.southernquiet.throttle;

public class DefaultThrottleManager extends BaseThrottleManager {

    @Override
    public Throttle createTimeBased(String throttleName) {
        return new DefaultTimeBasedThrottle();
    }

    @Override
    public Throttle createCountBased(String throttleName) {
        return new DefaultCountBasedThrottle();
    }

}
