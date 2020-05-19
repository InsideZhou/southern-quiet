package me.insidezhou.southernquiet.amqp.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.insidezhou.southernquiet.util.GoldenRatioAmplifier;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
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

    @SuppressWarnings({"ConstantConditions", "Duplicates"})
    public static CachingConnectionFactory rabbitConnectionFactory(
        RabbitProperties rabbitProperties,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
    ) {
        PropertyMapper map = PropertyMapper.get();
        CachingConnectionFactory factory;
        try {
            factory = new CachingConnectionFactory(factoryBean.getObject());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        map.from(rabbitProperties::determineAddresses).to(factory::setAddresses);
        map.from(rabbitProperties::isPublisherConfirms).to(factory::setPublisherConfirms);
        map.from(rabbitProperties::isPublisherReturns).to(factory::setPublisherReturns);
        RabbitProperties.Cache.Channel channel = rabbitProperties.getCache().getChannel();
        map.from(channel::getSize).whenNonNull().to(factory::setChannelCacheSize);
        map.from(channel::getCheckoutTimeout).whenNonNull().as(Duration::toMillis)
            .to(factory::setChannelCheckoutTimeout);
        RabbitProperties.Cache.Connection connection = rabbitProperties.getCache()
            .getConnection();
        map.from(connection::getMode).whenNonNull().to(factory::setCacheMode);
        map.from(connection::getSize).whenNonNull()
            .to(factory::setConnectionCacheSize);
        map.from(connectionNameStrategy::getIfUnique).whenNonNull()
            .to(factory::setConnectionNameStrategy);
        return factory;
    }

    @SuppressWarnings({"Duplicates", "SpringJavaInjectionPointsAutowiringInspection"})
    @Bean
    @ConditionalOnMissingBean
    public RabbitConnectionFactoryBean getRabbitConnectionFactoryBean(RabbitProperties rabbitProperties) {
        PropertyMapper map = PropertyMapper.get();
        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
        map.from(rabbitProperties::determineHost).whenNonNull().to(factory::setHost);
        map.from(rabbitProperties::determinePort).to(factory::setPort);
        map.from(rabbitProperties::determineUsername).whenNonNull().to(factory::setUsername);
        map.from(rabbitProperties::determinePassword).whenNonNull().to(factory::setPassword);
        map.from(rabbitProperties::determineVirtualHost).whenNonNull().to(factory::setVirtualHost);
        map.from(rabbitProperties::getRequestedHeartbeat).whenNonNull().asInt(Duration::getSeconds)
            .to(factory::setRequestedHeartbeat);
        RabbitProperties.Ssl ssl = rabbitProperties.getSsl();
        if (ssl.isEnabled()) {
            factory.setUseSSL(true);
            map.from(ssl::getAlgorithm).whenNonNull().to(factory::setSslAlgorithm);
            map.from(ssl::getKeyStoreType).to(factory::setKeyStoreType);
            map.from(ssl::getKeyStore).to(factory::setKeyStore);
            map.from(ssl::getKeyStorePassword).to(factory::setKeyStorePassphrase);
            map.from(ssl::getTrustStoreType).to(factory::setTrustStoreType);
            map.from(ssl::getTrustStore).to(factory::setTrustStore);
            map.from(ssl::getTrustStorePassword).to(factory::setTrustStorePassphrase);
            map.from(ssl::isValidateServerCertificate)
                .to((validate) -> factory.setSkipServerCertificateValidation(!validate));
            map.from(ssl::getVerifyHostname).to(factory::setEnableHostnameVerification);
        }
        map.from(rabbitProperties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
            .to(factory::setConnectionTimeout);
        factory.afterPropertiesSet();
        return factory;
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
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
