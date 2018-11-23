package com.ai.southernquiet.notification;

/**
 * 发布通知。
 */
public interface NotificationPublisher<N> {
    String DefaultNotificationSource = "NOTIFICATION.PUBLIC";

    /**
     * 发布通知。被{@link NotificationSource}标注的通知将通过其指定的源发送。
     */
    void publish(N notification);
}
