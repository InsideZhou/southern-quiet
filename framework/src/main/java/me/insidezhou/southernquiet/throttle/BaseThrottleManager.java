package me.insidezhou.southernquiet.throttle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class BaseThrottleManager implements ThrottleManager {

    private ConcurrentMap<String, Throttle> timeBaseThrottleMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, Throttle> countBaseThrottleMap = new ConcurrentHashMap<>();

    @Override
    public Throttle getTimeBased(String throttleName) {
        if (throttleName == null) {
            return getTimeBased();
        }
        Throttle throttle = timeBaseThrottleMap.get(throttleName);
        if (throttle != null) {
            return throttle;
        }
        timeBaseThrottleMap.putIfAbsent(throttleName, createTimeBased(throttleName));
        return timeBaseThrottleMap.get(throttleName);
    }

    public abstract Throttle createTimeBased(String throttleName);

    @Override
    public Throttle getCountBased(String throttleName) {
        if (throttleName == null) {
            return getCountBased();
        }
        Throttle throttle = countBaseThrottleMap.get(throttleName);
        if (throttle != null) {
            return throttle;
        }
        countBaseThrottleMap.putIfAbsent(throttleName, createCountBased(throttleName));
        return countBaseThrottleMap.get(throttleName);
    }

    public abstract Throttle createCountBased(String throttleName);



    }
