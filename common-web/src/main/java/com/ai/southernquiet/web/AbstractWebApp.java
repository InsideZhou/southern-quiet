package com.ai.southernquiet.web;

import com.ai.southernquiet.web.auth.AuthInterceptor;
import com.ai.southernquiet.web.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class AbstractWebApp implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (null != authService) {
            registry.addInterceptor(new AuthInterceptor(authService));
        }
    }

    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    @Autowired(required = false)
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }
}
