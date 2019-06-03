package me.insidezhou.southernquiet.amqp.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;

public class AmqpMessageRecover extends RepublishMessageRecoverer {
    private final static Logger log = LoggerFactory.getLogger(AmqpMessageRecover.class);

    private AmqpAutoConfiguration.Properties properties;

    private AmqpAdmin amqpAdmin;

    public AmqpMessageRecover(AmqpTemplate amqpTemplate,
                              AmqpAdmin amqpAdmin,
                              String errorExchange,
                              String errorRoutingKey,
                              AmqpAutoConfiguration.Properties properties) {

        super(amqpTemplate, errorExchange, errorRoutingKey);
        setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        this.properties = properties;
        this.amqpAdmin = amqpAdmin;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void recover(Message message, Throwable cause) {
        MessageProperties messageProperties = message.getMessageProperties();
        Map<String, Object> headers = messageProperties.getHeaders();
        headers.putIfAbsent("x-recover-count", 0);
        headers.putIfAbsent("x-expiration", properties.getInitialExpiration().toMillis());

        int queuedMessageCount = 0;
        int recoverCount = (int) headers.get("x-recover-count");

        long expiration;
        if (StringUtils.isEmpty(messageProperties.getExpiration())) {
            expiration = (long) headers.get("x-expiration");
        }
        else {
            expiration = Long.parseLong(messageProperties.getExpiration());
        }

        if (recoverCount > 0) {
            if (null != messageProperties.getMessageCount()) {
                queuedMessageCount = messageProperties.getMessageCount();
            }
            else {
                Properties queueProperties = amqpAdmin.getQueueProperties(messageProperties.getConsumerQueue());
                queuedMessageCount = (int) queueProperties.getOrDefault("QUEUE_MESSAGE_COUNT", 0);
            }

            expiration += (long) Math.pow(expiration + queuedMessageCount * recoverCount, properties.getPower());
        }

        messageProperties.setHeader("x-recover-count", ++recoverCount);
        messageProperties.setHeader("x-expiration", expiration);

        if (null == messageProperties.getDeliveryMode()) {
            messageProperties.setDeliveryMode(getDeliveryMode());
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "准备把通知送进死信队列: exchange={}, queue={}, expiration={}/{}, recoverCount={}, deliveryMode={}, messageCount={}, message={}",
                errorExchangeName,
                errorRoutingKey,
                expiration,
                properties.getExpiration().toMillis(),
                recoverCount,
                messageProperties.getDeliveryMode(),
                queuedMessageCount,
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
