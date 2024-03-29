package me.insidezhou.southernquiet.notification;

import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.notification.driver.AmqpNotificationListenerManager;
import me.insidezhou.southernquiet.notification.driver.AmqpNotificationPublisher;
import me.insidezhou.southernquiet.util.Amplifier;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.CachingConnectionFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration.RecoverAmplifierQualifier;

@EnableRabbit
@Configuration
@EnableConfigurationProperties
@AutoConfigureAfter({RabbitAutoConfiguration.class, AmqpAutoConfiguration.class})
public class AmqpNotificationAutoConfiguration {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Bean
    @ConditionalOnMissingBean
    public AmqpNotificationPublisher amqpNotificationPublisher(
        SmartMessageConverter messageConverter,
        AmqpNotificationAutoConfiguration.Properties notificationProperties,
        AmqpAutoConfiguration.Properties properties,
        CachingConnectionFactoryConfigurer factoryConfigurer,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionFactoryCustomizer> factoryCustomizers
    ) {
        return new AmqpNotificationPublisher(
            messageConverter,
            notificationProperties,
            properties,
            factoryConfigurer,
            factoryBean,
            factoryCustomizers
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpNotificationListenerManager amqpNotificationListenerManager(
        AmqpAdmin amqpAdmin,
        @Qualifier(RecoverAmplifierQualifier) Amplifier amplifier,
        AmqpNotificationPublisher<?> publisher,
        AmqpNotificationAutoConfiguration.Properties amqpNotificationProperties,
        AmqpAutoConfiguration.Properties amqpProperties,
        RabbitProperties rabbitProperties,
        CachingConnectionFactoryConfigurer factoryConfigurer,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionFactoryCustomizer> factoryCustomizers,
        ApplicationContext applicationContext
    ) {
        return new AmqpNotificationListenerManager(
            amqpAdmin,
            publisher,
            amplifier,
            amqpNotificationProperties,
            amqpProperties,
            rabbitProperties,
            factoryConfigurer,
            factoryBean,
            factoryCustomizers,
            applicationContext
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.notification.amqp")
    public Properties amqpNotificationProperties() {
        return new Properties();
    }

    public static class Properties {
        /**
         * 队列的前缀
         */
        private String namePrefix = "NOTIFICATION.";

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
        }
    }
}
