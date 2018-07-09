package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.broadcasting.SerializableEvent;
import com.ai.southernquiet.util.SerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@SuppressWarnings("NullableProblems")
public class FstSerializationRedisSerializer implements RedisSerializer<SerializableEvent> {
    @Override
    public byte[] serialize(SerializableEvent o) throws SerializationException {
        return SerializationUtils.serialize(o);
    }

    @Override
    public SerializableEvent deserialize(byte[] bytes) throws SerializationException {
        return (SerializableEvent) SerializationUtils.deserialize(bytes);
    }
}
