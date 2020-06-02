package me.insidezhou.southernquiet.notification;

/**
 * 发布通知。
 */
public interface NotificationPublisher<N> {
    /**
     * 发布通知。
     */
    default void publish(N notification) {
        publish(notification, 0);
    }

    /**
     * 在指定延迟后发布通知。
     *
     * @param delay 要延迟的时间，单位：毫秒。
     */
    void publish(N notification, long delay);
}
