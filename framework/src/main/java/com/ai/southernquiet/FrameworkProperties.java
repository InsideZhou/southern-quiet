package com.ai.southernquiet;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * framework模块依赖的外部配置。
 */
@ConfigurationProperties("framework")
public class FrameworkProperties {
    private FileSystem fileSystem;

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public static class FileSystem {
        private DefaultDriver defaultDriver;

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
}
