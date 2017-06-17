package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(AuthService.class)
public class AuthAutoConfiguration {
    @Bean
    public AuthFilter authFilter(AuthService authService) {
        return new AuthFilter(authService);
    }

    @Bean
    public AuthInterceptor authInterceptor(AuthService authService) {
        return new AuthInterceptor(authService);
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterFilterRegistration(AuthFilter authFilter, CommonWebAutoConfiguration.WebProperties webProperties) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>(authFilter);
        registration.setOrder(webProperties.getDefaultFilterOrder());
        return registration;
    }
}
