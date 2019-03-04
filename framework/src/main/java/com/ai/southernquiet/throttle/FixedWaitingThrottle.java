package com.ai.southernquiet.throttle;


/**
 * 以毫秒为时间单位，每个任务执行完毕后等待固定时间才能再次打开的节流器。
 */
public class FixedWaitingThrottle implements Throttle {

    /**
     * 节流器什么时候创建的
     */
    private long createdAt = System.currentTimeMillis();

    /**
     * 节流器下次打开的时间
     */
    private long nextOpening = createdAt;

    /**
     * 节流器被打开之后必须等待多长时间才能再次打开
     */
    private long waiting;

    /**
     * 节流器开启计数器
     */
    private long counter = 0;

    public FixedWaitingThrottle(long waiting) {
        this.waiting = waiting;
    }

    @Override
    public long elapsed() {
        return System.currentTimeMillis() - createdAt;
    }

    @Override
    public long counter() {
        return counter;
    }

    @Override
    synchronized public boolean open() {
        long current = System.currentTimeMillis();

        if (current >= nextOpening) {
            nextOpening = current + waiting;
            counter += 1;

            return true;
        }
        else {
            return false;
        }
    }
}
