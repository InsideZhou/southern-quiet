package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@ConditionalOnBean(AuthService.class)
public class AuthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RequestFactory<Request> requestFactory() {
        return new RequestFactory<Request>() {
            @Override
            public Class<Request> getRequestClass() {
                return Request.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Request createInstance(HttpServletRequest httpServletRequest,
                                          HttpServletResponse httpServletResponse,
                                          CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties,
                                          CommonWebAutoConfiguration.WebProperties webProperties,
                                          AuthService authService) {

                return new Request(httpServletRequest, httpServletResponse, rememberMeProperties, webProperties, authService);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthFilter authFilter(AuthService authService,
                                 RequestFactory requestFactory,
                                 CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties,
                                 CommonWebAutoConfiguration.WebProperties webProperties) {
        return new AuthFilter(requestFactory, rememberMeProperties, webProperties, authService);
    }

    @Bean
    @ConditionalOnMissingBean
    public <R extends Request> AuthInterceptor authInterceptor(AuthService authService,
                                                               RequestFactory<R> requestFactory,
                                                               CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties) {

        return new AuthInterceptor<>(authService, requestFactory, rememberMeProperties);
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
