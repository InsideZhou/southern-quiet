package com.ai.southernquiet.web.session.jetty;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class JettyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FileSessionDataStore sessionDataStore(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
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
}
