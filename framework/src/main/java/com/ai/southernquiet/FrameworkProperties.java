package com.ai.southernquiet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * framework模块依赖的外部配置。
 */
@Component
@ConfigurationProperties("framework")
public class FrameworkProperties {
    private FileSystem fileSystem = new FileSystem();
    private Cache cache = new Cache();
    private Job job = new Job();

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

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

    public static class Job {
        private Integer retryLimit;

        public Integer getRetryLimit() {
            return retryLimit;
        }

        public void setRetryLimit(Integer retryLimit) {
            this.retryLimit = retryLimit;
        }
    }

    public static class FileSystem {
        private DefaultDriver defaultDriver = new DefaultDriver();

        public DefaultDriver getDefaultDriver() {
            return defaultDriver;
        }

        public void setDefaultDriver(DefaultDriver defaultDriver) {
            this.defaultDriver = defaultDriver;
        }

        public static class DefaultDriver {
            private String workingRoot;

            public String getWorkingRoot() {
                return workingRoot;
            }

            public void setWorkingRoot(String workingRoot) {
                this.workingRoot = workingRoot;
            }
        }
    }

    public static class Cache {
        private FileSystem fileSystem = new FileSystem();

        public FileSystem getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(FileSystem fileSystem) {
            this.fileSystem = fileSystem;
        }

        public static class FileSystem {
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
