package com.ai.southernquiet.web;

import com.ai.southernquiet.web.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class AbstractWebApp implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (null != authInterceptor) {
            registry.addInterceptor(authInterceptor);
        }
    }

    private AuthInterceptor authInterceptor;

    public AuthInterceptor getAuthInterceptor() {
        return authInterceptor;
    }

    @Autowired(required = false)
    public void setAuthInterceptor(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }
}
