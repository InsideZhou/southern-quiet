package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AmqpJobAutoConfiguration;
import com.ai.southernquiet.job.JobProcessor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.Serializable;

public class AmqpJobEngineImpl<T extends Serializable> extends AbstractJobEngine<T> implements AmqpJobEngine<T> {
    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private AmqpJobAutoConfiguration.Properties properties;

    public AmqpJobEngineImpl(ConnectionFactory connectionFactory,
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

    @PostConstruct
    public void init() {
        amqpAdmin.declareQueue(new Queue(properties.getQueueName()));
    }

    @Transactional
    @Override
    public void arrange(T job) {
        rabbitTemplate.convertAndSend(properties.getQueueName(), job);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @RabbitListener(queues = "#{amqpJobProperties.queueName}")
    @Override
    public void process(Message message) throws Exception {
        T job = (T) rabbitTemplate.getMessageConverter().fromMessage(message);
        JobProcessor<T> processor = getProcessor(job);
        processor.process(job);
    }
}
