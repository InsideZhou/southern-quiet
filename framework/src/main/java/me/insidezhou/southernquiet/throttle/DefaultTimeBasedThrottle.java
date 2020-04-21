package me.insidezhou.southernquiet.throttle;

@SuppressWarnings("WeakerAccess")
public class DefaultTimeBasedThrottle implements Throttle {

    /**
     * 上次开闸时间
     */
    private long lastOpenedAt = System.currentTimeMillis();

    /**
     * 以时间为依据打开节流器，上次打开之后必须至少节流了指定时间才能再次打开，如果打开失败返回false。
     */
    @Override
    public synchronized boolean open(long threshold) {
        long now = System.currentTimeMillis();
        if (threshold <= 0) {
            return reset(now);
        }

        if (now >= lastOpenedAt + threshold) {
            return reset(now);
        }
        else {
            return false;
        }
    }

    private boolean reset(long openAt) {
        lastOpenedAt = openAt;
        return true;
    }

}
