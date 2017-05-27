package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebProperties;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestWrapperFilter implements Filter {
    private AuthService authService;
    private CommonWebProperties webProperties;

    public CommonWebProperties getWebProperties() {
        return webProperties;
    }

    public void setWebProperties(CommonWebProperties webProperties) {
        this.webProperties = webProperties;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Integer timeout = getWebProperties().getSession().getRememberMe().getTimeout();
        String cookie = getWebProperties().getSession().getRememberMe().getCookie();
        String user = getWebProperties().getSession().getUser();

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
