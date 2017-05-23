package com.ai.southernquiet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.nio.file.Files;

/**
 * framework模块依赖的外部配置。
 */
@ConfigurationProperties("framework")
public class FrameworkProperties {
    private FileSystem fileSystem = new FileSystem();

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
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
                if (!StringUtils.hasLength(workingRoot)) {
                    return SystemPropertyUtils.resolvePlaceholders("${user.home}/sq_filesystem");
                }

                return workingRoot;
            }

            public void setWorkingRoot(String workingRoot) {
                this.workingRoot = workingRoot;
            }
        }
    }
}
