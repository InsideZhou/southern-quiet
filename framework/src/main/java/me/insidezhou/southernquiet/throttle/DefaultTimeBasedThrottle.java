package me.insidezhou.southernquiet.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class DefaultTimeBasedThrottle implements Throttle {
    private final static Logger log = LoggerFactory.getLogger(DefaultTimeBasedThrottle.class);

    /**
     * 上次开闸时间
     */
    private Long lastOpenedAt;
    private long openedCount = 0;

    private final long countDelay;

    public DefaultTimeBasedThrottle(long countDelay) {
        this.countDelay = countDelay;
    }

    /**
     * 以时间为依据打开节流器，上次打开之后必须至少节流了指定时间才能再次打开，如果打开失败返回false。
     */
    @Override
    public synchronized boolean open(long threshold) {
        if (openedCount++ < countDelay) {
            return true;
        }

        long now = System.currentTimeMillis();

        if (threshold <= 0) {
            return reset(now);
        }

        if (null == lastOpenedAt) {
            lastOpenedAt = now;
        }

        if (log.isTraceEnabled()) {
            log.trace("throttled millis\tnow={}, lastOpenedAt={}, threshold={}, throttled={}", now, lastOpenedAt, threshold, now - lastOpenedAt);
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
