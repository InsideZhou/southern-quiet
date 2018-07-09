package com.ai.southernquiet.broadcasting;

/**
 * 发布及广播事件。
 * 广播指通过{@link Broadcaster}发布到当前ApplicationContext之外。
 */
public interface Publisher {
    /**
     * 仅在本地（当前ApplicationContext）发布事件，不对外广播。
     */
    <T> void publishToLocalOnly(T event);

    /**
     * 发布事件。被{@link ShouldBroadcast}标注的事件将通过其指定的频道发送广播。
     */
    <E extends Event> void publish(E event);
}
