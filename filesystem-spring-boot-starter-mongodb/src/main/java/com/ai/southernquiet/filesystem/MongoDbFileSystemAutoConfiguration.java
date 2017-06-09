package com.ai.southernquiet.filesystem;

import com.ai.southernquiet.filesystem.driver.MongoDbFileSystem;
import com.mongodb.gridfs.GridFS;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Configuration
@AutoConfigureAfter(MongoDataAutoConfiguration.class)
public class MongoDbFileSystemAutoConfiguration {
    @Bean
    public MongoDbFileSystem fileSystem(Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) throws IOException {
        return new MongoDbFileSystem(properties, mongoOperations, gridFsOperations, gridFS);
    }

    @Bean
    @ConditionalOnMissingBean(GridFS.class)
    public GridFS gridFS(MongoDbFactory factory) {
        return new GridFS(factory.getLegacyDb());
    }

    /**
     * @see org.springframework.boot.autoconfigure.mongo.MongoProperties
     */
    @Component
    @ConfigurationProperties("framework.file-system.mongodb")
    @ConditionalOnBean(MongoDbFileSystem.class)
    public class Properties {
        /**
         * 文件集合
         */
        private String fileCollection;
        /**
         * 目录集合
         */
        private String directoryCollection;

        public String getDirectoryCollection() {
            return directoryCollection;
        }

        public void setDirectoryCollection(String directoryCollection) {
            this.directoryCollection = directoryCollection;
        }

        public String getFileCollection() {
            return fileCollection;
        }

        public void setFileCollection(String fileCollection) {
            this.fileCollection = fileCollection;
        }
    }
}