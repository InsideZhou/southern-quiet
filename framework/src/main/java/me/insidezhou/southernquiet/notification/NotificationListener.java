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
     * 监听器并发量
     */
    int concurrency() default 1;

    /**
     * 要监听的通知类。
     */
    Class<?> notification();

    /**
     * 监听器的名称。
     */
    String name() default "";

    /**
     * recover时要使用的amplifier bean在容器内的名字
     */
    String amplifierBeanName() default "";

    @SuppressWarnings("unused")
    @Target({ElementType.METHOD})
    @Retention(RUNTIME)
    @Inherited
    @Documented
    @interface List {
        NotificationListener[] value();
    }
}
