package com.example.jobs.cache;

import com.example.jobs.dto.JobDetailResponse;
import com.example.jobs.dto.JobSyncDto;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class JobCacheJavaService {
  private final ReactiveRedisTemplate<String,Object> reactiveRedisTemplate;
  private final ObjectMapper objectMapper;

  public Mono<JobDetailResponse> getJob(UUID id){
    return reactiveRedisTemplate.opsForValue()
        .get(cacheKey(id))
        .map(rawData -> objectMapper.convertValue(rawData, JobDetailResponse.class))
        .onErrorResume(_ -> Mono.empty());
  }

  public Mono<Void> saveJob(JobDetailResponse dto){
    return reactiveRedisTemplate.opsForValue()
        .set(cacheKey(dto.id()),dto, Duration.ofHours(2))
        .then();
  }


  public Mono<Void> evictJob(UUID id){
    return reactiveRedisTemplate.delete(cacheKey(id)).then();
  }

  public Mono<Void> saveJob(JobSyncDto dto, String companySlug, String companyName, String companyLogoUrl){
    var detail = new JobDetailResponse(
        dto.id(),
        dto.title(),
        dto.slug(),
        dto.description(),
        dto.salaryMin(),
        dto.salaryMax(),
        dto.location(),
        dto.workplace(),
        dto.jobType(),
        dto.experienceLevel(),
        dto.tags(),
        companySlug,
        companyName,
        companyLogoUrl,
        dto.qualityScore(),
        dto.createdAt(),
        dto.applyUrl(),
        dto.isFeatured(),
        dto.isSticky(),
        dto.status(),
        dto.url()
    );
        return reactiveRedisTemplate.opsForValue()
            .set(cacheKey(dto.id()),detail,Duration.ofHours(2))
            .then();
  }

  private String cacheKey(UUID id){
    return "job:"+id;
  }
}
