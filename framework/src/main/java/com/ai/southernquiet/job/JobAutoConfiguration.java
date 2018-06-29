package com.ai.southernquiet.job;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.job.driver.FileSystemJobQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class JobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(JobQueue.class)
    public FileSystemJobQueue jobQueue(FileSystem fileSystem, JobAutoConfiguration.Properties properties) {
        return new FileSystemJobQueue(fileSystem, properties);
    }

    @Component
    @ConfigurationProperties("framework.job")
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
