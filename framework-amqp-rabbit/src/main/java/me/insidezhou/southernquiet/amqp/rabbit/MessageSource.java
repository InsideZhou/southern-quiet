package me.insidezhou.southernquiet.amqp.rabbit;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 声明消息的来源，用于构造Exchange、Queue的名字。
 *
 * <p>组成格式"$prefix.$messageSource"</p>
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MessageSource {
    @AliasFor("source")
    String value();

    @AliasFor("value")
    String source();
}
