package me.insidezhou.southernquiet.amqp.rabbit;

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.amqp.AbstractRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.PropertyMapper;

public class DirectRabbitListenerContainerFactoryConfigurer extends AbstractRabbitListenerContainerFactoryConfigurer<DirectRabbitListenerContainerFactory> {
    private final AmqpAutoConfiguration.Properties properties;

    public DirectRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties,
                                                          MessageRecoverer messageRecoverer,
                                                          AmqpAutoConfiguration.Properties properties) {
        super(rabbitProperties);
        this.properties = properties;

        setMessageRecoverer(messageRecoverer);
    }

    @Override
    public void configure(DirectRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
        RabbitProperties.DirectContainer config = getRabbitProperties().getListener().getDirect();

        DirectContainer localConfig = new DirectContainer();

        RabbitProperties.ListenerRetry retry = new RabbitProperties.ListenerRetry();
        BeanUtils.copyProperties(config.getRetry(), retry);
        retry.setEnabled(true); //这里必须强行设置为true，否则recover机制不生效，上游的实现依赖了这个值。
        retry.setMaxAttempts(properties.getMaxDeliveryAttempts() > 0 ? properties.getMaxDeliveryAttempts() : 1);

        BeanUtils.copyProperties(config, localConfig);
        localConfig.setRetry(retry);
        configure(factory, connectionFactory, localConfig);

        PropertyMapper map = PropertyMapper.get();
        map.from(config::getConsumersPerQueue).whenNonNull().to(factory::setConsumersPerQueue);
    }

    public static class DirectContainer extends RabbitProperties.DirectContainer {
        private RabbitProperties.ListenerRetry retry;

        @Override
        public RabbitProperties.ListenerRetry getRetry() {
            return retry;
        }

        public void setRetry(RabbitProperties.ListenerRetry retry) {
            this.retry = retry;
        }
    }
}
