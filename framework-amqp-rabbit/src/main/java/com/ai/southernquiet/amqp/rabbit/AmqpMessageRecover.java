package com.ai.southernquiet.amqp.rabbit;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.util.StringUtils;

public class AmqpMessageRecover extends RepublishMessageRecoverer {
    private AmqpAutoConfiguration.Properties properties;

    public AmqpMessageRecover(AmqpTemplate amqpTemplate,
                              String errorExchange,
                              String errorRoutingKey,
                              AmqpAutoConfiguration.Properties properties) {

        super(amqpTemplate, errorExchange, errorRoutingKey);

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
