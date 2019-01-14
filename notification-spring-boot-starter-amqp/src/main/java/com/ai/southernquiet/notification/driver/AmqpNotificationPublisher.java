package com.ai.southernquiet.notification.driver;

import com.ai.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import com.ai.southernquiet.notification.AmqpNotificationAutoConfiguration;
import com.ai.southernquiet.notification.NotificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static com.ai.southernquiet.notification.AmqpNotificationAutoConfiguration.NAME_PREFIX;

@SuppressWarnings("WeakerAccess")
public class AmqpNotificationPublisher<N extends Serializable> implements NotificationPublisher<N> {
    private final static Logger log = LoggerFactory.getLogger(AmqpNotificationPublisher.class);

    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private MessageConverter messageConverter;
    private AmqpNotificationAutoConfiguration.Properties properties;

    private Set<String> declaredExchanges = new HashSet<>();

    private boolean enablePublisherConfirm;

    public AmqpNotificationPublisher(
        MessageConverter messageConverter,
        AmqpAdmin amqpAdmin,
        AmqpNotificationAutoConfiguration.Properties properties,
        RabbitProperties rabbitProperties,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
        boolean enablePublisherConfirm
    ) {
        this.amqpAdmin = amqpAdmin;
        this.messageConverter = messageConverter;
        this.properties = properties;
        this.enablePublisherConfirm = enablePublisherConfirm;

        ConnectionFactory connectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, connectionNameStrategy);
        ((CachingConnectionFactory) connectionFactory).setPublisherConfirms(enablePublisherConfirm);

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        if (enablePublisherConfirm) {
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (log.isDebugEnabled()) {
                    log.debug("接到publisher confirm: correlationData={}, ack={}, cause={}", correlationData, ack, cause);
                }
                else if (!ack) {
                    log.warn("通知发送确认失败: correlationData={}, cause={}", correlationData, cause);
                }
            });
        }
        this.rabbitTemplate = rabbitTemplate;
    }

    public MessageConverter getMessageConverter() {
        return messageConverter;
    }

    @Override
    public void publish(N notification, String source) {
        String exchange = getExchange(source);
        String routing = getRouting(source);

        declareExchange(exchange);

        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        };

        if (enablePublisherConfirm) {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                    exchange,
                    routing,
                    notification,
                    messagePostProcessor
                );

                rabbitTemplate.waitForConfirmsOrDie(properties.getPublisherConfirmTimeout());
                return null;
            });
        }
        else {
            rabbitTemplate.convertAndSend(
                exchange,
                routing,
                notification,
                messagePostProcessor
            );
        }
    }

    public String getExchange(String source) {
        return NAME_PREFIX + "EXCHANGE." + source;
    }

    public String getRouting(String source) {
        return NAME_PREFIX + source;
    }

    public Exchange declareExchange(String exchangeName) {
        if (declaredExchanges.contains(exchangeName)) return new FanoutExchange(exchangeName);

        Exchange exchange = new FanoutExchange(exchangeName);
        amqpAdmin.declareExchange(exchange);

        declaredExchanges.add(exchangeName);

        return exchange;
    }

    public Exchange declareExchange(Class<N> cls) {
        return declareExchange(getExchange(getNotificationSource(cls)));
    }
}
