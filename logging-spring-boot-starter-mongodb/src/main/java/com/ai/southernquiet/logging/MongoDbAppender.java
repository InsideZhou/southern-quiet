package com.ai.southernquiet.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.StringUtils;

public class MongoDbAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private String logCollection = "LOG";
    private MongoOperations mongoOperations;

    public MongoDbAppender(MongoDbLoggingAutoConfiguration.Properties properties, MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;

        String logCollection = properties.getCollection();
        if (StringUtils.hasText(logCollection)) {
            this.logCollection = logCollection;
        }

        if (!mongoOperations.collectionExists(this.logCollection)) {
            mongoOperations.createCollection(this.logCollection);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        mongoOperations.insert(new MongoLoggingEvent(eventObject), logCollection);
    }
}
