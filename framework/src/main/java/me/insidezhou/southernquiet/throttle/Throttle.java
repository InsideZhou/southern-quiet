package me.insidezhou.southernquiet.throttle;

/**
 * 节流器
 */
public interface Throttle {

    boolean open(String key, long threshold);

}
