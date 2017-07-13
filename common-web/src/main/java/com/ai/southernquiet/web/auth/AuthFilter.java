package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter extends OncePerRequestFilter {
    private AuthService authService;
    private CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties;
    private CommonWebAutoConfiguration.WebProperties webProperties;

    @Autowired
    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    public CommonWebAutoConfiguration.WebProperties getWebProperties() {
        return webProperties;
    }

    @Autowired
    public void setWebProperties(CommonWebAutoConfiguration.WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    public CommonWebAutoConfiguration.SessionRememberMeProperties getRememberMeProperties() {
        return rememberMeProperties;
    }

    @Autowired
    public void setRememberMeProperties(CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties) {
        this.rememberMeProperties = rememberMeProperties;
    }

    @Override
    protected void initFilterBean() throws ServletException {
        Request.REMEMBER_ME_TIMEOUT = rememberMeProperties.getTimeout();
        Request.KEY_REMEMBER_ME_COOKIE = rememberMeProperties.getCookie();
        Request.KEY_USER = webProperties.getUser();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new Request(request, response, authService), response);
    }
}
