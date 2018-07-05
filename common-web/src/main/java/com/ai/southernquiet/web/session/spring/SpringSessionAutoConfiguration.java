package com.ai.southernquiet.web.session.spring;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.Session;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@ConditionalOnClass(Session.class)
public class SpringSessionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FileSessionRepository fileSessionRepository(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
        return new FileSessionRepository(fileSystem, properties);
    }
}
