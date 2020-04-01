package me.insidezhou.southernquiet.notification;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 订阅通知。
 */
@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
@Documented
@Repeatable(NotificationListener.List.class)
public @interface NotificationListener {
    /**
     * 要监听的通知类。
     */
    Class<?> notification();

    /**
     * 监听器的名称。
     */
    String name() default "";

    @Target({ElementType.METHOD})
    @Retention(RUNTIME)
    @Inherited
    @Documented
    @interface List {
        NotificationListener[] value();
    }
}
