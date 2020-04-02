package me.insidezhou.southernquiet.throttle;

import java.util.HashMap;
import java.util.Map;

public class DefaultThrottleManager implements ThrottleManager {

    private Map<String, DefaultThrottle> throttleMap = new HashMap<>();

    public DefaultThrottle getThrottle() {
        return getThrottle(null);
    }

    public DefaultThrottle getThrottle(String name) {
        DefaultThrottle throttle = throttleMap.get(name);
        if (throttle == null) {
            throttle = createThrottle(name);
        }
        return throttle;
    }

    private synchronized DefaultThrottle createThrottle(String name) {
        DefaultThrottle throttle = throttleMap.get(name);
        if (throttle != null) {
            return throttle;
        }
        throttle = new DefaultThrottle();
        throttleMap.put(name, throttle);
        return throttle;
    }

}
