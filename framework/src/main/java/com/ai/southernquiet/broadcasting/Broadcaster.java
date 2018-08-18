package com.ai.southernquiet.broadcasting;

/**
 * 事件广播器。
 */
public interface Broadcaster<E> {
    String CustomApplicationEventChannel = "CustomApplicationEvent";

    /**
     * 广播事件。
     *
     * @param event    事件
     * @param channels 频道
     */
    void broadcast(E event, String[] channels);
}
