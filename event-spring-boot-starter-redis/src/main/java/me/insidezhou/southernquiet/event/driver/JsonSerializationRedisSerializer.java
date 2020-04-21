package me.insidezhou.southernquiet.event.driver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class JsonSerializationRedisSerializer<T extends Serializable> implements RedisSerializer<T> {
    private final ObjectMapper objectMapper;

    public JsonSerializationRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(T o) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(o);
        }
        catch (JsonProcessingException e) {
            throw new SerializationException("将对象序列化为JSON数据时异常", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        Map map;

        try {
            map = objectMapper.readValue(bytes, Map.class);
        }
        catch (IOException e) {
            throw new SerializationException("将JSON数据反序列化为对象时异常", e);
        }

        String typeId = (String) map.get(RedisEventPubSub.EventTypeIdName);
        if (StringUtils.isEmpty(typeId)) return null;

        Class<T> eventClass = (Class<T>) AbstractEventPubSub.EventTypeMap.get(typeId);
        if (null == eventClass) return null;

        return objectMapper.convertValue(map, eventClass);
    }
}
