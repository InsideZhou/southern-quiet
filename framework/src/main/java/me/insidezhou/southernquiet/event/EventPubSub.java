package me.insidezhou.southernquiet.event;

import java.util.Set;

/**
 * 发布及广播事件。
 * 广播指发布到当前ApplicationContext之外。
 */
public interface EventPubSub<E> {
    String DefaultEventChannel = "CHANNEL.PUBLIC";
    String CustomApplicationEventChannel = "CHANNEL.CUSTOM_APPLICATION";

    /**
     * 仅在本地（当前ApplicationContext）发布事件，不对外广播。
     */
    void publishToLocalOnly(E event);

    /**
     * 发布事件。被{@link ShouldBroadcast}标注的事件将通过其指定的频道发送广播。
     */
    void publish(E event);

    Set<String> getListeningChannels();
}
