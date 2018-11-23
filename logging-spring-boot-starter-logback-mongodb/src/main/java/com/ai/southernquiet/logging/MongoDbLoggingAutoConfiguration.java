package com.ai.southernquiet.logging;

import com.mongodb.MongoClientURI;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.util.StringUtils;

@SuppressWarnings({"SpringJavaAutowiringInspection", "SpringJavaInjectionPointsAutowiringInspection"})
@Configuration
@AutoConfigureAfter(MongoDataAutoConfiguration.class)
@EnableConfigurationProperties
public class MongoDbLoggingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MongoDbAppender mongoDbAppender(Properties properties, MongoOperations mongoOperations) {
        String uri = properties.getUri();
        if (StringUtils.hasText(uri)) {
            MongoDbFactory factory = new SimpleMongoDbFactory(new MongoClientURI(uri));
            mongoOperations = new MongoTemplate(factory);
        }

        return new MongoDbAppender(properties, mongoOperations);
    }

    @Bean
    @ConditionalOnMissingBean
    public Properties properties() {
        return new Properties();
    }

    @ConfigurationProperties("southern-quiet.framework.logging.mongodb")
    public static class Properties {
        /**
         * 日志集合
         */
        private String collection = "LOG";
        /**
         * 数据库连接uri，如果使用了这个uri，则使用独立的MongoOperation实例连接独立的数据源，忽略全局bean定义。
         */
        private String uri;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }
}
