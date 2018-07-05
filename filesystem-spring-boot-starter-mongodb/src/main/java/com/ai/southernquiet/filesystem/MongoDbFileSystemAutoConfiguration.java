package com.ai.southernquiet.filesystem;

import com.ai.southernquiet.filesystem.driver.MongoDbFileSystem;
import com.mongodb.gridfs.GridFS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Component;

import java.io.IOException;

@SuppressWarnings({"SpringJavaAutowiringInspection", "SpringJavaInjectionPointsAutowiringInspection"})
@Configuration
@ConditionalOnBean({MongoOperations.class, MongoDbFactory.class, GridFsOperations.class, GridFS.class})
public class MongoDbFileSystemAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MongoDbFileSystem mongoDbFileSystem(Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) throws IOException {
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
    public static class Properties {
        /**
         * 路径集合
         */
        private String pathCollection = "PATH";
        /**
         * 文件大小阈值，大于该阈值的使用GridFs而不是普通Document。阈值上限是mongodb上限16m。
         */
        private Integer fileSizeThreshold = 15 * 1024 * 1024;

        public Integer getFileSizeThreshold() {
            return fileSizeThreshold;
        }

        public void setFileSizeThreshold(Integer fileSizeThreshold) {
            this.fileSizeThreshold = fileSizeThreshold;
        }

        public String getPathCollection() {
            return pathCollection;
        }

        public void setPathCollection(String pathCollection) {
            this.pathCollection = pathCollection;
        }
    }
}
