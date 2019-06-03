package me.insidezhou.southernquiet.event;

import java.lang.annotation.*;

import static me.insidezhou.southernquiet.event.EventPublisher.CustomApplicationEventChannel;


/**
 * 被标记的自定义事件会被广播到当前ApplicationContext之外，且支持spring的 {@link org.springframework.context.event.EventListener} 机制。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ShouldBroadcast(CustomApplicationEventChannel)
public @interface CustomApplicationEvent {
}
