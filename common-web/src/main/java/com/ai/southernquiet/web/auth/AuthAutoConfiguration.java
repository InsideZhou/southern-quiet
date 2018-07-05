package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@ConditionalOnBean(AuthService.class)
public class AuthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AuthFilter authFilter(AuthService authService) {
        return new AuthFilter(authService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthInterceptor authInterceptor(AuthService authService) {
        return new AuthInterceptor(authService);
    }

    @Bean
    @ConditionalOnMissingBean
    public User.HandlerMethodArgumentResolver userHandlerMethodArgumentResolver() {
        return new User.HandlerMethodArgumentResolver();
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterFilterRegistration(AuthFilter authFilter, CommonWebAutoConfiguration.WebProperties webProperties) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>(authFilter);
        registration.setOrder(webProperties.getFilterOrder());
        return registration;
    }
}
