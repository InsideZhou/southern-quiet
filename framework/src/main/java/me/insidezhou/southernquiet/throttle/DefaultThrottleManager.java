package me.insidezhou.southernquiet.throttle;

public class DefaultThrottleManager extends BaseThrottleManager {

    @Override
    public Throttle getTimeBasedInternal(String throttleName) {
        return new DefaultTimeBasedThrottle();
    }

    @Override
    public Throttle getCountBasedInternal(String throttleName) {
        return new DefaultCountBasedThrottle();
    }

}
