package com.ai.southernquiet.job;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties(FileSystemJobAutoConfiguration.Properties.class)
public class FileSystemJobAutoConfiguration {
    @ConfigurationProperties("framework.job.filesystem")
    public static class Properties {
        private FileSystem fileSystem = new FileSystem();

        public FileSystem getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(FileSystem fileSystem) {
            this.fileSystem = fileSystem;
        }

        public class FileSystem {
            /**
             * 任务队列持久化在FileSystem中的路径
             */
            private String workingRoot = "JOB_QUEUE";

            public String getWorkingRoot() {
                return workingRoot;
            }

            public void setWorkingRoot(String workingRoot) {
                this.workingRoot = workingRoot;
            }
        }
    }
}
