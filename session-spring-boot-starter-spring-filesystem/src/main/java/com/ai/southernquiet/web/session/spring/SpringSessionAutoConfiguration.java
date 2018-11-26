package com.ai.southernquiet.web.session.spring;

import com.ai.southernquiet.filesystem.FileSystem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties
public class SpringSessionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FileSessionRepository fileSessionRepository(FileSystem fileSystem, FileSessionProperties properties) {
        return new FileSessionRepository(fileSystem, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("web.session.file-system")
    public FileSessionProperties springFileSessionProperties() {
        return new FileSessionProperties();
    }

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
