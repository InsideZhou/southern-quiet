package com.ai.southernquiet.notification;

import com.ai.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import com.ai.southernquiet.notification.driver.AmqpNotificationListenerManager;
import com.ai.southernquiet.notification.driver.AmqpNotificationPublisher;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@EnableRabbit
@Configuration
@EnableConfigurationProperties
public class AmqpNotificationAutoConfiguration {
    public final static String NAME_PREFIX = "NOTIFICATION.";

    @Bean
    @ConditionalOnMissingBean
    public RabbitListenerConfigurer rabbitListenerConfigurer(AmqpNotificationListenerManager listenerManager) {
        return listenerManager::registerListeners;
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpNotificationPublisher amqpNotificationPublisher(
        ConnectionFactory connectionFactory,
        @Autowired(required = false) MessageConverter messageConverter,
        AmqpAdmin amqpAdmin
    ) {
        return new AmqpNotificationPublisher(
            connectionFactory,
            null == messageConverter ? new Jackson2JsonMessageConverter() : messageConverter,
            amqpAdmin
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpNotificationListenerManager amqpNotificationListenerManager(
        ConnectionFactory connectionFactory,
        AmqpNotificationPublisher publisher,
        RabbitAdmin rabbitAdmin,
        AmqpAutoConfiguration.Properties amqpProperties,
        RabbitProperties rabbitProperties,
        ApplicationContext applicationContext
    ) {
        return new AmqpNotificationListenerManager(
            connectionFactory,
            rabbitAdmin,
            publisher,
            amqpProperties,
            rabbitProperties,
            applicationContext
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpAutoConfiguration.Properties amqpProperties() {
        return new AmqpAutoConfiguration.Properties();
    }

    @Bean
    @ConditionalOnMissingBean
    public Properties amqpNotificationProperties() {
        return new Properties();
    }

    @ConfigurationProperties("southern-quiet.framework.notification.amqp")
    public static class Properties {
    }
}
