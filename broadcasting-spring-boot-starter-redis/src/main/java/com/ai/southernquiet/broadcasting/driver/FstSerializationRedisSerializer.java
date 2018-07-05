package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.util.SerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@SuppressWarnings("NullableProblems")
public class FstSerializationRedisSerializer implements RedisSerializer<Object> {
    @Override
    public byte[] serialize(Object o) throws SerializationException {
        return SerializationUtils.serialize(o);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        return SerializationUtils.deserialize(bytes);
    }
}
