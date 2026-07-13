package com.example.jobs.service;

import com.example.jobs.utils.DbUtils;
import com.example.jobs.client.AiDevBoardWebClient;
import com.example.jobs.dto.CompanyDetailsDto;
import com.example.jobs.dto.CompanyDto;
import com.example.jobs.dto.CompanyProfileResponse;
import com.example.jobs.dto.JobDetailResponse;
import com.example.jobs.dto.JobDetails;
import com.example.jobs.dto.JobSyncDto;
import com.example.jobs.entity.Company;
import com.example.jobs.entity.Job;
import com.example.jobs.mapper.JobMapper;
import com.example.jobs.mapper.Mapper;
import com.example.jobs.repository.CompanyJavaRepository;
import com.example.jobs.repository.JobJavaRepository;
import com.example.jobs.cache.CompanyCacheJavaService;
import com.example.jobs.cache.JobCacheJavaService;
import com.example.jobs.cache.TagJavaCacheService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSyncJavaService {

  private final JobJavaRepository jobJavaRepository;
  private final DatabaseClient databaseClient;
  private final CompanyJavaRepository companyJavaRepository;
  private final JobCacheJavaService jobCacheJavaService;
  private final TagJavaCacheService tagJavaCacheService;
  private final CompanyCacheJavaService companyCacheJavaService;
  private final JobMapper jobMapper;
  private final Mapper mapper;
  private final AiDevBoardWebClient webClient;

  public Mono<CompanyProfileResponse> syncCompanyJobs(String slug) {
    return webClient.getCompanyProfile(slug)
        .flatMap(response -> {
          var incomingDetails = response.company();
          var incomingJobs = response.jobs() != null ? response.jobs() : List.<JobSyncDto>of();
          var incomingCompanyDto = new CompanyDto(
              incomingDetails.getSlug(),
              incomingDetails.getName(),
              incomingDetails.getJobs(),
              incomingDetails.getWebsite(),
              incomingDetails.getLogoUrl(),
              incomingDetails.getSalaryCount(),
              incomingDetails.getAvgSalary()
          );
          return companyCacheJavaService.getCompany(slug)
              .flatMap(cachedCompany -> {
                boolean isFresh = !mapper.hasChanged(cachedCompany, incomingCompanyDto);
                return companyJavaRepository.existsBySlug(slug)
                    .map(exists -> isFresh && exists);
              })
              .switchIfEmpty(Mono.just(false))
              .flatMap(isFreshAndExists -> {
                if (isFreshAndExists) {
                  log.info("Company '{}' is fresh. Short-circuiting.", slug);
                  return Mono.just(response);
                }
                return performFullSync(slug, response, incomingDetails, incomingJobs,
                    incomingCompanyDto)
                    .doOnError(e -> log.error("Error during full sync for '{}': {}", slug,
                        e.getMessage()));
              });
        })
        .onErrorResume(error -> {
          log.error("Failed synchronization pipeline execution for slug '{}'.", slug, error);
          return Mono.error(error);
        });
  }


  public Flux<JobDetailResponse> fetchJobs() {
    return webClient.fetchRecentJobs()
        .flatMapMany(response -> {
          log.info("Fetched {} jobs from upstream API", response.jobs().size());
          return Flux.fromIterable(response.jobs());
        })
        .limitRate(100)
        .flatMap(jobDetails ->
            jobCacheJavaService.getJob(jobDetails.id())
                .flatMap(cachedJob -> {
                  if (cachedJob != null) {
                    return Mono.just(cachedJob);
                  } else {
                    return processCompanyUpsert(jobDetails)
                        .flatMap(companyEntity -> {
                          var detailResponse = jobMapper.toDetailResponse(jobDetails);
                          var entity = jobMapper.toJobEntityFromDetail(detailResponse,
                              companyEntity, true);

                          return upsertJobRelational(entity, companyEntity.getId())
                              .flatMap(_ -> {
                                if (jobDetails.tags() != null && !jobDetails.tags().isEmpty()) {
                                  return syncJobTagsBatch(jobDetails.id(), jobDetails.tags());
                                }
                                return Mono.empty();
                              })
                              .then(performAsyncCacheUpdates(detailResponse))
                              .thenReturn(detailResponse);
                        })
                        .onErrorResume(error -> {
                          log.error("Error processing job {}: {}", jobDetails.id(),
                              error.getMessage());
                          return Mono.empty();
                        });
                  }
                }), 10);
  }

  public Mono<JobDetailResponse> syncSpecificJob(UUID id) {
    log.info("Starting targeted sync pipeline for Job ID: '{}'", id);
    return jobCacheJavaService.getJob(id)
        .flatMap(cachedJob -> {
          log.info(
              "Cache hit for Job ID '{}' in Redis. Short-circuiting pipeline execution.", id);
          return Mono.just(cachedJob);
        })
        .switchIfEmpty(Mono.defer(() -> {
          log.info("Cache miss for Job ID '{}'. Fetching live data from upstream...", id);
          return webClient.getJobDetail(id)
              .flatMap(response -> {
                log.info(
                    "Successfully retrieved job detail from upstream for slug: '{}'. Parent company: '{}'",
                    response.slug(),
                    response.companySlug());
                return processCompanyRelation(response)
                    .flatMap(companyEntity -> processJobRelation(response, companyEntity)
                        .then(asyncCacheUpdates(response)))
                    .thenReturn(response);
              })
              .onErrorResume(error -> {
                log.error("Failed targeted sync pipeline execution for job ID [{}]. Error: {}", id,
                    error.getMessage());
                return Mono.empty();
              });
        }));
  }

  private Mono<CompanyProfileResponse> performFullSync(String slug, CompanyProfileResponse response,
      CompanyDetailsDto incomingDetails, List<JobSyncDto> jobs,
      CompanyDto dto) {
    return refreshCompanyCache(slug, incomingDetails, dto)
        .then(syncCompanyEntity(slug, incomingDetails))
        .flatMap(dbCompany -> {
          if (dbCompany.getId() == null) {
            return Mono.error(
                new IllegalStateException("Failed to assign primary ID for: " + slug));
          }

          return utilizeStaleJobs(dbCompany.getId(), slug, jobs)
              .flatMap(shouldContinue -> {
                if (!shouldContinue) {
                  return Mono.just(response);
                }
                return processActiveJobsPipeline(jobs, dbCompany, dbCompany.getId())
                    .then(Mono.just(response));
              });
        });
  }

  private Mono<Void> performAsyncCacheUpdates(JobDetailResponse response) {
    return Mono.fromRunnable(() -> {
          try {
            jobCacheJavaService.saveJob(response);
            tagJavaCacheService.evictAllTagsCollection();
          } catch (Exception e) {
            log.warn("Cache update failed: {}", e.getMessage());
          }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  private Mono<Company> processCompanyRelation(JobDetailResponse response) {
    return companyJavaRepository.findBySlug(response.slug())
        .switchIfEmpty(Mono.fromCallable(() -> {
          log.info("Parent company '{}' not found in DB. Initializing new company record.",
              response.companySlug());
          Company company = new Company();
          company.setSlug(response.slug());
          company.setName(response.companyName());
          company.setLogoUrl(response.companyLogoUrl());
          return company;
        }))
        .flatMap(company -> {
          log.debug(
              "Found existing parent company record for slug '{}' with ID: {}",
              response.companySlug(), company.getId());
          return companyJavaRepository.save(company);
        });
  }

  private Mono<Void> processJobRelation(JobDetailResponse response, Company companyEntity) {
    return jobJavaRepository.findById(response.id())
        .map(existingJob -> {
          log.info("Job ID '{}' already exists in DB. Preparing SQL UPDATE pipeline.",
              response.id());
          return false;
        })
        .switchIfEmpty(Mono.fromCallable(() -> {
          log.info("Job ID '{}' is unknown to relational store. Preparing SQL INSERT pipeline.",
              response.id());
          return true;
        }))
        .flatMap(isNewRecord -> {
          var entity = jobMapper.toJobEntityFromDetail(response, companyEntity, isNewRecord);
          entity.setIsNewRecord(isNewRecord);
          return jobJavaRepository.save(entity)
              .doOnNext(_ -> log.info(
                  "Successfully persisted job metadata to relational store for ID: '{}'",
                  response.id()))
              .then(Mono.defer(() -> {
                if (response.tags() != null && !response.tags().isEmpty()) {
                  log.info("Synchronizing {} relational tags for Job ID: '{}'",
                      response.tags().size(), response.id());
                  return syncJobTagsBatch(response.id(), response.tags());
                } else {
                  log.debug(
                      "No tags attached to incoming job record for ID: '{}'. Skipping tag sync.",
                      response.id());
                  return Mono.empty();
                }
              }));
        })
        .then();
  }

  private Mono<Void> asyncCacheUpdates(JobDetailResponse response) {
    log.info("Dispatching asynchronous cache updates for Job ID: '{}'", response.id());
    return jobCacheJavaService.saveJob(response)
        .doOnSuccess(_ -> log.info("Asynchronously refreshed Redis cache layer for Job ID: '{}'",
            response.applyUrl()))
        .doOnError(error -> log.error("Async Redis sync failed for Job ID '{}': {}", response.id(),
            error.getMessage()))
        .flatMap(item ->
            tagJavaCacheService.evictAllTagsCollection()
                .doOnSuccess(_ -> log.info("Evicted tag collections cache safely."))
                .doOnError(
                    error -> log.error("Async tag cache eviction failed: {}", error.getMessage()))
        )
        .then();
  }

  private Mono<Void> refreshCompanyCache(String slug, CompanyDetailsDto incomingDetails,
      CompanyDto dto) {
    log.info(
        "Company cache missing, stale, or DB dropped for '{}'. Refreshing cache layer.",
        slug
    );
    return companyCacheJavaService.saveCompany(incomingDetails)
        .doOnSuccess(_ -> log.info("company dto : {}", dto))
        .doOnError(error -> log.error("some issue has been occurred: {}", error.getMessage()))
        .then();
  }

  private Mono<Company> syncCompanyEntity(String slug, CompanyDetailsDto incomingDetails) {
    return companyJavaRepository.findBySlug(slug)
        .flatMap(dbCompany -> {
          dbCompany.setSlug(incomingDetails.getSlug());
          dbCompany.setName(incomingDetails.getName());
          dbCompany.setWebsite(incomingDetails.getWebsite());
          dbCompany.setLogoUrl(incomingDetails.getLogoUrl());
          dbCompany.setTotalJobs(incomingDetails.getJobs());
          dbCompany.setAvgSalaryAll(incomingDetails.getAvgSalary());
          dbCompany.setSalaryCountAll(incomingDetails.getSalaryCount());
          dbCompany.setClicks(incomingDetails.getClicks());
          dbCompany.setViews(incomingDetails.getViews());
          dbCompany.setViews7d(incomingDetails.getViews7d());
          log.info("Saving company entity to PostgreSQL. Current ID: {}", dbCompany.getId());
          return companyJavaRepository.save(dbCompany);
        });
  }

  private Mono<Boolean> utilizeStaleJobs(Long companyId, String slug,
      List<JobSyncDto> incomingJobs) {
    if (incomingJobs.isEmpty()) {
      log.info("No active jobs sent for '{}'. Clearing company database footprint.", slug);
      return jobJavaRepository.deleteByCompanyId(companyId)
          .thenReturn(false);
    }

    Set<UUID> upstreamJobIds = incomingJobs.stream()
        .map(JobSyncDto::id)
        .collect(Collectors.toSet());

    return jobJavaRepository.findAllIdsByCompanyId(companyId)
        .collect(Collectors.toSet())
        .flatMap(existingDbJobIds -> {
          Set<UUID> jobsToDelete = existingDbJobIds.stream()
              .filter(id -> !upstreamJobIds.contains(id))
              .collect(Collectors.toSet());

          if (jobsToDelete.isEmpty()) {
            return Mono.just(true);
          }

          log.info("Detected {} stale jobs for company ID {}. Evicting...", jobsToDelete.size(),
              companyId);

          return jobJavaRepository.deleteAllByIdIn(jobsToDelete)
              .then(
                  Flux.fromIterable(jobsToDelete)
                      .parallel()
                      .runOn(Schedulers.boundedElastic())
                      .doOnNext(jobId -> {
                        try {
                          jobCacheJavaService.evictJob(jobId);
                        } catch (Exception e) {
                          log.error("Cache error", e);
                        }
                      })
                      .sequential()
                      .then(Mono.just(true))
              );
        });

  }

  private Flux<JobDetailResponse> processActiveJobsPipeline(
      List<JobSyncDto> incomingJobs,
      Company dbCompany,
      Long companyId
  ) {
    return Flux.fromIterable(incomingJobs)
        .flatMap(jobDetails ->
                jobCacheJavaService.getJob(jobDetails.id())
                    .flatMap(Mono::just)
                    .switchIfEmpty(Mono.defer(() ->
                        jobJavaRepository.existsById(jobDetails.id())
                            .flatMap(existsInDb -> {
                              var detailResponse = jobMapper.toDetailResponse(jobDetails, dbCompany);
                              var entity = mapper.toJobEntityFromDetail(detailResponse, dbCompany);
                              entity.setNew(!existsInDb);

                              return upsertJobRelational(entity, companyId)
                                  .then(Mono.defer(() -> {
                                    if (jobDetails.tags() != null && !jobDetails.tags().isEmpty()) {
                                      return syncJobTagsBatch(jobDetails.id(), jobDetails.tags());
                                    }
                                    return Mono.empty();
                                  }))
                                  .doOnSuccess(v -> triggerBackgroundCacheUpdates(detailResponse))
                                  .thenReturn(detailResponse);
                            })
                    ))
                    .onErrorResume(e -> {
                      log.error("Error processing job {}: {}", jobDetails.id(), e.getMessage());
                      return Mono.empty();
                    }),
            10
        );
  }

  private void triggerBackgroundCacheUpdates(JobDetailResponse detailResponse) {
    Mono.fromRunnable(() -> {
      try {
        jobCacheJavaService.saveJob(detailResponse);
      } catch (Exception ignored) {
      }
      try {
        tagJavaCacheService.evictAllTagsCollection();
      } catch (Exception ignored) {
      }
    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
  }

  private Mono<Void> processTags(JobSyncDto jobDetails) {
    if (jobDetails.tags() == null || jobDetails.tags().isEmpty()) {
      return Mono.empty();
    }
    return syncJobTagsBatch(jobDetails.id(), jobDetails.tags());
  }

  private Mono<Void> upsertJobRelational(Job entity, Long companyId) {
    String sqlQuery = """
            INSERT INTO public.jobs (...) 
            VALUES (...)
            ON CONFLICT (id) DO UPDATE SET ...
        """;

    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sqlQuery)
        .bind("id", entity.getId() != null ? entity.getId().toString() : "")
        .bind("companyId", companyId)
        .bind("title", entity.getTitle())
        .bind("slug", entity.getSlug())
        .bind("isFeatured", entity.getIsFeatured() != null ? entity.getIsFeatured() : false)
        .bind("isSticky", entity.getIsSticky() != null ? entity.getIsSticky() : false);

    spec = DbUtils.bindNullable(spec, "description", entity.getDescription(), String.class);
    spec = DbUtils.bindNullable(spec, "location", entity.getLocation(), String.class);
    spec = DbUtils.bindNullable(spec, "workplace", entity.getWorkplace(), String.class);
    spec = DbUtils.bindNullable(spec, "jobType", entity.getJobType(), String.class);
    spec = DbUtils.bindNullable(spec, "experienceLevel", entity.getExperienceLevel(), String.class);
    spec = DbUtils.bindNullable(spec, "salaryMin", entity.getSalaryMin(),
        java.math.BigDecimal.class);
    spec = DbUtils.bindNullable(spec, "salaryMax", entity.getSalaryMax(),
        java.math.BigDecimal.class);
    spec = DbUtils.bindNullable(spec, "qualityScore", entity.getQualityScore(), Integer.class);
    spec = DbUtils.bindNullable(spec, "status", entity.getStatus(), String.class);
    spec = DbUtils.bindNullable(spec, "applyUrl", entity.getApplyUrl(), String.class);
    spec = DbUtils.bindNullable(spec, "url", entity.getUrl(), String.class);
    spec = DbUtils.bindNullable(spec, "publishedAt", entity.getPublishedAt(),
        java.time.LocalDateTime.class);
    spec = DbUtils.bindNullable(spec, "expiresAt", entity.getExpiresAt(),
        java.time.LocalDateTime.class);
    spec = DbUtils.bindNullable(spec, "createdAt",
        entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now(),
        LocalDateTime.class);
    spec = DbUtils.bindNullable(spec, "updatedAt",
        entity.getUpdatedAt() != null ? entity.getUpdatedAt() : LocalDateTime.now(),
        LocalDateTime.class);

    return spec.then()
        .doOnError(e -> log.error("Relational sync upsert hit a block on ID [{}]: {}",
            entity.getId(), e.getMessage(), e));

  }

  private Mono<Company> processCompanyUpsert(JobDetails jobDetails) {
    if (jobDetails == null || jobDetails.slug() == null) {
      return Mono.empty();
    }

    return databaseClient.sql("""
            INSERT INTO "companies" ("slug", "name", "logo_url", "total_jobs")
            VALUES (:slug, :name, :logoUrl, 1)
            ON CONFLICT ("slug") DO UPDATE 
            SET "total_jobs" = "companies"."total_jobs" + 1
            RETURNING *
            """)
        .bind("slug", jobDetails.slug())
        .bind("name", jobDetails.companyName())
        .bind("logoUrl", jobDetails.companyLogoUrl())
        .map((row, rowMetadata) -> {
          Company c = new Company();
          c.setId(row.get("id", Long.class));
          c.setSlug(row.get("slug", String.class));
          c.setName(row.get("name", String.class));
          c.setTotalJobs(row.get("total_jobs", Integer.class));
          return c;
        })
        .one();
  }

  private Mono<Void> syncJobTagsBatch(UUID jobId, List<String> tagNames) {
    var uniqueTags = tagNames.stream().distinct()
        .filter(it -> !it.isBlank())
        .toList();

    if (uniqueTags == null) {
      return Mono.empty();
    }

    return Flux.fromIterable(uniqueTags)
        .flatMap(name ->
            databaseClient.sql("""
                    INSERT INTO "tags" ("name", "job_count")
                    VALUES (:name, 0)
                    ON CONFLICT (name)
                    DO UPDATE SET "name" = EXCLUDED."name"
                    RETURNING tag_id
                    """)
                .bind("name", name)
                .map(row -> Objects.requireNonNull(row.get("tag_id", Integer.class)))
                .one()
                .flatMap(tagId -> databaseClient.sql("""
                        INSERT INTO "job_tags" ("job_id", "tag_id")
                        VALUES (CAST(:jobId AS uuid), :tagId)
                        ON CONFLICT (job_id, tag_id)
                        DO NOTHING
                        """)
                    .bind("jobId", jobId.toString())
                    .bind("tagId", tagId)
                    .fetch()
                    .rowsUpdated()
                    .flatMap(rowsUpdated -> {
                      if (rowsUpdated > 0) {
                        return databaseClient.sql("""
                                UPDATE "tags"
                                SET "job_count" = "job_count" + 1
                                WHERE "tag_id" = :tagId
                                """)
                            .bind("tagId", tagId)
                            .then();
                      }
                      return Mono.empty();
                    })
                )
                .onErrorResume(error -> {
                  log.error("Failed handling tag link for [{}] on job [{}]: {}", name, jobId,
                      error.getMessage());
                  return Mono.empty();
                })
        )
        .then();
  }
}
