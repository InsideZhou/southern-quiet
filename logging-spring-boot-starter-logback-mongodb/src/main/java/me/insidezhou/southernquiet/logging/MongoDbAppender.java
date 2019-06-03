package me.insidezhou.southernquiet.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.springframework.data.mongodb.core.MongoOperations;

public class MongoDbAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private String logCollection;
    private MongoOperations mongoOperations;

    public MongoDbAppender(MongoDbLoggingAutoConfiguration.Properties properties, MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
        this.logCollection = properties.getCollection();

        if (!mongoOperations.collectionExists(this.logCollection)) {
            mongoOperations.createCollection(this.logCollection);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        mongoOperations.insert(new MongoLoggingEvent(eventObject), logCollection);
    }
}
