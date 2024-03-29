package me.insidezhou.southernquiet;

import me.insidezhou.southernquiet.debounce.DebouncerProvider;
import me.insidezhou.southernquiet.debounce.DefaultDebouncerProvider;
import me.insidezhou.southernquiet.event.EventPubSub;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.driver.LocalFileSystem;
import me.insidezhou.southernquiet.keyvalue.driver.FileSystemKeyValueStore;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.throttle.DefaultThrottleManager;
import me.insidezhou.southernquiet.throttle.ThrottleManager;
import me.insidezhou.southernquiet.util.AsyncRunner;
import me.insidezhou.southernquiet.util.Metadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import static me.insidezhou.southernquiet.auth.AuthAdvice.AuthorizationMatcherQualifier;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
@ImportAutoConfiguration(TaskSchedulingAutoConfiguration.class)
@ComponentScan
public class FrameworkAutoConfiguration {
    private final static SouthernQuietLogger logger = SouthernQuietLoggerFactory.getLogger(FrameworkAutoConfiguration.class);

    public final static String ConfigRoot = "southern-quiet.framework";
    public final static String ConfigRoot_Auth = ConfigRoot + ".auth";
    public final static String ConfigRoot_Debounce = ConfigRoot + ".debounce";
    public final static String ConfigRoot_Throttle = ConfigRoot + ".throttle";
    public final static String ConfigRoot_Event = ConfigRoot + ".event";
    public final static String ConfigRoot_FileSystem = ConfigRoot + ".file-system";
    public final static String ConfigRoot_KeyValue = ConfigRoot + ".key-value";

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_KeyValue)
    @ConditionalOnMissingBean
    public FileSystemKeyValueStore keyValueStore(KeyValueStoreProperties properties, FileSystem fileSystem) {
        return new FileSystemKeyValueStore(properties.getFileSystem(), fileSystem);
    }

    @Bean
    @ConditionalOnMissingBean(FileSystem.class)
    public LocalFileSystem localFileSystem(LocalFileSystemProperties properties) {
        return new LocalFileSystem(properties);
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
    @Qualifier(AuthorizationMatcherQualifier)
    public AntPathMatcher authorizationMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Debounce, matchIfMissing = true)
    @ConditionalOnMissingBean(DebouncerProvider.class)
    public DefaultDebouncerProvider defaultDebouncerProvider(DebounceProperties debounceProperties, TaskScheduler taskScheduler, Metadata metadata) {
        return new DefaultDebouncerProvider(debounceProperties, taskScheduler, metadata);
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
    @ConditionalOnMissingBean(ThrottleManager.class)
    public DefaultThrottleManager defaultThrottleManager() {
        return new DefaultThrottleManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public Metadata metadata(Properties properties) {
        Metadata meta = new Metadata() {
            final private int coreNumber = Runtime.getRuntime().availableProcessors();

            @Override
            public String getRuntimeId() {
                return StringUtils.hasText(properties.getRuntimeId()) ? properties.getRuntimeId() : getIPWithProcessId();
            }

            @Override
            public int getCoreNumber() {
                return coreNumber > 0 ? coreNumber : 1;
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

        logger.message("SouthernQuiet Metadata 已生成")
            .context("coreNumber", meta.getCoreNumber())
            .context("runtimeId", meta.getRuntimeId())
            .debug();

        return meta;
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncRunner asyncRunner() {
        return new AsyncRunner();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(ConfigRoot)
    public Properties frameworkProperties() {
        return new Properties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(ConfigRoot_Debounce)
    public DebounceProperties debounceProperties() {
        return new DebounceProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(ConfigRoot_Event)
    public EventProperties eventProperties() {
        return new EventProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(ConfigRoot_FileSystem + ".local")
    public LocalFileSystemProperties localFileSystemProperties() {
        return new LocalFileSystemProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(ConfigRoot_KeyValue)
    public KeyValueStoreProperties keyValueStoreProperties() {
        return new KeyValueStoreProperties();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Properties {
        /**
         * 框架运行时的id，必须唯一。
         */
        private String runtimeId;

        private Auth auth;

        private Throttle throttle;

        public String getRuntimeId() {
            return runtimeId;
        }

        public void setRuntimeId(String runtimeId) {
            this.runtimeId = runtimeId;
        }

        public Auth getAuth() {
            return auth;
        }

        public void setAuth(Auth auth) {
            this.auth = auth;
        }

        public Throttle getThrottle() {
            return throttle;
        }

        public void setThrottle(Throttle throttle) {
            this.throttle = throttle;
        }

        public static class Auth {
            private boolean enable = true;

            public boolean isEnable() {
                return enable;
            }

            public void setEnable(boolean enable) {
                this.enable = enable;
            }
        }

        public static class Throttle {
            private boolean enable = true;

            public boolean isEnable() {
                return enable;
            }

            public void setEnable(boolean enable) {
                this.enable = enable;
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class DebounceProperties {
        private boolean enable = true;
        /**
         * 多长时间上报一次检查及执行计数。
         */
        private Duration reportDuration = Duration.ofMinutes(1);

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public Duration getReportDuration() {
            return reportDuration;
        }

        public void setReportDuration(Duration reportDuration) {
            this.reportDuration = reportDuration;
        }
    }

    public static class EventProperties {
        private String[] defaultChannels = new String[]{EventPubSub.DefaultEventChannel};

        public String[] getDefaultChannels() {
            return defaultChannels;
        }

        public void setDefaultChannels(String[] defaultChannels) {
            this.defaultChannels = defaultChannels;
        }
    }

    public static class LocalFileSystemProperties {
        /**
         * FileSystem默认驱动在本地文件系统中的实际路径
         */
        private String workingRoot = "${user.home}/.SQ_FILESYSTEM";

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
