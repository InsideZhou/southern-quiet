package com.ai.southernquiet.web;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class CommonWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(SessionDataStore.class)
    public SessionDataStore sessionDataStore(FileSystem fileSystem, CommonWebProperties properties) {
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
}
