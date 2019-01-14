package com.ai.southernquiet.notification;

import com.ai.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import com.ai.southernquiet.notification.driver.AmqpNotificationListenerManager;
import com.ai.southernquiet.notification.driver.AmqpNotificationPublisher;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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
        @Autowired(required = false) MessageConverter messageConverter,
        AmqpAdmin amqpAdmin,
        Properties properties,
        RabbitProperties rabbitProperties,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
    ) {
        return new AmqpNotificationPublisher(
            null == messageConverter ? new Jackson2JsonMessageConverter() : messageConverter,
            amqpAdmin,
            properties,
            rabbitProperties,
            connectionNameStrategy,
            properties.isEnablePublisherConfirm()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpNotificationListenerManager amqpNotificationListenerManager(
        AmqpNotificationPublisher publisher,
        RabbitAdmin rabbitAdmin,
        AmqpAutoConfiguration.Properties amqpProperties,
        RabbitProperties rabbitProperties,
        PlatformTransactionManager transactionManager,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
        ApplicationContext applicationContext
    ) {
        return new AmqpNotificationListenerManager(
            rabbitAdmin,
            publisher,
            amqpProperties,
            rabbitProperties,
            transactionManager,
            connectionNameStrategy,
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
    @ConfigurationProperties("southern-quiet.framework.notification.amqp")
    public Properties amqpNotificationProperties() {
        return new Properties();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Properties {
        /**
         * 在开启publisher confirm的情况下，等待confirm的超时时间，单位：毫秒。
         */
        private long publisherConfirmTimeout = 5000;

        /**
         * 是否打开publisher confirm
         */
        private boolean enablePublisherConfirm = false;

        public long getPublisherConfirmTimeout() {
            return publisherConfirmTimeout;
        }

        public void setPublisherConfirmTimeout(long publisherConfirmTimeout) {
            this.publisherConfirmTimeout = publisherConfirmTimeout;
        }

        public boolean isEnablePublisherConfirm() {
            return enablePublisherConfirm;
        }

        public void setEnablePublisherConfirm(boolean enablePublisherConfirm) {
            this.enablePublisherConfirm = enablePublisherConfirm;
        }
    }
}
