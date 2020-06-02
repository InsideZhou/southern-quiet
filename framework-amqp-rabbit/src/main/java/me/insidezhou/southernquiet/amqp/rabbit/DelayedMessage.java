package me.insidezhou.southernquiet.amqp.rabbit;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 声明消息至少延迟多长时间才投递到目标exchange及queue。
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DelayedMessage {
    @AliasFor("delay")
    long value() default 0;

    /**
     * 单位：毫秒。
     */
    @AliasFor("value")
    long delay() default 0;
}
