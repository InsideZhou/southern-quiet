package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AmqpJobAutoConfiguration;
import com.ai.southernquiet.job.JobProcessor;
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

import static com.ai.southernquiet.Constants.AMQP_DLK;
import static com.ai.southernquiet.Constants.AMQP_DLX;

public class AmqpJobEngine<T extends Serializable> extends AbstractJobEngine<T> {
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

    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> deadQueueArgs = new HashMap<>();
        deadQueueArgs.put(AMQP_DLX, rabbitTemplate.getExchange());
        deadQueueArgs.put(AMQP_DLK, properties.getWorkingQueue());
        Queue deadQueue = new Queue(properties.getDeadJobQueue(), true, false, false, deadQueueArgs);

        Exchange deadExchange = new DirectExchange(properties.getDeadJobExchange());

        Binding deadBinding = BindingBuilder.bind(deadQueue).to(deadExchange).with(properties.getDeadJobQueue()).noargs();


        amqpAdmin.declareQueue(new Queue(properties.getWorkingQueue()));
        amqpAdmin.declareQueue(deadQueue);

        amqpAdmin.declareExchange(deadExchange);

        amqpAdmin.declareBinding(deadBinding);
    }

    @Transactional
    @Override
    public void arrange(T job) {
        rabbitTemplate.convertAndSend(properties.getWorkingQueue(), job);
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
