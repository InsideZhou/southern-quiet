package com.ai.southernquiet.broadcasting;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 声明该类型的事件在发布时也应该被广播到当前ApplicationContext之外。
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ShouldBroadcast {
    @AliasFor("channels")
    String[] value() default {};

    /**
     * 应该向哪些频道广播事件。
     */
    @AliasFor("value")
    String[] channels() default {};
}
