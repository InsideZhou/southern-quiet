package com.ai.southernquiet;

import com.ai.southernquiet.broadcasting.Broadcaster;
import com.ai.southernquiet.broadcasting.Publisher;
import com.ai.southernquiet.broadcasting.driver.DefaultPublisher;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.FileSystemSupport;
import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
import com.ai.southernquiet.keyvalue.KeyValueStore;
import com.ai.southernquiet.keyvalue.driver.FileSystemKeyValueStore;
import com.ai.southernquiet.util.Metadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties({
    FrameworkAutoConfiguration.Properties.class,
    FrameworkAutoConfiguration.BroadcastingProperties.class,
    FrameworkAutoConfiguration.FileSystemProperties.class,
    FrameworkAutoConfiguration.LocalFileSystemProperties.class,
    FrameworkAutoConfiguration.KeyValueStoreProperties.class
})
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

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean(Publisher.class)
    public DefaultPublisher publisher(Broadcaster<?> broadcaster, BroadcastingProperties properties) {
        return new DefaultPublisher(broadcaster, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public Metadata metadata(Properties properties) {
        return new Metadata() {
            @Override
            public String getRuntimeId() {
                return StringUtils.hasText(properties.getRuntimeId()) ? properties.getRuntimeId() : getIPWithProcessId();
            }

            private String getIPWithProcessId() {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();

                try {
                    return jvmName + "/" + InetAddress.getLocalHost().getHostAddress();
                }
                catch (UnknownHostException e) {
                    return jvmName;
                }
            }
        };
    }

    @ConfigurationProperties("framework")
    public static class Properties {
        /**
         * 框架运行时的id，必须唯一。
         */
        private String runtimeId;

        public String getRuntimeId() {
            return runtimeId;
        }

        public void setRuntimeId(String runtimeId) {
            this.runtimeId = runtimeId;
        }
    }

    @ConfigurationProperties("framework.broadcasting")
    public static class BroadcastingProperties {
        private String[] defaultChannels = new String[]{"public"};

        public String[] getDefaultChannels() {
            return defaultChannels;
        }

        public void setDefaultChannels(String[] defaultChannels) {
            this.defaultChannels = defaultChannels;
        }
    }

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
