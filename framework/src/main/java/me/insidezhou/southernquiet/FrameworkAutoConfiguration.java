package me.insidezhou.southernquiet;

import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.FileSystemSupport;
import me.insidezhou.southernquiet.filesystem.driver.LocalFileSystem;
import me.insidezhou.southernquiet.keyvalue.KeyValueStore;
import me.insidezhou.southernquiet.keyvalue.driver.FileSystemKeyValueStore;
import me.insidezhou.southernquiet.util.AsyncRunner;
import me.insidezhou.southernquiet.util.Metadata;
import me.insidezhou.southernquiet.event.EventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Configuration
@EnableAsync
@EnableConfigurationProperties
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

    @Bean
    @ConditionalOnMissingBean
    public AsyncRunner asyncRunner() {
        return new AsyncRunner();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework")
    public Properties frameworkProperties() {
        return new Properties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.event")
    public EventProperties eventProperties() {
        return new EventProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.file-system")
    public FileSystemProperties fileSystemProperties() {
        return new FileSystemProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.file-system.local")
    public LocalFileSystemProperties localFileSystemProperties() {
        return new LocalFileSystemProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.key-value")
    public KeyValueStoreProperties keyValueStoreProperties() {
        return new KeyValueStoreProperties();
    }

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

    public static class EventProperties {
        private String[] defaultChannels = new String[]{EventPublisher.DefaultEventChannel};

        public String[] getDefaultChannels() {
            return defaultChannels;
        }

        public void setDefaultChannels(String[] defaultChannels) {
            this.defaultChannels = defaultChannels;
        }
    }

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

    public static class KeyValueStoreProperties {
        /**
         * 是否启用key-value特性
         */
        private boolean enable = false;

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
