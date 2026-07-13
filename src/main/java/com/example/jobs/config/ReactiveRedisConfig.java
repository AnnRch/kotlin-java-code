package com.example.jobs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ReactiveRedisConfig {

  public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
      ObjectMapper objectMapper
  ) {
    RedisSerializer<String> keySerializer = new StringRedisSerializer();
    GenericJacksonJsonRedisSerializer valueSerializer = new GenericJacksonJsonRedisSerializer(
        objectMapper);

    RedisSerializationContext.SerializationPair<String> keyPair =
        RedisSerializationContext.SerializationPair.fromSerializer(keySerializer);
    RedisSerializationContext.SerializationPair<Object> valuePair =
        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer);

    RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
        .<String, Object>newSerializationContext()
        .key(keyPair)
        .value(valuePair)
        .hashKey(keyPair)
        .hashValue(valuePair)
        .build();

    return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
  }

}
