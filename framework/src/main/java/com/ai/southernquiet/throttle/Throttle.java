package com.ai.southernquiet.throttle;

/**
 * 节流器
 */
public interface Throttle {
    /**
     * 以时间为依据打开节流器，上次打开之后必须至少节流了指定时间才能再次打开，如果打开失败返回false。
     */
    boolean openByTime(long threshold);

    /**
     * 以次数为依据打开节流器，上次打开之后必须至少节流了指定次数才能再次打开，如果打开失败返回false。
     */
    boolean openByCount(long threshold);
}
