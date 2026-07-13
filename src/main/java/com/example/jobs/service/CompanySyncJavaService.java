package com.example.jobs.service;

import com.example.jobs.client.AiDevBoardWebClient;
import com.example.jobs.dto.CompanyDto;
import com.example.jobs.dto.CompanyRegisterRequest;
import com.example.jobs.dto.CompanyRegisterResponse;
import com.example.jobs.dto.CompanyResponse;
import com.example.jobs.entity.Company;
import com.example.jobs.mapper.Mapper;
import com.example.jobs.repository.CompanyJavaRepository;
import com.example.jobs.cache.CompanyCacheJavaService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySyncJavaService {

  private final CompanyJavaRepository companyJavaRepository;
  private final CompanyCacheJavaService companyCacheJavaService;
  private final AiDevBoardWebClient webClient;
  private final CompanyJavaService companyJavaService;
  private final Mapper mapper;

  public Mono<CompanyResponse> syncAllCompaniesOverviews(){
    return webClient.getAllCompanies()
        .doOnNext(r -> log.info("Fetched {} companies from upstream API", r.getCompanies() != null ? r.getCompanies().size() : 0))
        .flatMap(response -> {
          if (response.getCompanies() == null || response.getCompanies().isEmpty()) {
            return Mono.just(response);
          }

          return Flux.fromIterable(response.getCompanies())
              .distinct(CompanyDto::getSlug)
              .flatMap(this::processIncomingData, 10)
              .then()
              .thenReturn(response);
        })
        .onErrorResume(error -> {
          log.error("Pipeline Failure during overview sync: {}", error.getMessage());
          return Mono.empty();
        })
        .doOnSuccess(_ -> log.info("Pipeline completed all items processing successfully."));
  }

  public Mono<CompanyRegisterResponse> registerAndSaveCompany(CompanyRegisterRequest request) {
    return webClient.registerCompany(request)
        .flatMap(response -> {
          if (response.getDto() == null) {
            return Mono.just(response);
          }
          var remoteCompany = response.getDto();
          return companyJavaRepository.findBySlug(response.getDto().getSlug())
              .switchIfEmpty(Mono.fromSupplier(() ->
                  Company.builder()
                      .slug(remoteCompany.getSlug())
                      .build()
              ))
              .map(entity -> {
                entity.setName(remoteCompany.getName());
                entity.setWebsite(remoteCompany.getWebsite());
                if (remoteCompany.getApiKey() != null && !remoteCompany.getApiKey()
                    .isBlank()) {
                  entity.setApiKey(remoteCompany.getApiKey());
                }
                return entity;
              })
              .flatMap(companyJavaRepository::save)
              .thenReturn(response);
        });
  }


  public Mono<Void> processIncomingData(CompanyDto dto) {
    return companyCacheJavaService.getCompany(dto.getSlug())
        .map(cached -> mapper.hasChanged(cached, dto) ? "UPDATE" : "SKIP")
        .defaultIfEmpty("MISS")
        .flatMap(action -> {
          log.info("Action for {}: {}", dto.getSlug(), action);
          if ("UPDATE".equals(action) || "MISS".equals(action)) {
            return performUpdate(dto);
          }
          return Mono.empty();
        })
        .then();
  }

  public Mono<Void> updateCompanyProfile(String slug, String name, String webSite, String logoUrl){
      return companyJavaRepository.findBySlug(slug)
          .onErrorResume(error -> {
                log.error("Company profile not tracked locally {}",error.getMessage());
                return Mono.empty();
              })
          .flatMap(entity -> {
              if (entity.getApiKey() == null || entity.getApiKey().isBlank()) {
                  return Mono.error(new IllegalStateException("No API credentials found for this slug. Register company first."));
              }
              Map<String, Object> updatePayload = new HashMap<>();
              updatePayload.put("name", name);
              updatePayload.put("website", webSite);
              updatePayload.put("logoUrl", logoUrl);

              return webClient.updateCompanyOnRemote(slug, entity.getApiKey(), updatePayload);
          });
  }

  private Mono<Void> performUpdate(CompanyDto dto) {
    return companyJavaService.updateCompany(dto)
        .doOnNext(_ -> log.info("Successfully updated {}", dto.getSlug()))
        .flatMap(_ -> companyCacheJavaService.saveCompany(dto)
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(error -> {
              log.error("Cache failure for {}", dto.getSlug(), error);
              return Mono.empty();
            })
        )
        .then();
  }

}
