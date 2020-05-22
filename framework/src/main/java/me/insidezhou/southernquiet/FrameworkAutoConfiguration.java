package me.insidezhou.southernquiet;

import me.insidezhou.southernquiet.auth.AuthAdvice;
import me.insidezhou.southernquiet.auth.AuthPointcut;
import me.insidezhou.southernquiet.event.EventPubSub;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.driver.LocalFileSystem;
import me.insidezhou.southernquiet.keyvalue.KeyValueStore;
import me.insidezhou.southernquiet.keyvalue.driver.FileSystemKeyValueStore;
import me.insidezhou.southernquiet.throttle.DefaultThrottleManager;
import me.insidezhou.southernquiet.throttle.ThrottleAdvice;
import me.insidezhou.southernquiet.throttle.ThrottleManager;
import me.insidezhou.southernquiet.throttle.ThrottlePointcut;
import me.insidezhou.southernquiet.util.AsyncRunner;
import me.insidezhou.southernquiet.util.Metadata;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static me.insidezhou.southernquiet.auth.AuthAdvice.AuthorizationMatcherQualifier;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class FrameworkAutoConfiguration {
    public final static String ConfigRoot = "southern-quiet.framework";
    public final static String ConfigRoot_Auth = ConfigRoot + ".auth";
    public final static String ConfigRoot_Throttle = ConfigRoot + ".throttle";
    public final static String ConfigRoot_Event = ConfigRoot + ".event";
    public final static String ConfigRoot_FileSystem = ConfigRoot + ".file-system";
    public final static String ConfigRoot_KeyValue = ConfigRoot + ".key-value";

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_KeyValue)
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
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
    @ConditionalOnMissingBean
    public AuthAdvice authAdvice(@Qualifier(AuthorizationMatcherQualifier) PathMatcher pathMatcher) {
        return new AuthAdvice(pathMatcher);
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
    @ConditionalOnMissingBean
    public AuthPointcut authPointcut() {
        return new AuthPointcut();
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
    @Qualifier(AuthorizationMatcherQualifier)
    public AntPathMatcher authorizationMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
    @ConditionalOnBean({AuthAdvice.class, AuthPointcut.class})
    public AnnotationAdvisingBeanPostProcessor authAnnotationAdvisingBeanPostProcessor(AuthAdvice authAdvice, AuthPointcut authPointcut) {
        return new AnnotationAdvisingBeanPostProcessor(new DefaultPointcutAdvisor(authPointcut, authAdvice)) {};
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
    @ConditionalOnMissingBean
    public ThrottlePointcut throttlePointcut() {
        return new ThrottlePointcut();
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
    @ConditionalOnMissingBean
    public ThrottleAdvice throttleAdvice(ThrottleManager throttleManager) {
        return new ThrottleAdvice(throttleManager);
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
    @ConditionalOnMissingBean
    public ThrottleManager throttleManager() {
        return new DefaultThrottleManager();
    }

    @Bean
    @ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
    @ConditionalOnBean({ThrottleAdvice.class, ThrottlePointcut.class})
    public AnnotationAdvisingBeanPostProcessor throttleAnnotationAdvisingBeanPostProcessor(ThrottleAdvice advice, ThrottlePointcut pointcut) {
        return new AnnotationAdvisingBeanPostProcessor(new DefaultPointcutAdvisor(pointcut, advice)) {};
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
    @ConfigurationProperties(ConfigRoot)
    public Properties frameworkProperties() {
        return new Properties();
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

        public String getRuntimeId() {
            return runtimeId;
        }

        public void setRuntimeId(String runtimeId) {
            this.runtimeId = runtimeId;
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
