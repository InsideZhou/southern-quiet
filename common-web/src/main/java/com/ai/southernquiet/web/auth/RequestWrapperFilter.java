package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestWrapperFilter implements Filter {
    private AuthService authService;
    private CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties;
    private CommonWebAutoConfiguration.WebProperties webProperties;

    public CommonWebAutoConfiguration.WebProperties getWebProperties() {
        return webProperties;
    }

    public void setWebProperties(CommonWebAutoConfiguration.WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public CommonWebAutoConfiguration.SessionRememberMeProperties getRememberMeProperties() {
        return rememberMeProperties;
    }

    public void setRememberMeProperties(CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties) {
        this.rememberMeProperties = rememberMeProperties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Integer timeout = rememberMeProperties.getTimeout();
        String cookie = rememberMeProperties.getCookie();
        String user = webProperties.getUser();

        if (null != timeout) Request.REMEMBER_ME_TIMEOUT = timeout;
        if (StringUtils.hasText(cookie)) Request.KEY_REMEMBER_ME_COOKIE = cookie;
        if (StringUtils.hasText(user)) Request.KEY_USER = user;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new Request((HttpServletRequest) request, (HttpServletResponse) response, getAuthService()), response);
    }

    @Override
    public void destroy() {
    }
}
