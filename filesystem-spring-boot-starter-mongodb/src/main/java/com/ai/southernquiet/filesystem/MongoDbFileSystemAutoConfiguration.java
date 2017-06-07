package com.ai.southernquiet.filesystem;

import com.ai.southernquiet.FrameworkProperties;
import com.ai.southernquiet.filesystem.driver.MongoDbFileSystem;
import com.mongodb.gridfs.GridFS;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

import java.io.IOException;

@Configuration
@EnableAutoConfiguration
public class MongoDbFileSystemAutoConfiguration {
    @Bean
    public FileSystem fileSystem(FrameworkProperties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) throws IOException {
        return new MongoDbFileSystem(properties, mongoOperations, gridFsOperations, gridFS);
    }

    @Bean
    @ConditionalOnMissingBean(GridFS.class)
    public GridFS gridFS(MongoDbFactory factory) {
        return new GridFS(factory.getLegacyDb());
    }
}
