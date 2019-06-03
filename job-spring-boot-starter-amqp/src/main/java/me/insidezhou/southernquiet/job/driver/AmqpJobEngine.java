package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AmqpJobEngine<T extends Serializable> extends AbstractJobEngine<T> {
    private final static Logger log = LoggerFactory.getLogger(AmqpJobEngine.class);

    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private AmqpJobAutoConfiguration.Properties properties;

    public AmqpJobEngine(MessageConverter messageConverter,
                         AmqpAdmin amqpAdmin,
                         AmqpJobAutoConfiguration.Properties properties,
                         RabbitProperties rabbitProperties,
                         RabbitConnectionFactoryBean factoryBean,
                         ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
    ) {
        this.amqpAdmin = amqpAdmin;
        this.properties = properties;

        CachingConnectionFactory connectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        connectionFactory.setPublisherConfirms(false);

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
        deadQueueArgs.put(Constants.AMQP_DLX, Constants.AMQP_DEFAULT);
        deadQueueArgs.put(Constants.AMQP_DLK, properties.getWorkingQueue());
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
        private AmqpAdmin amqpAdmin;

        public Recoverer(AmqpJobEngine amqpJobEngine, AmqpJobAutoConfiguration.Properties properties, RabbitProperties rabbitProperties) {
            super(amqpJobEngine.getRabbitTemplate(), properties.getDeadJobExchange(), properties.getDeadJobQueue());

            this.retry = rabbitProperties.getListener().getSimple().getRetry();
            this.properties = properties;
            this.amqpAdmin = amqpJobEngine.amqpAdmin;
        }

        @SuppressWarnings("Duplicates")
        @Override
        public void recover(Message message, Throwable cause) {
            MessageProperties messageProperties = message.getMessageProperties();
            Map<String, Object> headers = messageProperties.getHeaders();
            headers.putIfAbsent("x-recover-count", 0);
            headers.putIfAbsent("x-expiration", retry.getInitialInterval().toMillis());

            int messageCount;
            if (null != messageProperties.getMessageCount()) {
                messageCount = messageProperties.getMessageCount();
            }
            else {
                Properties queueProperties = amqpAdmin.getQueueProperties(messageProperties.getConsumerQueue());
                messageCount = (int) queueProperties.getOrDefault("QUEUE_MESSAGE_COUNT", 0);
            }

            int recoverCount = (int) headers.get("x-recover-count");

            long expiration;
            if (StringUtils.isEmpty(messageProperties.getExpiration())) {
                expiration = (long) headers.get("x-expiration");
            }
            else {
                expiration = Long.parseLong(messageProperties.getExpiration());
            }
            expiration += messageCount * recoverCount * retry.getMultiplier();
            expiration += expiration * retry.getMultiplier();

            messageProperties.setHeader("x-recover-count", ++recoverCount);
            messageProperties.setHeader("x-expiration", expiration);

            if (null == messageProperties.getDeliveryMode()) {
                messageProperties.setDeliveryMode(getDeliveryMode());
            }

            if (log.isDebugEnabled()) {
                log.debug(
                    "准备把任务送进死信队列: expiration={}/{}, recoverCount={}, deliveryMode={}, messageCount={}, message={}",
                    expiration,
                    properties.getJobTTL().toMillis(),
                    recoverCount,
                    messageProperties.getDeliveryMode(),
                    messageCount,
                    message,
                    cause
                );
            }

            if (expiration < properties.getJobTTL().toMillis()) {
                messageProperties.setExpiration(String.valueOf(expiration));
                errorTemplate.send(errorExchangeName, errorRoutingKey, message);
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
