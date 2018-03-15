package com.ai.southernquiet;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.FileSystemSupport;
import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
import com.ai.southernquiet.keyvalue.KeyValueStore;
import com.ai.southernquiet.keyvalue.driver.FileSystemKeyValueStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Configuration
public class FrameworkAutoConfiguration {
    @Bean
    @ConditionalOnProperty(value = "enable", prefix = "framework.key-value")
    @ConditionalOnMissingBean(KeyValueStore.class)
    public FileSystemKeyValueStore keyValueStore(KeyValueStoreProperties properties, FileSystem fileSystem) {
        return new FileSystemKeyValueStore(properties.getFileSystem(), fileSystem);
    }

    @Bean
    @ConditionalOnMissingBean(FileSystem.class)
    public LocalFileSystem fileSystem(LocalFileSystemProperties properties) {
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
                FileSystemSupport.setNamePattern(Pattern.compile(nameRegex));
            }
        }
    }

    @Component
    @ConfigurationProperties("framework.file-system.local")
    public static class LocalFileSystemProperties {
        /**
         * FileSystem默认驱动在本地文件系统中的实际路径
         */
        private String workingRoot = "${user.home}/sq_filesystem";

        public String getWorkingRoot() {
            return workingRoot;
        }

        public void setWorkingRoot(String workingRoot) {
            this.workingRoot = workingRoot;
        }
    }

    @Component
    @ConfigurationProperties("framework.key-value")
    public static class KeyValueStoreProperties {
        /**
         * 是否启用key-value特性
         */
        private boolean enable = true;

        private FileSystem fileSystem = new FileSystem();

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public FileSystem getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(FileSystem fileSystem) {
            this.fileSystem = fileSystem;
        }

        public static class FileSystem {
            /**
             * KeyValueStore在FileSystem中的路径
             */
            private String workingRoot = "KEY_VALUE";
            /**
             * 文件名中不同部分的分隔
             */
            private String nameSeparator = "__";

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
}
