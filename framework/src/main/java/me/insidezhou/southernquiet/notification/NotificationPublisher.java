package me.insidezhou.southernquiet.notification;

/**
 * 发布通知。
 */
public interface NotificationPublisher<N> {
    /**
     * 发布通知。
     */
    void publish(N notification);
}
