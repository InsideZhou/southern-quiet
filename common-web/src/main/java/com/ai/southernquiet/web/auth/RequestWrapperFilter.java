package com.ai.southernquiet.web.auth;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestWrapperFilter implements Filter {
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        AuthService authService = getAuthService();

        if (null == authService) {
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(new Request((HttpServletRequest) request, (HttpServletResponse) response, authService), response);
    }

    @Override
    public void destroy() {
    }
}
