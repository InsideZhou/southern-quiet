package com.ai.southernquiet;

import com.ai.southernquiet.cache.Cache;
import com.ai.southernquiet.cache.driver.FileSystemCache;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.FileSystemHelper;
import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.regex.Pattern;

@Configuration
public class FrameworkAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public FileSystemCache cache(FileSystemCacheProperties properties, FileSystem fileSystem) {
        return new FileSystemCache(properties, fileSystem);
    }

    @Bean
    @ConditionalOnMissingBean(FileSystem.class)
    public LocalFileSystem fileSystem(LocalFileSystemProperties properties) throws IOException {
        return new LocalFileSystem(properties);
    }

    @Component
    @ConfigurationProperties("framework.file-system")
    public static class FileSystemProperties {
        /**
         * FileSystem中合法文件名的正则表达式
         */
        private String nameRegex;

        public String getNameRegex() {
            return nameRegex;
        }

        public void setNameRegex(String nameRegex) {
            this.nameRegex = nameRegex;

            if (StringUtils.hasText(nameRegex)) {
                FileSystemHelper.setNamePattern(Pattern.compile(nameRegex));
            }
        }
    }

    @Component
    @ConfigurationProperties("framework.file-system.local")
    public static class LocalFileSystemProperties {
        /**
         * FileSystem默认驱动在本地文件系统中的实际路径
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
    @ConfigurationProperties("framework.cache.file-system")
    public class FileSystemCacheProperties {
        /**
         * Cache在FileSystem中的路径
         */
        private String workingRoot;
        /**
         * 文件名中不同部分的分隔
         */
        private String nameSeparator;

        public String getNameSeparator() {
            return nameSeparator;
        }

        public void setNameSeparator(String nameSeparator) {
            this.nameSeparator = nameSeparator;
        }

        public String getWorkingRoot() {
            return workingRoot;
        }

        public void setWorkingRoot(String workingRoot) {
            this.workingRoot = workingRoot;
        }
    }
}
