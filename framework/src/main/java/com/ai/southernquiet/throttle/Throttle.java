package com.ai.southernquiet.throttle;

/**
 * 节流器
 */
@SuppressWarnings("unused")
public interface Throttle {
    /**
     * 节流器存活了多长时间
     */
    long elapsed();

    /**
     * 节流器开启计数器
     */
    long counter();

    boolean open();
}
