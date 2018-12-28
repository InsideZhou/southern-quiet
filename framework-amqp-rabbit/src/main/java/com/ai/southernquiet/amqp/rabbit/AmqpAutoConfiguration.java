package com.ai.southernquiet.amqp.rabbit;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
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
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.amqp.rabbit")
    public Properties amqpProperties() {
        return new Properties();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Properties {
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration initialExpiration = Duration.ofSeconds(3);

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration expiration = Duration.ofDays(1);

        private double power = 1.2;

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

        public double getPower() {
            return power;
        }

        public void setPower(double power) {
            this.power = power;
        }
    }
}
