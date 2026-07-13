package com.example.jobs.cache;

import com.example.jobs.dto.DeveloperRegisterResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DeveloperCacheJavaService {

  private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
  private final ObjectMapper objectMapper;

  private String cacheKey(UUID id) {
    return "developer:" + id;
  }

  public Mono<DeveloperRegisterResponse> getDeveloper(UUID id) {
    return reactiveRedisTemplate.opsForValue()
        .get(cacheKey(id))
        .map(rawData -> objectMapper.convertValue(rawData, DeveloperRegisterResponse.class))
        .onErrorResume(error -> Mono.empty());
  }

  public Mono<Void> saveDeveloper(UUID id, DeveloperRegisterResponse response) {
    return reactiveRedisTemplate.opsForValue()
        .set(cacheKey(id), response, Duration.ofHours(2))
        .then();
  }

  public Mono<Void> evictDeveloper(UUID id) {
    return reactiveRedisTemplate
        .delete(cacheKey(id))
        .then();
  }

}
