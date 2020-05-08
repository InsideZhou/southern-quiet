package me.insidezhou.southernquiet.auth;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 权限验证。被标记的方法或类只能被拥有指定权限的用户访问。
 * 当类及其方法成员均被标记时，每个Auth以其自身模式执行，但所有Auth都必须验证通过。
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
    /**
     * 权限标识。空则标识任意权限均可认证通过，包括空权限。
     */
    @AliasFor("value")
    String[] permissions() default {};

    @AliasFor("permissions")
    String[] value() default {};

    MatchMode mode() default MatchMode.All;

    /**
     * 权限验证时，多权限情况下的匹配模式。
     */
    enum MatchMode {
        /**
         * 匹配列出的所有权限
         */
        All,
        /**
         * 匹配列出的任意权限
         */
        Any
    }
}
