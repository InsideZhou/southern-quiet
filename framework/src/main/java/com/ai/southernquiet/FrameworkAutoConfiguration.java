package com.ai.southernquiet;

import com.ai.southernquiet.cache.Cache;
import com.ai.southernquiet.cache.driver.FileSystemCache;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Configuration
public class FrameworkAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public FileSystemCache cache(Properties properties, FileSystem fileSystem) {
        return new FileSystemCache(properties, fileSystem);
    }

    @Bean
    @ConditionalOnMissingBean(FileSystem.class)
    public LocalFileSystem fileSystem(Properties properties) throws IOException {
        return new LocalFileSystem(properties);
    }

    @Component
    @ConfigurationProperties("framework")
    public static class Properties {
        private FileSystem fileSystem = new FileSystem();
        private Cache cache = new Cache();

        public Cache getCache() {
            return cache;
        }

        public void setCache(Cache cache) {
            this.cache = cache;
        }

        public FileSystem getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(FileSystem fileSystem) {
            this.fileSystem = fileSystem;
        }

        public class FileSystem {
            private Local local = new Local();

            public Local getLocal() {
                return local;
            }

            public void setLocal(Local local) {
                this.local = local;
            }

            public class Local {
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
        }

        public class Cache {
            private FileSystem fileSystem = new FileSystem();

            public FileSystem getFileSystem() {
                return fileSystem;
            }

            public void setFileSystem(FileSystem fileSystem) {
                this.fileSystem = fileSystem;
            }

            public class FileSystem {
                /**
                 * Cache在FileSystem中的路径
                 */
                private String workingRoot;
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
    }
}
