package me.insidezhou.southernquiet.amqp.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.insidezhou.southernquiet.util.GoldenRatioAmplifier;
import me.insidezhou.southernquiet.util.Metadata;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.CachingConnectionFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionFactoryBeanConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@EnableRabbit
@Configuration
@EnableConfigurationProperties
public class AmqpAutoConfiguration {
    public final static String RecoverAmplifierQualifier = "AmqpAutoConfiguration.RecoverAmplifierQualifier";

    @Bean
    @Qualifier(RecoverAmplifierQualifier)
    public GoldenRatioAmplifier amqpRecoverAmplifier(AmqpAutoConfiguration.Properties amqpProperties) {
        return new GoldenRatioAmplifier(amqpProperties.getInitialExpiration().toMillis());
    }

    @SuppressWarnings("ConstantConditions")
    public static CachingConnectionFactory rabbitConnectionFactory(CachingConnectionFactoryConfigurer configurer,
                                                                   RabbitConnectionFactoryBean factoryBean,
                                                                   ObjectProvider<ConnectionFactoryCustomizer> connectionFactoryCustomizers) {
        com.rabbitmq.client.ConnectionFactory connectionFactory;
        try {
            connectionFactory = factoryBean.getObject();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        connectionFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(connectionFactory));

        CachingConnectionFactory factory = new CachingConnectionFactory(connectionFactory);
        configurer.configure(factory);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitConnectionFactoryBean getRabbitConnectionFactoryBean(RabbitConnectionFactoryBeanConfigurer configurer) {
        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
        configurer.configure(factory);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public SmartMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.amqp.rabbit")
    public Properties amqpProperties() {
        return new Properties();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Properties {
        /**
         * 把优先队列的级别数设定为cpu核心数的倍数，并且受rabbitmq的上限控制。
         */
        public int getPriorityLevels(Metadata metadata) {
            return Math.min(metadata.getCoreNumber() * 2, 255);
        }

        /**
         * 预估的消息最大超时时间。
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration estimatedMaxExpiration = Duration.ofHours(3);

        /**
         * 消息的初始超时时间。
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration initialExpiration = Duration.ofSeconds(5);

        /**
         * 消息的超时上限，当前超时超过上限时，永远进入死信队列不再重新投递到工作队列。
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration expiration = Duration.ofDays(1);

        /**
         * 在开启publisher confirm的情况下，等待confirm的超时时间，单位：毫秒。
         */
        private long publisherConfirmTimeout = 5000;

        /**
         * 是否打开publisher confirm
         */
        private boolean enablePublisherConfirm = true;

        /**
         * 消费者应答模式。
         */
        private AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO;

        /**
         * 最大投递次数，包括重试次数在内，小于1的值会被强行设置为1。
         *
         * @see RabbitProperties.Retry#getMaxAttempts()
         */
        private int maxDeliveryAttempts = 1;

        public Duration getEstimatedMaxExpiration() {
            return estimatedMaxExpiration;
        }

        public void setEstimatedMaxExpiration(Duration estimatedMaxExpiration) {
            this.estimatedMaxExpiration = estimatedMaxExpiration;
        }

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

        public AcknowledgeMode getAcknowledgeMode() {
            return acknowledgeMode;
        }

        public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
            this.acknowledgeMode = acknowledgeMode;
        }

        public Duration getInitialExpiration() {
            return initialExpiration;
        }

        public void setInitialExpiration(Duration initialExpiration) {
            this.initialExpiration = initialExpiration;
        }

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }

        public int getMaxDeliveryAttempts() {
            return maxDeliveryAttempts;
        }

        public void setMaxDeliveryAttempts(int maxDeliveryAttempts) {
            this.maxDeliveryAttempts = maxDeliveryAttempts;
        }
    }
}
