package me.insidezhou.southernquiet.throttle;

@SuppressWarnings("WeakerAccess")
public class DefaultTimeBasedThrottle implements Throttle {

    DefaultTimeBasedThrottle() {
    }

    /**
     * 下次开闸时间
     */
    private long shouldOpenedAt = 0;

    /**
     * 以时间为依据打开节流器，上次打开之后必须至少节流了指定时间才能再次打开，如果打开失败返回false。
     */
    @Override
    public synchronized boolean open(long threshold) {
        long now = System.currentTimeMillis();

        if (now >= shouldOpenedAt) {
            shouldOpenedAt = now + threshold;
            return true;
        } else {
            return false;
        }
    }

}
