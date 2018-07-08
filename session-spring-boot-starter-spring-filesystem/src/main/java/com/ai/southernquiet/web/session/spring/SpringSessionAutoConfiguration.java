package com.ai.southernquiet.web.session.spring;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class SpringSessionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FileSessionRepository fileSessionRepository(FileSystem fileSystem, CommonWebAutoConfiguration.FileSessionProperties properties) {
        return new FileSessionRepository(fileSystem, properties);
    }
}
