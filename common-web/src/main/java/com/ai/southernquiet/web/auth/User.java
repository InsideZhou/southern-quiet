package com.ai.southernquiet.web.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 会话用户
 *
 * @param <T> 关联的账号。
 */
public class User<T extends Account> implements Serializable {
    private final static long serialVersionUID = -2874340118038495940L;

    static int AuthenticationTTL;

    public User(T account) {
        this(account, null);
    }

    public User(T account, String rememberToken) {
        setAccount(account);
        setRememberToken(rememberToken);
    }

    private T account;
    private Set<String> roles = new HashSet<>();
    private long authenticationTime;
    private String rememberToken;

    public T getAccount() {
        return account;
    }

    public void setAccount(T account) {
        this.account = account;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public long getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(long authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public static int getAuthenticationTTL() {
        return AuthenticationTTL;
    }

    public static void setAuthenticationTTL(int authenticationTTL) {
        AuthenticationTTL = authenticationTTL;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }

    public boolean isAuthenticated() {
        return System.currentTimeMillis() < getAuthenticationTime() + AuthenticationTTL; //距离上次验证时间超过限制则视为验证已过期。
    }

    public static class HandlerMethodArgumentResolver implements org.springframework.web.method.support.HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return User.class.isAssignableFrom(parameter.getParameterType());
        }

        @SuppressWarnings("unchecked")
        @Override
        public User<?> resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
            Request request = webRequest.getNativeRequest(Request.class);
            return null == request ? null : request.getUser();
        }
    }
}
