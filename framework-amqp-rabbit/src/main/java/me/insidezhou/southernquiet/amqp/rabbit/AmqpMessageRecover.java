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

public class AmqpMessageRecover extends RepublishMessageRecoverer {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpMessageRecover.class);

    private final Amplifier amplifier;
    private final long maxExpiration;

    public AmqpMessageRecover(AmqpTemplate amqpTemplate,
                              Amplifier amplifier,
                              String errorExchange,
                              String errorRoutingKey,
                              AmqpAutoConfiguration.Properties properties) {

        super(amqpTemplate, errorExchange, errorRoutingKey);
        setDeliveryMode(MessageDeliveryMode.PERSISTENT);

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

        log.message("准备把消息送进死信队列")
            .context(context -> {
                context.put("exchange", errorExchangeName);
                context.put("queue", errorRoutingKey);
                context.put("expiration", expiration);
                context.put("recoverCount", recoverCount);
                context.put("deliveryMode", messageProperties.getDeliveryMode());
                context.put("message", message);
                context.put("cause", cause);
            })
            .debug();

        if (expiration < maxExpiration) {
            messageProperties.setExpiration(String.valueOf(expiration));
            errorTemplate.send(errorExchangeName, errorRoutingKey, message);
        }
        else {
            messageProperties.setExpiration(null);
            super.recover(message, cause);
        }
    }
}
