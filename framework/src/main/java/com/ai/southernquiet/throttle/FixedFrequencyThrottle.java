package com.ai.southernquiet.throttle;


/**
 * 以毫秒为时间单位，按固定频率打开的节流器。
 * 本节流器在打开之后，除非接到任务请求，否则不会关闭。
 */
public class FixedFrequencyThrottle implements Throttle {

    /**
     * 节流器下次打开的时间
     */
    private long nextOpening = System.currentTimeMillis();

    /**
     * 节流器被打开之后必须等待多长时间才能再次打开
     */
    private long frequency;

    /**
     * 节流器被打开了几次
     */
    private long counter = 0;

    /**
     * 节流器什么时候创建的
     */
    private long createdAt = System.currentTimeMillis();

    @Override
    public long getCounter() {
        return counter;
    }

    @Override
    public long getElapsed() {
        return System.currentTimeMillis() - createdAt;
    }

    public FixedFrequencyThrottle(long frequency) {
        this.frequency = frequency;
    }

    @Override
    public void open() throws Throttle.CannotOpenException {
        long current = System.currentTimeMillis();

        if (current >= nextOpening) {
            nextOpening = current + frequency;
            counter += 1;
        }
        else {
            throw new Throttle.CannotOpenException(nextOpening - current);
        }
    }
}
