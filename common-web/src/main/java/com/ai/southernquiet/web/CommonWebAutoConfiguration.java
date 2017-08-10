package com.ai.southernquiet.web;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.session.jetty.FileSessionDataStore;
import com.ai.southernquiet.web.session.spring.FileSessionRepository;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

@Configuration
public class CommonWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean({SessionDataStore.class})
    @ConditionalOnMissingClass("org.springframework.session.Session")
    public FileSessionDataStore sessionDataStore(FileSystem fileSystem, FileSessionProperties properties) {
        return new FileSessionDataStore(fileSystem, properties);
    }

    @Bean
    @ConditionalOnClass(Session.class)
    @ConditionalOnMissingBean
    public FileSessionRepository fileSessionRepository(FileSystem fileSystem, FileSessionProperties properties) {
        return new FileSessionRepository(fileSystem, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommonWebInit webInit() {
        return new CommonWebInit() {};
    }

    @Bean
    public ServletContextInitializer servletContextInitializer(CommonWebInit commonWebInit) {
        return servletContext -> commonWebInit.onStartup(servletContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public JettyConfiguration jettyConfiguration() {
        return new JettyConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public JettyServletWebServerFactory servletContainerFactory(JettyConfiguration jettyConfiguration) {
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
        factory.addConfigurations(jettyConfiguration);
        return factory;
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
         * 用户身份验证的过期时间，单位：秒。
         * <p>虽然Session长时间保持活跃，但一定时间过后用户身份会被视为未验证的。</p>
         * <p>从remember_me中重建用户时，该用户被视为未验证的。</p>
         */
        private int authenticationTTL = 86400;

        public int getAuthenticationTTL() {
            return authenticationTTL;
        }

        public void setAuthenticationTTL(int authenticationTTL) {
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
         * 记住我的cookie有效时间，单位：秒
         */
        private Integer timeout = 31536000;

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }
}
