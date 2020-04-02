package me.insidezhou.southernquiet.throttle;

public interface ThrottleManager {

    Throttle getThrottle();

    Throttle getThrottle(String name);

}
