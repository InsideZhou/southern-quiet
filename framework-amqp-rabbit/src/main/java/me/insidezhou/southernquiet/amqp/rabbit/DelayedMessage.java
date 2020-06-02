package me.insidezhou.southernquiet.amqp.rabbit;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DelayedMessage {
    @AliasFor("delay")
    long value() default 0;

    @AliasFor("value")
    long delay() default 0;
}
