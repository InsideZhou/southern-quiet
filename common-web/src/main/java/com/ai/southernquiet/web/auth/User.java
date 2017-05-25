package com.ai.southernquiet.web.auth;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {
    public static int AuthenticationTTL = 86400;

    public User(String username) {
        setUsername(username);
    }

    public User(String username, String rememberToken) {
        setUsername(username);
        setRememberToken(rememberToken);
    }

    private String username;
    private Set<String> roles = new HashSet<>();
    private long authenticationTime;
    private String rememberToken;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}
