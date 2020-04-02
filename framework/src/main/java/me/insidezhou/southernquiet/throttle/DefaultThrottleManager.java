package me.insidezhou.southernquiet.throttle;

import java.util.HashMap;
import java.util.Map;

public class DefaultThrottleManager {

    private static Map<String, DefaultThrottle> throttleMap = new HashMap<>();

    public static DefaultThrottle getThrottle() {
        return getThrottle(null);
    }

    public static DefaultThrottle getThrottle(String name) {
        DefaultThrottle throttle = throttleMap.get(name);
        if (throttle == null) {
            throttle = createOrThrottle(name);
        }
        return throttle;
    }

    private static synchronized DefaultThrottle createOrThrottle(String name) {
        DefaultThrottle throttle = throttleMap.get(name);
        if (throttle != null) {
            return throttle;
        }
        throttle = new DefaultThrottle();
        throttleMap.put(name, throttle);
        return throttle;
    }

}
