package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AbstractAmqpNotificationPublisher;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.MessageSource;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class AmqpNotificationPublisher<N> extends AbstractAmqpNotificationPublisher<N> implements Lifecycle {
    public final static String DelayedMessageExchange = "NOTIFICATION.EXCHANGE.DELAYED_MESSAGE";
    public final static String DelayedMessageQueue = "NOTIFICATION.QUEUE.DELAYED_MESSAGE";

    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmqpNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final SmartMessageConverter messageConverter;
    private final AmqpNotificationAutoConfiguration.Properties notificationProperties;
    private final AmqpAutoConfiguration.Properties properties;

    private final boolean enablePublisherConfirm;

    public AmqpNotificationPublisher(
        RabbitAdmin rabbitAdmin,
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

        Exchange exchange = new FanoutExchange(DelayedMessageExchange);
        Queue queue = new Queue(DelayedMessageQueue);

        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(DelayedMessageQueue).noargs());
    }

    public SmartMessageConverter getMessageConverter() {
        return messageConverter;
    }

    @Override
    public void publish(N notification, long delay) {
        String prefix = notificationProperties.getNamePrefix();
        String source = getNotificationSource(notification.getClass());
        String exchange = getExchange(prefix, source);
        String routing = getRouting(prefix, source);

        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            if (delay > 0) {
                properties.setExpiration(String.valueOf(delay));
                properties.setHeader(Constants.AMQP_DLK, exchange);
                properties.setHeader(Constants.AMQP_DLK, routing);
            }

            return message;
        };

        if (enablePublisherConfirm) {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                    delay > 0 ? DelayedMessageExchange : exchange,
                    delay > 0 ? DelayedMessageQueue : routing,
                    notification,
                    messagePostProcessor
                );

                rabbitTemplate.waitForConfirmsOrDie(properties.getPublisherConfirmTimeout());
                return null;
            });
        }
        else {
            rabbitTemplate.convertAndSend(
                delay > 0 ? DelayedMessageExchange : exchange,
                delay > 0 ? DelayedMessageQueue : routing,
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
