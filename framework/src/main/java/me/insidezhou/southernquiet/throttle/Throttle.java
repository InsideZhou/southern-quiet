package me.insidezhou.southernquiet.throttle;

/**
 * 节流器
 */
public interface Throttle {
    /**
     * 打开节流器。节流器是否能打开取决于threshold是否达到或超过。
     *
     * @param threshold 阈值的含义由实现类定义，可以是基于时间的，未达到时间阈值无法打开；也可以是基于次数的，未达到次数阈值无法打开。
     * @return 打开失败返回false。
     */
    boolean open(long threshold);
}
