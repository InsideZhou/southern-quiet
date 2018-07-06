package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("NullableProblems")
public class AuthFilter extends OncePerRequestFilter {
    private AuthService authService;
    private CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties;
    private CommonWebAutoConfiguration.WebProperties webProperties;
    private RequestFactory requestFactory;

    public AuthFilter(RequestFactory requestFactory,
                      CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties,
                      CommonWebAutoConfiguration.WebProperties webProperties,
                      AuthService authService) {

        this.authService = authService;
        this.requestFactory = requestFactory;
        this.rememberMeProperties = rememberMeProperties;
        this.webProperties = webProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(requestFactory.createInstance(request, response, rememberMeProperties, webProperties, authService), response);
    }
}
