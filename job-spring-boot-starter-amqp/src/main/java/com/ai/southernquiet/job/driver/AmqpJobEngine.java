package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AmqpJobAutoConfiguration;
import com.ai.southernquiet.job.JobProcessor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import javax.annotation.PostConstruct;
import java.io.Serializable;

public class AmqpJobEngine<T extends Serializable> extends AbstractJobEngine<T> {
    private AmqpTemplate amqpTemplate;
    private AmqpAdmin amqpAdmin;
    private AmqpJobAutoConfiguration.Properties properties;

    public AmqpJobEngine(AmqpTemplate amqpTemplate, AmqpAdmin amqpAdmin, AmqpJobAutoConfiguration.Properties properties) {
        this.amqpTemplate = amqpTemplate;
        this.amqpAdmin = amqpAdmin;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        amqpAdmin.declareQueue(new Queue(properties.getQueueName()));
    }

    @Override
    public void arrange(T job) {
        getProcessor(job);
        amqpTemplate.convertAndSend(properties.getQueueName(), job);
    }

    @RabbitListener(queues = "#{amqpJobProperties.queueName}")
    public void process(T job) {
        JobProcessor<T> processor = getProcessor(job);
        try {
            processor.process(job);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
