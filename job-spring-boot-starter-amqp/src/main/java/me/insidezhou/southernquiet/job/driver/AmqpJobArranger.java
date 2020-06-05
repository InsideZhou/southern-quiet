package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AbstractAmqpJobArranger;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.context.Lifecycle;

public class AmqpJobArranger<J> extends AbstractAmqpJobArranger<J> implements Lifecycle {
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
    public void arrange(J job, long delay) {
        String prefix = jobProperties.getNamePrefix();
        String source = getQueueSource(job.getClass());
        String exchange = getExchange(prefix, source);
        String routing = getRouting(prefix, source);
        String delayRouting = getDelayedRouting(prefix, source);

        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            if (delay > 0) {
                properties.setExpiration(String.valueOf(delay));
            }

            return message;
        };

        rabbitTemplate.convertAndSend(
            delay > 0 ? Constants.AMQP_DEFAULT : exchange,
            delay > 0 ? delayRouting : routing,
            job,
            messagePostProcessor
        );
    }

    public SmartMessageConverter getMessageConverter() {
        return messageConverter;
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
