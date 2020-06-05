package me.insidezhou.southernquiet.amqp.rabbit;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.Amplifier;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AmqpMessageRecover extends RepublishMessageRecoverer {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpMessageRecover.class);

    private final Amplifier amplifier;
    private final long maxExpiration;

    private final String retryExchange;
    private final String retryRoutingKey;

    public AmqpMessageRecover(AmqpTemplate amqpTemplate,
                              Amplifier amplifier,
                              String errorExchange,
                              String errorRoutingKey,
                              String retryExchange,
                              String retryRoutingKey,
                              AmqpAutoConfiguration.Properties properties) {

        super(amqpTemplate, errorExchange, errorRoutingKey);
        setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        this.retryExchange = retryExchange;
        this.retryRoutingKey = retryRoutingKey;

        this.maxExpiration = properties.getExpiration().toMillis();
        this.amplifier = amplifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void recover(Message message, Throwable cause) {
        MessageProperties messageProperties = message.getMessageProperties();

        if (!messageProperties.isRedelivered()) {
            throw new ImmediateRequeueAmqpException(cause);
        }

        Map<String, Object> headers = messageProperties.getHeaders();
        Map<String, Object> xDeath = ((List<Map<String, Object>>) headers.getOrDefault("x-death", Collections.singletonList(Collections.emptyMap()))).get(0);

        long recoverCount = ((Number) xDeath.getOrDefault("count", 0)).longValue();
        long expiration = amplifier.amplify(recoverCount);

        if (null == messageProperties.getDeliveryMode()) {
            messageProperties.setDeliveryMode(getDeliveryMode());
        }

        Consumer<Map<String, Object>> consumer = context -> {
            context.put("deadExchange", errorExchangeName);
            context.put("deadQueue", errorRoutingKey);
            context.put("retryExchange", retryExchange);
            context.put("retryQueue", retryRoutingKey);
            context.put("expiration", expiration);
            context.put("recoverCount", recoverCount);
            context.put("deliveryMode", messageProperties.getDeliveryMode());
            context.put("message", message);
            context.put("cause", cause);
        };

        if (expiration < maxExpiration) {
            log.message("准备把消息送进重试队列").context(consumer).debug();

            messageProperties.setExpiration(String.valueOf(expiration));
            errorTemplate.send(retryExchange, retryRoutingKey, message);
        }
        else {
            log.message("准备把消息送进死信队列").context(consumer).warn();

            messageProperties.setExpiration(null);
            super.recover(message, cause);
        }
    }
}
