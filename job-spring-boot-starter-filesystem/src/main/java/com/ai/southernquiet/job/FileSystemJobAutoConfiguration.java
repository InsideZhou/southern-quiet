package com.ai.southernquiet.job;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.job.driver.FileJobQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@ConditionalOnBean(FileSystem.class)
@EnableConfigurationProperties(FileSystemJobAutoConfiguration.Properties.class)
public class FileSystemJobAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public FileJobQueue fileJobQueue(FileSystem fileSystem, Properties properties, List<JobHandler<?>> jobHandlerList) {
        return new FileJobQueue(fileSystem, jobHandlerList, properties);
    }

    @ConfigurationProperties("framework.job.file-system")
    public static class Properties {
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
