package com.example.jobs.cache;

import com.example.jobs.dto.CompanyDetailsDto;
import com.example.jobs.dto.CompanyDto;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class CompanyCacheJavaService {
  private final ReactiveRedisTemplate<String,Object> reactiveRedisTemplate;
  private final ObjectMapper objectMapper;

  public Mono<CompanyDto> getCompany(String slug){
    return reactiveRedisTemplate.opsForValue()
        .get("company:" + slug)
        .flatMap(rawData -> rawData instanceof Map<?,?>
            ? Mono.fromCallable(() -> objectMapper.convertValue(rawData, CompanyDto.class))
            : rawData instanceof CompanyDto
            ? Mono.justOrEmpty((CompanyDto) rawData)
            : Mono.empty());
  }

  public Mono<Void> saveCompany(CompanyDto dto){
    return reactiveRedisTemplate.opsForValue()
        .set("company:" + dto.getSlug(), dto, Duration.ofHours(2))
        .then();
  }

  public Mono<Void> saveCompany(CompanyDetailsDto details){
    CompanyDto dto = CompanyDto.builder()
        .slug(details.getSlug())
        .name(details.getName())
        .jobs(details.getJobs())
        .website(details.getWebsite())
        .logoUrl(details.getLogoUrl())
        .salaryCount(details.getSalaryCount())
        .avgSalary(details.getAvgSalary())
        .build();

   return reactiveRedisTemplate.opsForValue()
        .set("company:" + dto.getSlug(), dto, Duration.ofHours(2))
        .then();
  }

}
