package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AmqpJobAutoConfiguration;
import com.ai.southernquiet.job.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.ai.southernquiet.Constants.*;

public class AmqpJobEngine<T extends Serializable> extends AbstractJobEngine<T> {
    private final static Logger log = LoggerFactory.getLogger(AmqpJobEngine.class);

    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private AmqpJobAutoConfiguration.Properties properties;

    public AmqpJobEngine(ConnectionFactory connectionFactory,
                         MessageConverter messageConverter,
                         AmqpAdmin amqpAdmin,
                         AmqpJobAutoConfiguration.Properties properties
    ) {
        this.amqpAdmin = amqpAdmin;
        this.properties = properties;

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setChannelTransacted(true);

        this.rabbitTemplate = rabbitTemplate;
    }

    @SuppressWarnings("WeakerAccess")
    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(AMQP_DLX, AMQP_DEFAULT);
        deadQueueArgs.put(AMQP_DLK, properties.getWorkingQueue());
        Queue deadQueue = new Queue(properties.getDeadJobQueue(), true, false, false, deadQueueArgs);

        amqpAdmin.declareQueue(new Queue(properties.getWorkingQueue()));
        amqpAdmin.declareQueue(deadQueue);

        Exchange deadExchange = new DirectExchange(properties.getDeadJobExchange());
        Binding deadBinding = BindingBuilder.bind(deadQueue).to(deadExchange).with(properties.getDeadJobQueue()).noargs();

        amqpAdmin.declareExchange(deadExchange);
        amqpAdmin.declareBinding(deadBinding);
    }

    @Transactional
    @Override
    public void arrange(T job) {
        rabbitTemplate.convertAndSend(properties.getWorkingQueue(), job, message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void process(Message message) throws Exception {
        T job = (T) rabbitTemplate.getMessageConverter().fromMessage(message);
        JobProcessor<T> processor = getProcessor(job);
        processor.process(job);
    }

    public static class Recoverer extends RepublishMessageRecoverer {
        private RabbitProperties.ListenerRetry retry;
        private AmqpJobAutoConfiguration.Properties properties;

        public Recoverer(AmqpJobEngine amqpJobEngine, AmqpJobAutoConfiguration.Properties properties, RabbitProperties rabbitProperties) {
            super(amqpJobEngine.getRabbitTemplate(), properties.getDeadJobExchange(), properties.getDeadJobQueue());

            this.retry = rabbitProperties.getListener().getSimple().getRetry();
            this.properties = properties;
        }

        @SuppressWarnings("Duplicates")
        @Override
        public void recover(Message message, Throwable cause) {
            MessageProperties messageProperties = message.getMessageProperties();

            long expiration = (long) messageProperties.getHeaders().compute("original-expiration", (key, value) -> {
                if (StringUtils.isEmpty(value)) {
                    return retry.getInitialInterval().toMillis();
                }

                long expiry = (long) value;
                return expiry + expiry * (long) retry.getMultiplier();
            });

            if (log.isDebugEnabled()) {
                log.debug(
                    "准备把消息送进死信队列: expiration/ttl={}/{}, deliveryMode={}, message={}",
                    expiration,
                    properties.getJobTTL().toMillis(),
                    messageProperties.getDeliveryMode(),
                    message
                );
            }

            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            if (expiration < properties.getJobTTL().toMillis()) {
                messageProperties.setExpiration(String.valueOf(expiration));
                errorTemplate.send(properties.getDeadJobExchange(), properties.getDeadJobQueue(), message);
            }
            else {
                messageProperties.setExpiration(null);
                super.recover(message, cause);
            }
        }
    }

    public static class Listener implements AmqpJobListener {
        private AmqpJobEngine jobEngine;

        public Listener(AmqpJobEngine jobEngine) {
            this.jobEngine = jobEngine;
        }

        @Override
        public void process(Message message) throws Exception {
            jobEngine.process(message);
        }
    }
}
