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
    public Properties properties() {
        return new Properties();
    }

    @ConfigurationProperties("southern-quiet.framework.amqp.rabbit")
    public static class Properties {
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration initialExpiration = Duration.ofSeconds(5);

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration expiration = Duration.ofDays(1);

        private double multiplier = 2.0;

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

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}
