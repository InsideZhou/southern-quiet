package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobArranger;
import me.insidezhou.southernquiet.amqp.rabbit.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

public class AmqpJobArranger<J> implements JobArranger<J> {
    private final static Logger log = LoggerFactory.getLogger(AmqpJobArranger.class);

    private final RabbitTemplate rabbitTemplate;
    private final SmartMessageConverter messageConverter;
    private final AmqpJobAutoConfiguration.Properties jobProperties;

    public AmqpJobArranger(SmartMessageConverter messageConverter,
                           AmqpJobAutoConfiguration.Properties jobProperties,
                           RabbitTransactionManager transactionManager
    ) {
        this.messageConverter = messageConverter;
        this.jobProperties = jobProperties;

        RabbitTemplate rabbitTemplate = new RabbitTemplate(transactionManager.getConnectionFactory());
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setChannelTransacted(true);
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void arrange(J job) {
        String prefix = jobProperties.getNamePrefix();
        String source = getQueueSource(job.getClass());
        String exchange = getExchange(prefix, source);
        String routing = getRouting(prefix, source);

        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        };

        rabbitTemplate.convertAndSend(
            exchange,
            routing,
            job,
            messagePostProcessor
        );
    }

    public SmartMessageConverter getMessageConverter() {
        return messageConverter;
    }

    public static String getQueueSource(Class<?> cls) {
        MessageSource annotation = AnnotationUtils.getAnnotation(cls, MessageSource.class);
        return null == annotation || StringUtils.isEmpty(annotation.source()) ? cls.getSimpleName() : annotation.source();
    }

    public static String getExchange(String prefix, Class<?> cls) {
        return getExchange(prefix, getQueueSource(cls));
    }

    public static String getExchange(String prefix, String source) {
        return prefix + "EXCHANGE." + source;
    }

    public static String getRouting(String prefix, Class<?> cls) {
        return getRouting(prefix, getQueueSource(cls));
    }

    public static String getRouting(String prefix, String source) {
        return prefix + source;
    }
}
