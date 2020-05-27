package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;

@SuppressWarnings("WeakerAccess")
public class DefaultTimeBasedThrottle implements Throttle {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(DefaultTimeBasedThrottle.class);

    /**
     * 上次开闸时间
     */
    private Long lastOpenedAt;
    private long openedCount = 0;

    private final long countDelay;

    public DefaultTimeBasedThrottle(long countDelay) {
        this.countDelay = countDelay;

        if (0 == countDelay) {
            lastOpenedAt = System.currentTimeMillis();
        }
    }

    /**
     * 以时间为依据打开节流器，上次打开之后必须至少节流了指定时间才能再次打开，如果打开失败返回false。
     */
    @Override
    public synchronized boolean open(long threshold) {
        long now = System.currentTimeMillis();

        if (openedCount++ < countDelay) {

            if (openedCount == countDelay) {
                lastOpenedAt = now;
            }

            return true;
        }

        if (threshold <= 0) {
            return reset(now);
        }

        log.message("throttled millis")
            .context("now", now)
            .context("lastOpenedAt", lastOpenedAt)
            .context("threshold", threshold)
            .context("throttled", now - lastOpenedAt)
            .trace();

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
