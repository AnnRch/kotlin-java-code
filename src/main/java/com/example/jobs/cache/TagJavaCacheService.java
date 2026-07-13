package com.example.jobs.cache;

import com.example.jobs.dto.Tag;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TagJavaCacheService {

  private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
  private final ObjectMapper objectMapper;
  private String listKey = "tags:all";

  public Mono<Void> evictTag(String name) {
    return reactiveRedisTemplate.delete(itemKey(name))
        .then(evictAllTagsCollection());
  }

  public Flux<Tag> getAllTagsCollection() {
    return reactiveRedisTemplate.opsForValue()
        .get(listKey)
        .flatMapMany(rawData -> {
          if (rawData instanceof List<?>) {
            var targetType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, Tag.class);
            List<Tag> tags = objectMapper.convertValue(rawData, targetType);

            return Flux.fromIterable(tags);
          }

          return Flux.empty();
        })
        .switchIfEmpty(Flux.empty());
  }

  public Mono<Void> saveAllTagsCollection(List<Tag> tags) {
    return reactiveRedisTemplate.opsForValue()
        .set(listKey, tags, Duration.ofHours(2))
        .then();
  }

  public Mono<Void> saveTag(Tag tag) {
    return reactiveRedisTemplate.opsForValue()
        .set(itemKey(tag.getName()), tag, Duration.ofHours(2))
        .then(evictAllTagsCollection());
  }

  public Mono<Void> evictAllTagsCollection() {
    return reactiveRedisTemplate.delete(listKey).then();
  }

  public Mono<Tag> getTagByName(String name) {
    return reactiveRedisTemplate.opsForValue()
        .get(itemKey(name))
        .map(rawData -> objectMapper.convertValue(rawData, Tag.class))
        .onErrorResume(_ -> Mono.empty());
  }

  private String itemKey(String id) {
    return "tag:name:" + id;
  }

}

