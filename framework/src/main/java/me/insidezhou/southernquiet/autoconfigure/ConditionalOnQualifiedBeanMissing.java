package me.insidezhou.southernquiet.autoconfigure;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnQualifiedBeanMissingCondition.class)
public @interface ConditionalOnQualifiedBeanMissing {
    String qualifier() default "";

    Class<?>[] classes() default {};
}
