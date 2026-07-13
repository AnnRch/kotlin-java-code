package com.example.jobs.client;

import com.example.jobs.dto.CompanyProfileResponse;
import com.example.jobs.dto.CompanyRegisterRequest;
import com.example.jobs.dto.CompanyRegisterResponse;
import com.example.jobs.dto.CompanyResponse;
import com.example.jobs.dto.DeveloperRegisterResponse;
import com.example.jobs.dto.JobDetailResponse;
import com.example.jobs.dto.JobResponse;
import com.example.jobs.dto.RegisterDeveloperRequest;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component("aiDevBoardClientComponent")
@RequiredArgsConstructor
public class AiDevBoardWebClient {

  private final WebClient webClient;
  private final Retry retryStrategy = Retry.backoff(3, Duration.ofSeconds(2));

  public Mono<CompanyResponse> getAllCompanies() {
    return webClient.get()
        .uri("/companies")
        .retrieve()
        .bodyToMono(CompanyResponse.class)
        .retryWhen(retryStrategy);
  }

  public Mono<CompanyProfileResponse> getCompanyProfile(String slug) {
    return webClient.get()
        .uri("/companies/{slug}", slug)
        .retrieve()
        .bodyToMono(CompanyProfileResponse.class)
        .retryWhen(retryStrategy);
  }

  public Mono<JobDetailResponse> getJobDetail(UUID id) {
    return webClient.get()
        .uri("/jobs/{id}", id)
        .retrieve()
        .bodyToMono(JobDetailResponse.class)
        .retryWhen(retryStrategy)
        .onErrorResume(error -> {
          if (error instanceof WebClientResponseException) {
            log.error("HTTP Status Code: ${error.statusCode}");
            log.error("Response Content: ${error.responseBodyAsString}");
            log.error("---------------------------------");
          } else {
            log.error("Direct network/connection error: ${error.message}");
          }
          return Mono.empty();
        });
  }

  public Mono<JobResponse> fetchRecentJobs() {
    return webClient.get()
        .uri("/jobs")
        .retrieve()
        .bodyToMono(JobResponse.class)
        .retryWhen(retryStrategy);
  }

  public Mono<CompanyRegisterResponse> registerCompany(CompanyRegisterRequest request) {
    return webClient.post()
        .uri("/register/company")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(CompanyRegisterResponse.class)
        .retryWhen(retryStrategy);
  }

  public Mono<DeveloperRegisterResponse> registerDeveloper(RegisterDeveloperRequest request) {
    return webClient.post()
        .uri("/register/developer")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(DeveloperRegisterResponse.class)
        .retryWhen(retryStrategy);
  }


  public Mono<Void> updateCompanyOnRemote(String slug, String apiKey,
      Map<String, Object> updateMap) {
    return webClient.patch()
        .uri("/companies/{slug}", slug)
        .header("Authorization", "Bearer " + apiKey)
        .bodyValue(updateMap)
        .retrieve()
        .bodyToMono(Void.class)
        .retryWhen(retryStrategy);
  }
}
