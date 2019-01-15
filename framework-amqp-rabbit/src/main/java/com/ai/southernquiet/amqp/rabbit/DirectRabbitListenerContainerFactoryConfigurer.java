package com.ai.southernquiet.amqp.rabbit;

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.autoconfigure.amqp.AbstractRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.PropertyMapper;

public class DirectRabbitListenerContainerFactoryConfigurer extends AbstractRabbitListenerContainerFactoryConfigurer<DirectRabbitListenerContainerFactory> {
    private AmqpAutoConfiguration.Properties properties;

    public DirectRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties,
                                                          MessageRecoverer messageRecoverer,
                                                          AmqpAutoConfiguration.Properties properties) {
        this.properties = properties;

        setRabbitProperties(rabbitProperties);
        setMessageRecoverer(messageRecoverer);
    }

    @Override
    public void configure(DirectRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
        PropertyMapper map = PropertyMapper.get();
        RabbitProperties.DirectContainer config = getRabbitProperties().getListener().getDirect();
        config.getRetry().setMaxAttempts(properties.getMaxDeliveryAttempts());
        configure(factory, connectionFactory, config);
        map.from(config::getConsumersPerQueue).whenNonNull().to(factory::setConsumersPerQueue);
    }
}
