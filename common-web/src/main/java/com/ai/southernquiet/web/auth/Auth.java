package com.ai.southernquiet.web.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 身份及权限验证。
 * <p>1、身份验证。</p>
 * <p>2、角色检查。白名单不为空时，身份必须在白名单内；黑名单不为空时，身份必须<b>不</b>在黑名单内。</p>
 * <p>3、根据 {@link Auth#name()} 检查身份是否具备此权限，为空时忽略检查。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
    /**
     * 权限名称。
     */
    String name() default "";

    /**
     * 角色白名单。
     */
    String[] whiteRoles() default {};

    /**
     * 角色黑名单。
     */
    String[] blackRoles() default {};
}
