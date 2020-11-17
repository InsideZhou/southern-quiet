package me.insidezhou.southernquiet.amqp.rabbit;

import me.insidezhou.southernquiet.util.Metadata;
import org.springframework.amqp.core.MessageProperties;

import java.util.OptionalInt;
import java.util.stream.IntStream;

public class AmqpMessageConfigurer {
    private final AmqpAutoConfiguration.Properties properties;
    private final Metadata metadata;

    public AmqpMessageConfigurer(AmqpAutoConfiguration.Properties properties, Metadata metadata) {
        this.properties = properties;
        this.metadata = metadata;
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageProperties configureExpirationAndPriority(MessageProperties messageProperties, int expiration, int estimatedMaxExpiration) {
        messageProperties.setExpiration(String.valueOf(expiration));

        int levels = properties.getPriorityLevels(metadata);
        if (levels <= 1) return messageProperties;

        if (expiration <= 60 * 1000) {
            messageProperties.setPriority(levels);
            return messageProperties;
        }

        if (expiration <= 5 * 60 * 1000) {
            messageProperties.setPriority(levels - 1);
            return messageProperties;
        }

        if (expiration >= estimatedMaxExpiration) {
            messageProperties.setPriority(0);
            return messageProperties;
        }

        int avg = estimatedMaxExpiration / levels;
        OptionalInt optionalInt = IntStream.rangeClosed(0, levels).filter(i -> expiration <= i * avg).findFirst();

        messageProperties.setPriority(optionalInt.orElse(levels));
        return messageProperties;
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageProperties configureExpirationAndPriority(MessageProperties messageProperties, int expiration) {
        return configureExpirationAndPriority(messageProperties, expiration, (int) properties.getEstimatedMaxExpiration().toMillis());
    }
}
