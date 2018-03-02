package com.ai.southernquiet.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class CommonWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public CommonWebInit webInit() {
        return new CommonWebInit() {};
    }

    @Bean
    public ServletContextInitializer servletContextInitializer(CommonWebInit commonWebInit) {
        return commonWebInit::onStartup;
    }

    @Component
    @ConfigurationProperties("web.session.file-system")
    public static class FileSessionProperties {
        /**
         * Session持久化在FileSystem中的路径
         */
        private String workingRoot = "SESSION";

        public String getWorkingRoot() {
            return workingRoot;
        }

        public void setWorkingRoot(String workingRoot) {
            this.workingRoot = workingRoot;
        }
    }

    @Component
    @ConfigurationProperties("web")
    public class WebProperties {
        /**
         * com.ai.southernquiet.web.auth.User在Request attribute中的KEY。
         */
        private String user = "com.ai.southernquiet.web.auth.User";
        /**
         * 框架自身所用Filter的注册次序。
         */
        private int filterOrder = 0;

        /**
         * 用户身份验证的过期时间。
         * <p>虽然Session长时间保持活跃，但一定时间过后用户身份会被视为未验证的。</p>
         * <p>从remember_me中重建用户时，该用户被视为未验证的。</p>
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration authenticationTTL = Duration.ofDays(1);

        public Duration getAuthenticationTTL() {
            return authenticationTTL;
        }

        public void setAuthenticationTTL(Duration authenticationTTL) {
            this.authenticationTTL = authenticationTTL;
        }

        public int getFilterOrder() {
            return filterOrder;
        }

        public void setFilterOrder(int filterOrder) {
            this.filterOrder = filterOrder;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

    }

    @Component
    @ConfigurationProperties("web.session.remember-me")
    public class SessionRememberMeProperties {
        /**
         * 记住我的cookie名称
         */
        private String cookie = "remember_me";
        /**
         * 记住我的cookie有效时间。
         */
        private Duration timeout = Duration.ofDays(365);

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
