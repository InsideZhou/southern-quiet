package com.ai.southernquiet.job;

import com.ai.southernquiet.job.driver.AmqpJobEngine;
import com.ai.southernquiet.job.driver.AmqpJobEngineImpl;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@EnableRabbit
@EnableTransactionManagement
@Configuration
@EnableConfigurationProperties
public class AmqpJobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AmqpJobEngine amqpJobEngine(ConnectionFactory connectionFactory, AmqpAdmin amqpAdmin, Properties properties) {
        return new AmqpJobEngineImpl(connectionFactory, new Jackson2JsonMessageConverter(), amqpAdmin, properties);
    }

    @Bean
    @ConfigurationProperties("southern-quiet.framework.job.amqp")
    public Properties amqpJobProperties() {
        return new Properties();
    }

    public static class Properties {
        /**
         * 任务队列名。
         */
        private String queueName = "southern-quiet.framework.job.default-queue";

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }
    }
}
