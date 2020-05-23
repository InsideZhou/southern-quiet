package me.insidezhou.southernquiet.throttle;

public class DefaultThrottleManager extends BaseThrottleManager {

    @Override
    public Throttle createTimeBased(String throttleName, long countDelay) {
        return new DefaultTimeBasedThrottle(countDelay);
    }

    @Override
    public Throttle createCountBased(String throttleName) {
        return new DefaultCountBasedThrottle();
    }

}
