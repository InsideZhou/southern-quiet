package com.ai.southernquiet.web;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class CommonWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(SessionDataStore.class)
    public FileSessionDataStore sessionDataStore(FileSystem fileSystem, Properties properties) {
        return new FileSessionDataStore(fileSystem, properties);
    }

    @Bean
    @ConditionalOnMissingBean(CommonWebInit.class)
    public CommonWebInit webInit() {
        return new CommonWebInit() {};
    }

    @Bean
    public ServletContextInitializer servletContextInitializer(CommonWebInit commonWebInit) {
        return servletContext -> commonWebInit.onStartup(servletContext);
    }

    @Bean
    @ConditionalOnMissingBean(JettyServletWebServerFactory.class)
    public JettyServletWebServerFactory servletContainerFactory(JettyConfiguration jettyConfiguration) {
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
        factory.addConfigurations(jettyConfiguration);
        return factory;
    }

    @Component
    @ConfigurationProperties("web")
    public class Properties {
        private Session session = new Session();

        public Session getSession() {
            return session;
        }

        public void setSession(Session session) {
            this.session = session;
        }

        public class Session {
            private FileSystem fileSystem = new FileSystem();
            private RememberMe rememberMe = new RememberMe();
            /**
             * com.ai.southernquiet.web.auth.User保存为Request attribute时使用的KEY。
             */
            private String user;

            public FileSystem getFileSystem() {
                return fileSystem;
            }

            public void setFileSystem(FileSystem fileSystem) {
                this.fileSystem = fileSystem;
            }

            public RememberMe getRememberMe() {
                return rememberMe;
            }

            public void setRememberMe(RememberMe rememberMe) {
                this.rememberMe = rememberMe;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public class FileSystem {
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

            public class RememberMe {
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
    }
}
