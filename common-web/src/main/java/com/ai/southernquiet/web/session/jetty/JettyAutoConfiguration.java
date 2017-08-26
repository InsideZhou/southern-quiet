package com.ai.southernquiet.web.session.jetty;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.SessionRepository;

@Configuration
@ConditionalOnClass(Server.class)
public class JettyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean({SessionDataStore.class, SessionRepository.class})
    public FileSessionDataStore sessionDataStore(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
        return new FileSessionDataStore(fileSystem, properties);
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
}
