package com.ai.southernquiet;

import com.ai.southernquiet.cache.Cache;
import com.ai.southernquiet.cache.driver.FileSystemCache;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties
public class FrameworkAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public Cache cache(FrameworkProperties properties, FileSystem fileSystem) {
        return new FileSystemCache(properties, fileSystem);
    }

    @Bean
    @ConditionalOnMissingBean(FileSystem.class)
    public FileSystem fileSystem(FrameworkProperties properties) throws IOException {
        return new LocalFileSystem(properties);
    }
}
