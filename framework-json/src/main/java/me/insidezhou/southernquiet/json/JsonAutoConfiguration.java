package me.insidezhou.southernquiet.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Set;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringFacetCodeInspection"})
@Configuration
@ConditionalOnBean(ObjectMapper.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class JsonAutoConfiguration {
    public JsonAutoConfiguration(ObjectMapper objectMapper) {
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {
            @Override
            public JsonProvider jsonProvider() {
                return new JacksonJsonProvider(objectMapper);
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }

            @Override
            public MappingProvider mappingProvider() {
                return new JacksonMappingProvider(objectMapper);
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public static JsonReader jsonReader(ObjectMapper objectMapper) {
        return new JsonReader() {
            @Override
            public <T> T read(InputStream inputStream, Class<T> cls) {
                try {
                    return objectMapper.readValue(inputStream, cls);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public static JsonWriter jsonWriter(ObjectMapper objectMapper) {
        return object -> {
            try {
                return objectMapper.writeValueAsBytes(object);
            }
            catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
