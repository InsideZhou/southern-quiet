package com.ai.southernquiet.web.session.jetty;

import com.ai.southernquiet.filesystem.FileSystem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties
public class JettyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FileSessionDataStore sessionDataStore(FileSystem fileSystem, FileSessionProperties properties) {
        return new FileSessionDataStore(fileSystem, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JettyConfiguration jettyConfiguration(FileSessionDataStore fileSessionDataStore) {
        return new JettyConfiguration(fileSessionDataStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public JettyServletWebServerFactory jettyServletWebServerFactory(JettyConfiguration jettyConfiguration) {
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
        factory.addConfigurations(jettyConfiguration);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public FileSessionProperties fileSessionProperties() {
        return new FileSessionProperties();
    }

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
}
