package com.ai.southernquiet.web.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 会话用户
 *
 * @param <T> 关联的账号。
 */
public class User<T extends Account> implements Serializable {
    private final static long serialVersionUID = -2874340118038495940L;

    public User(T account, Duration authenticationTTL) {
        this(account, null, authenticationTTL);
    }

    public User(T account, String rememberToken, Duration authenticationTTL) {
        setAccount(account);
        setRememberToken(rememberToken);
        setAuthenticationTTL(authenticationTTL);
    }

    private T account;
    private Set<String> roles = new HashSet<>();
    private Duration authenticationTTL;
    private Instant authenticationTime;
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

    public Duration getAuthenticationTTL() {
        return authenticationTTL;
    }

    public void setAuthenticationTTL(Duration authenticationTTL) {
        this.authenticationTTL = authenticationTTL;
    }

    public Instant getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Instant authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }

    public boolean isAuthenticated() {
        return Duration.between(getAuthenticationTime(), Instant.now()).compareTo(getAuthenticationTTL()) > 0; //距离上次验证时间超过限制则视为验证已过期。
    }

    @SuppressWarnings("NullableProblems")
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
