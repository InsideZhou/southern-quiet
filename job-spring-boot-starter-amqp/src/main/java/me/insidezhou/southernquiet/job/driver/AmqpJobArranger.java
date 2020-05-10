package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobArranger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

public class AmqpJobArranger<J> implements JobArranger<J> {

    private final RabbitTemplate rabbitTemplate;
    private final SmartMessageConverter messageConverter;
    private final AmqpJobAutoConfiguration.Properties jobProperties;
    private final AmqpAutoConfiguration.Properties properties;

    public AmqpJobArranger(SmartMessageConverter messageConverter,
                           AmqpJobAutoConfiguration.Properties jobProperties,
                           AmqpAutoConfiguration.Properties properties,
                           RabbitProperties rabbitProperties,
                           RabbitConnectionFactoryBean factoryBean,
                           ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
    ) {

        this.messageConverter = messageConverter;
        this.jobProperties = jobProperties;
        this.properties = properties;

        CachingConnectionFactory connectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setChannelTransacted(true);
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void arrange(J notification) {
    }

    public SmartMessageConverter getMessageConverter() {
        return messageConverter;
    }

    public static String getJobSource(Class<?> cls) {
        return cls.getSimpleName();
    }

    public static String getExchange(String prefix, Class<?> cls) {
        return getExchange(prefix, getJobSource(cls));
    }

    public static String getExchange(String prefix, String source) {
        return prefix + "EXCHANGE." + source;
    }

    public static String getRouting(String prefix, Class<?> cls) {
        return getRouting(prefix, getJobSource(cls));
    }

    public static String getRouting(String prefix, String source) {
        return prefix + source;
    }
}
