package me.insidezhou.southernquiet.job;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.job.driver.AmqpJobArranger;
import me.insidezhou.southernquiet.job.driver.AmqpJobProcessorManager;
import me.insidezhou.southernquiet.util.Amplifier;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration.RecoverAmplifierQualifier;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "WeakerAccess"})
@EnableRabbit
@EnableTransactionManagement
@Configuration
@EnableConfigurationProperties
@AutoConfigureAfter({RabbitAutoConfiguration.class, AmqpAutoConfiguration.class})
@AutoConfigureOrder(Constants.AutoConfigLevel_Highest)
public class AmqpJobAutoConfiguration {
    @ConditionalOnMissingBean
    @Bean
    public RabbitTransactionManager rabbitTransactionManager(
        RabbitProperties rabbitProperties,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
    ) {
        return new RabbitTransactionManager(AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy));
    }

    @SuppressWarnings("rawtypes")
    @Bean
    @ConditionalOnMissingBean
    public AmqpJobArranger amqpJobArranger(
        SmartMessageConverter messageConverter,
        AmqpJobAutoConfiguration.Properties jobProperties,
        RabbitTransactionManager transactionManager
    ) {
        return new AmqpJobArranger(messageConverter, jobProperties, transactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpJobProcessorManager amqpJobProcessorManager(
        RabbitAdmin rabbitAdmin,
        @Qualifier(RecoverAmplifierQualifier) Amplifier amplifier,
        AmqpJobArranger<?> jobArranger,
        AmqpJobAutoConfiguration.Properties amqpJobProperties,
        AmqpAutoConfiguration.Properties amqpProperties,
        RabbitProperties rabbitProperties,
        RabbitTransactionManager transactionManager,
        ApplicationContext applicationContext
    ) {
        return new AmqpJobProcessorManager(
            rabbitAdmin, jobArranger, amplifier, amqpJobProperties, amqpProperties, transactionManager, rabbitProperties, applicationContext
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.job.amqp")
    public Properties amqpJobProperties() {
        return new Properties();
    }

    public static class Properties {
        /**
         * 队列的前缀
         */
        private String namePrefix = "JOB.";

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
        }
    }
}
