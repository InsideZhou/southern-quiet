package com.ai.southernquiet.broadcasting;

import java.lang.annotation.*;

import static com.ai.southernquiet.broadcasting.Broadcaster.CustomApplicationEventChannel;


/**
 * 让被广播到不同ApplicationContext中的自定义事件可直接被spring的 {@link org.springframework.context.event.EventListener} 机制处理。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ShouldBroadcast(CustomApplicationEventChannel)
public @interface ShouldBroadcastCustomApplicationEvent {
}
