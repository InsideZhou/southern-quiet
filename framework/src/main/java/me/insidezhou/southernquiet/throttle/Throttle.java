package me.insidezhou.southernquiet.throttle;

/**
 * 节流器
 */
public interface Throttle {
    /**
     * 打开节流器。节流器是否能打开取决于节流指标的数值是否达到或超过阈值。
     * 节流开始的时间由实现类定义，一般情况下是节流器生成即开始，也可延迟开始。
     * <p>
     * Throttle throttle = ...创建一个以时间为指标，单位为毫秒的节流器... ;
     * throttle.open(300); //在时间流逝未达到300毫秒时返回false。
     * </p>
     *
     * @param threshold 阈值的含义由实现类定义，可以是基于时间的，未达到时间阈值无法打开；也可以是基于次数的，未达到次数阈值无法打开。
     * @return 打开失败返回false。
     */
    boolean open(long threshold);
}
