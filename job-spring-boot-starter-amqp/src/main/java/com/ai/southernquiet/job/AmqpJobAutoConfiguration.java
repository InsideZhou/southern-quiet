package com.ai.southernquiet.job;

import com.ai.southernquiet.job.driver.AmqpJobEngine;
import com.ai.southernquiet.job.driver.AmqpJobListener;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

import static com.ai.southernquiet.Constants.AMQP_DEFAULT;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@EnableRabbit
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
@EnableConfigurationProperties
public class AmqpJobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AmqpJobEngine.Recoverer amqpJobRecoverer(AmqpJobEngine jobEngine, Properties properties, RabbitProperties rabbitProperties) {
        return new AmqpJobEngine.Recoverer(jobEngine, properties, rabbitProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AmqpJobEngine amqpJobEngine(ConnectionFactory connectionFactory,
                                       @Autowired(required = false) MessageConverter messageConverter,
                                       AmqpAdmin amqpAdmin,
                                       Properties properties) {
        return new AmqpJobEngine(
            connectionFactory,
            null == messageConverter ? new Jackson2JsonMessageConverter() : messageConverter,
            amqpAdmin,
            properties
        );
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
         * 异常任务交换器名。
         */
        private String deadJobExchange = AMQP_DEFAULT;

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
