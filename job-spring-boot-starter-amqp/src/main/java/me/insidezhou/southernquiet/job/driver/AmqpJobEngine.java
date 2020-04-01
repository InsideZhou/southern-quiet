package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
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

    @Override
    public void arrange(T job) {
        rabbitTemplate.convertAndSend(properties.getWorkingQueue(), job, message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }

    @SuppressWarnings("unchecked")
    public void process(Message message) throws Exception {
        T job = (T) rabbitTemplate.getMessageConverter().fromMessage(message);
        JobProcessor<T> processor = getProcessor(job);
        processor.process(job);
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
