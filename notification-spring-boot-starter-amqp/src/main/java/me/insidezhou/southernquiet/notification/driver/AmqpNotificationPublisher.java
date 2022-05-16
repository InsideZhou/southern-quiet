package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.amqp.rabbit.AbstractAmqpNotificationPublisher;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.CachingConnectionFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.context.Lifecycle;

@SuppressWarnings("WeakerAccess")
public class AmqpNotificationPublisher<N> extends AbstractAmqpNotificationPublisher<N> implements Lifecycle {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final SmartMessageConverter messageConverter;
    private final AmqpNotificationAutoConfiguration.Properties notificationProperties;
    private final AmqpAutoConfiguration.Properties properties;

    private final boolean enablePublisherConfirm;

    public AmqpNotificationPublisher(
        SmartMessageConverter messageConverter,
        AmqpNotificationAutoConfiguration.Properties notificationProperties,
        AmqpAutoConfiguration.Properties properties,
        CachingConnectionFactoryConfigurer factoryConfigurer,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionFactoryCustomizer> factoryCustomizers
    ) {
        this(messageConverter, notificationProperties, properties, factoryConfigurer, factoryBean, factoryCustomizers, (correlationData, ack, cause) -> {
            log.message("接到publisher confirm")
                .context("correlationData", correlationData)
                .context("ack", ack)
                .context("cause", cause)
                .debug();

            if (!ack) {
                log.message("通知发送确认失败")
                    .context("correlationData", correlationData)
                    .context("cause", cause)
                    .warn();
            }
        });
    }

    public AmqpNotificationPublisher(
        SmartMessageConverter messageConverter,
        AmqpNotificationAutoConfiguration.Properties notificationProperties,
        AmqpAutoConfiguration.Properties properties,
        CachingConnectionFactoryConfigurer factoryConfigurer,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionFactoryCustomizer> factoryCustomizers,
        RabbitTemplate.ConfirmCallback confirmCallback
    ) {
        this.messageConverter = messageConverter;
        this.notificationProperties = notificationProperties;
        this.properties = properties;
        this.enablePublisherConfirm = properties.isEnablePublisherConfirm() && null != confirmCallback;

        CachingConnectionFactory connectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(factoryConfigurer, factoryBean, factoryCustomizers);
        if (enablePublisherConfirm) {
            connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        }

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        if (enablePublisherConfirm) {
            rabbitTemplate.setConfirmCallback(confirmCallback);
        }

        this.rabbitTemplate = rabbitTemplate;
    }

    public SmartMessageConverter getMessageConverter() {
        return messageConverter;
    }

    @Override
    public void publish(N notification, int delay) {
        publish(notification, delay, null);
    }

    public void publish(N notification, int delay, CorrelationData correlationData) {
        String prefix = notificationProperties.getNamePrefix();
        String source = getNotificationSource(notification.getClass());
        String routing = getRouting(prefix, source);
        String delayedRouting = getDelayRouting(prefix, source);

        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            if (delay > 0) {
                properties.setDelay(delay);
            }

            return message;
        };

        if (enablePublisherConfirm) {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                    delay > 0 ? delayedRouting : routing,
                    delay > 0 ? delayedRouting : routing,
                    notification,
                    messagePostProcessor,
                    correlationData
                );

                operations.waitForConfirmsOrDie(properties.getPublisherConfirmTimeout());
                return null;
            });
        }
        else {
            rabbitTemplate.convertAndSend(
                delay > 0 ? delayedRouting : routing,
                delay > 0 ? delayedRouting : routing,
                notification,
                messagePostProcessor,
                correlationData
            );
        }
    }

    @Override
    public void start() {
        rabbitTemplate.start();
    }

    @Override
    public void stop() {
        rabbitTemplate.stop();
    }

    @Override
    public boolean isRunning() {
        return rabbitTemplate.isRunning();
    }
}
