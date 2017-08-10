package com.ai.southernquiet.web;

import com.ai.southernquiet.web.auth.AuthInterceptor;
import com.ai.southernquiet.web.auth.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

public class AbstractWebApp implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (null != authInterceptor) {
            registry.addInterceptor(authInterceptor);
        }
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (null != userHandlerMethodArgumentResolver) {
            argumentResolvers.add(userHandlerMethodArgumentResolver);
        }
    }

    private AuthInterceptor authInterceptor;
    private User.HandlerMethodArgumentResolver userHandlerMethodArgumentResolver;

    public AuthInterceptor getAuthInterceptor() {
        return authInterceptor;
    }

    @Autowired(required = false)
    public void setAuthInterceptor(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    public User.HandlerMethodArgumentResolver getUserHandlerMethodArgumentResolver() {
        return userHandlerMethodArgumentResolver;
    }

    @Autowired(required = false)
    public void setUserHandlerMethodArgumentResolver(User.HandlerMethodArgumentResolver userHandlerMethodArgumentResolver) {
        this.userHandlerMethodArgumentResolver = userHandlerMethodArgumentResolver;
    }
}
