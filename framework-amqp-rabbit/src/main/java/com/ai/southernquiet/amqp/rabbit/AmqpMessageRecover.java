package com.ai.southernquiet.amqp.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.util.StringUtils;

public class AmqpMessageRecover extends RepublishMessageRecoverer {
    private final static Logger log = LoggerFactory.getLogger(AmqpMessageRecover.class);

    private AmqpAutoConfiguration.Properties properties;

    public AmqpMessageRecover(AmqpTemplate amqpTemplate,
                              String errorExchange,
                              String errorRoutingKey,
                              AmqpAutoConfiguration.Properties properties) {

        super(amqpTemplate, errorExchange, errorRoutingKey);
        setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        this.properties = properties;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        MessageProperties messageProperties = message.getMessageProperties();

        long expiration = (long) messageProperties.getHeaders().compute("original-expiration", (key, value) -> {
            if (StringUtils.isEmpty(value)) {
                return properties.getInitialExpiration().toMillis();
            }

            long expiry = (long) value;
            return expiry + (long) Math.pow(expiry, properties.getPower());
        });

        if (null == messageProperties.getDeliveryMode()) {
            messageProperties.setDeliveryMode(getDeliveryMode());
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "准备把消息送进死信队列: expiration/ttl={}/{}, deliveryMode={}, message={}",
                expiration,
                properties.getExpiration().toMillis(),
                messageProperties.getDeliveryMode(),
                message,
                cause
            );
        }

        if (expiration < properties.getExpiration().toMillis()) {
            messageProperties.setExpiration(String.valueOf(expiration));
            errorTemplate.send(errorExchangeName, errorRoutingKey, message);
        }
        else {
            messageProperties.setExpiration(null);
            super.recover(message, cause);
        }
    }
}
