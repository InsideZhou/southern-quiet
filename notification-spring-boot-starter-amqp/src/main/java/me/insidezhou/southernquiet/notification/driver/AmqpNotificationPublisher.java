package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.MessageSource;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationPublisher;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class AmqpNotificationPublisher<N> implements NotificationPublisher<N>, Lifecycle {
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
        RabbitProperties rabbitProperties,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
        boolean enablePublisherConfirm
    ) {
        this.messageConverter = messageConverter;
        this.notificationProperties = notificationProperties;
        this.properties = properties;
        this.enablePublisherConfirm = enablePublisherConfirm;

        CachingConnectionFactory connectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        connectionFactory.setPublisherConfirms(enablePublisherConfirm);

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        if (enablePublisherConfirm) {
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
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
        this.rabbitTemplate = rabbitTemplate;
    }

    public SmartMessageConverter getMessageConverter() {
        return messageConverter;
    }

    @Override
    public void publish(N notification) {
        String prefix = notificationProperties.getNamePrefix();
        String source = getNotificationSource(notification.getClass());
        String exchange = getExchange(prefix, source);
        String routing = getRouting(prefix, source);

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

    public static String getNotificationSource(Class<?> cls) {
        MessageSource annotation = AnnotationUtils.getAnnotation(cls, MessageSource.class);
        return null == annotation || StringUtils.isEmpty(annotation.source()) ? cls.getSimpleName() : annotation.source();
    }

    public static String getExchange(String prefix, Class<?> cls) {
        return getExchange(prefix, getNotificationSource(cls));
    }

    public static String getExchange(String prefix, String source) {
        return prefix + "EXCHANGE." + source;
    }

    public static String getRouting(String prefix, Class<?> cls) {
        return getRouting(prefix, getNotificationSource(cls));
    }

    public static String getRouting(String prefix, String source) {
        return prefix + source;
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
