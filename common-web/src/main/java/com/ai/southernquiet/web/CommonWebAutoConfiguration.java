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
        private String workingRoot;

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
         * com.ai.southernquiet.web.auth.User保存为Request attribute时使用的KEY。
         */
        private String user;
        private int defaultFilterOrder;

        public int getDefaultFilterOrder() {
            return defaultFilterOrder;
        }

        public void setDefaultFilterOrder(int defaultFilterOrder) {
            this.defaultFilterOrder = defaultFilterOrder;
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
        private String cookie;
        /**
         * 记住我的cookie有效时间，单位：秒
         */
        private Integer timeout;

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
