package me.insidezhou.southernquiet.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.driver.JsonSerializationRedisSerializer;
import me.insidezhou.southernquiet.event.driver.RedisEventPubSub;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "rawtypes"})
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@AutoConfigureOrder(Constants.AutoConfigLevel_Highest)
public class RedisEventAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisEventPubSub redisEventPublisher(RedisTemplateBuilder builder, ObjectMapper objectMapper, FrameworkAutoConfiguration.EventProperties properties, ApplicationContext applicationContext) {
        return new RedisEventPubSub<>(builder, objectMapper, properties, applicationContext);
    }

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplateBuilder redisTemplateBuilder(JsonSerializationRedisSerializer eventSerializer, RedisConnectionFactory connectionFactory) {
        return new RedisTemplateBuilder<>(eventSerializer, connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonSerializationRedisSerializer jsonSerializationRedisSerializer(ObjectMapper objectMapper) {
        return new JsonSerializationRedisSerializer(objectMapper);
    }
}
