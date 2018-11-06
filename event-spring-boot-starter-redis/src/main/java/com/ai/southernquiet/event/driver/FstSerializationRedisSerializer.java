package com.ai.southernquiet.event.driver;

import com.ai.southernquiet.util.SerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.Serializable;

@SuppressWarnings("NullableProblems")
public class FstSerializationRedisSerializer<T extends Serializable> implements RedisSerializer<T> {
    @Override
    public byte[] serialize(T o) throws SerializationException {
        return SerializationUtils.serialize(o);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        return (T) SerializationUtils.deserialize(bytes);
    }
}
