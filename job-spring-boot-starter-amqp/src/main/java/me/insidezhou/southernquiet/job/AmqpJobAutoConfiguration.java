package me.insidezhou.southernquiet.job;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpAutoConfiguration;
import me.insidezhou.southernquiet.amqp.rabbit.AmqpMessageRecover;
import me.insidezhou.southernquiet.amqp.rabbit.DirectRabbitListenerContainerFactoryConfigurer;
import me.insidezhou.southernquiet.job.driver.AmqpJobEngine;
import me.insidezhou.southernquiet.job.driver.AmqpJobListener;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "WeakerAccess"})
@EnableRabbit
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
@EnableConfigurationProperties
public class AmqpJobAutoConfiguration {
    public final static String AmqpJobListenerContainerFactory = "amqpJobListenerContainerFactory";

    @Bean
    @ConditionalOnMissingBean
    public AmqpJobEngine amqpJobEngine(@Autowired(required = false) MessageConverter messageConverter,
                                       AmqpAdmin amqpAdmin,
                                       Properties properties,
                                       RabbitProperties rabbitProperties,
                                       RabbitConnectionFactoryBean factoryBean,
                                       ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
    ) {
        return new AmqpJobEngine(
            null == messageConverter ? new Jackson2JsonMessageConverter() : messageConverter,
            amqpAdmin,
            properties,
            rabbitProperties,
            factoryBean,
            connectionNameStrategy
        );
    }

    @Bean(AmqpJobListenerContainerFactory)
    @ConditionalOnMissingBean
    public DirectRabbitListenerContainerFactory amqpJobListenerContainerFactory(
        @Autowired(required = false) MessageConverter messageConverter,
        AmqpAutoConfiguration.Properties amqpProperties,
        AmqpJobAutoConfiguration.Properties properties,
        RabbitProperties rabbitProperties,
        RabbitConnectionFactoryBean factoryBean,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
        AmqpAdmin amqpAdmin,
        AmqpJobEngine jobEngine
    ) {
        CachingConnectionFactory cachingConnectionFactory = AmqpAutoConfiguration.rabbitConnectionFactory(rabbitProperties, factoryBean, connectionNameStrategy);

        DirectRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer = new DirectRabbitListenerContainerFactoryConfigurer(
            rabbitProperties,
            new AmqpMessageRecover(
                jobEngine.getRabbitTemplate(),
                amqpAdmin,
                properties.getDeadJobExchange(),
                properties.getDeadJobQueue(),
                amqpProperties
            ),
            amqpProperties
        );

        DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
        factory.setMessageConverter(null == messageConverter ? new Jackson2JsonMessageConverter() : messageConverter);
        factory.setAcknowledgeMode(amqpProperties.getAcknowledgeMode());
        containerFactoryConfigurer.configure(factory, cachingConnectionFactory);

        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpJobListener amqpJobListener(AmqpJobEngine jobEngine) {
        return new AmqpJobEngine.Listener(jobEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.job.amqp")
    public Properties amqpJobProperties() {
        return new Properties();
    }

    public static class Properties {
        /**
         * 任务队列名。
         */
        private String workingQueue = "JOB.WORKING-QUEUE";

        /**
         * 任务监听容器的初始化工厂名。
         */
        private String containerFactoryBeanName = AmqpJobListenerContainerFactory;

        /**
         * 异常任务交换器名。
         */
        private String deadJobExchange = Constants.AMQP_DEFAULT;

        /**
         * 异常任务队列名。
         */
        private String deadJobQueue = "JOB.DEAD-QUEUE";

        /**
         * 任务的生命周期，超时的任务在异常队列中不再重新投递。
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration jobTTL = Duration.ofHours(1);

        public String getWorkingQueue() {
            return workingQueue;
        }

        public void setWorkingQueue(String workingQueue) {
            this.workingQueue = workingQueue;
        }

        public String getContainerFactoryBeanName() {
            return containerFactoryBeanName;
        }

        public void setContainerFactoryBeanName(String containerFactoryBeanName) {
            this.containerFactoryBeanName = containerFactoryBeanName;
        }

        public String getDeadJobExchange() {
            return deadJobExchange;
        }

        public void setDeadJobExchange(String deadJobExchange) {
            this.deadJobExchange = deadJobExchange;
        }

        public String getDeadJobQueue() {
            return deadJobQueue;
        }

        public void setDeadJobQueue(String deadJobQueue) {
            this.deadJobQueue = deadJobQueue;
        }

        public Duration getJobTTL() {
            return jobTTL;
        }

        public void setJobTTL(Duration jobTTL) {
            this.jobTTL = jobTTL;
        }
    }
}
