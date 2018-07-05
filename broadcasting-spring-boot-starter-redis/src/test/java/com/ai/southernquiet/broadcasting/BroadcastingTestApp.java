package com.ai.southernquiet.broadcasting;

import com.ai.southernquiet.broadcasting.driver.FstSerializationRedisSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

@SuppressWarnings({"unchecked", "ConstantConditions"})
@SpringBootApplication
public class BroadcastingTestApp {
    private static Log log = LogFactory.getLog(BroadcastingTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(BroadcastingTestApp.class);
    }

    @Bean
    public static RedisMessageListenerContainer redisMessageListenerContainer(
        RedisTemplate redisTemplate,
        RedisConnectionFactory connectionFactory,
        FstSerializationRedisSerializer fstSerializationRedisSerializer) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        RedisSerializer stringRedisSerializer = redisTemplate.getStringSerializer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener((message, pattern) -> {
            log.info(stringRedisSerializer.deserialize(pattern));
            log.info(stringRedisSerializer.deserialize(message.getChannel()));

            Object event = fstSerializationRedisSerializer.deserialize(message.getBody());
            if (event instanceof BroadcastingDone) {
                BroadcastingDone broadcastingDone = (BroadcastingDone) event;
                log.info(broadcastingDone.getId());
            }

        }, new ChannelTopic("public"));

        return container;
    }
}
