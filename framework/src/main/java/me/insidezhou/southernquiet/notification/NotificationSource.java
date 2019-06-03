package me.insidezhou.southernquiet.notification;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 声明通知的来源。
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface NotificationSource {
    @AliasFor("source")
    String value();

    @AliasFor("value")
    String source();
}
