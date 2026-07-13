package com.example.jobs.service;

import com.example.jobs.client.AiDevBoardWebClient;
import com.example.jobs.dto.DeveloperRegisterResponse;
import com.example.jobs.dto.RegisterDeveloperRequest;
import com.example.jobs.dto.RegisteredDeveloperDto;
import com.example.jobs.mapper.Mapper;
import com.example.jobs.repository.DeveloperJavaRepository;
import com.example.jobs.cache.DeveloperCacheJavaService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeveloperJavaService {
  private final DeveloperJavaRepository developerRepository;
  private final DeveloperCacheJavaService developerCacheService;
  private final AiDevBoardWebClient aiDevBoardClient;
  private final DatabaseClient databaseClient;
  private final Mapper mapper;


  public Mono<DeveloperRegisterResponse> getDeveloperProfile(UUID id){
    return developerCacheService.getDeveloper(id)
        .onErrorResume(error -> {
          log.warn("Non-fatal Redis cache read failure for developer ID '{}': {}", id, error.getMessage());
          return Mono.empty();
        })
        .switchIfEmpty(
            developerRepository.findById(id)
            .switchIfEmpty(Mono.error(new NoSuchElementException("Developer profile not found.")))
            .flatMap(entity -> fetchPermissions(id)
                .map(permissions -> {
                  entity.setPermissions(permissions);
                  return entity;
                })
            )
            .map(mapper::mapEntityToResponse)
                .flatMap(mappedResponse ->
                    developerCacheService.saveDeveloper(id,mappedResponse)
                        .doOnError(error -> log.warn("Non-fatal Redis cache write failure for developer ID '{}': {}", id, error.getMessage()))
                        .thenReturn(mappedResponse)
                    )
        );
  }

  public Mono<DeveloperRegisterResponse> registerDeveloper(
      RegisterDeveloperRequest request){
    log.info("Incoming registration request hit Service layer for: {}", request.getEmail());
    return aiDevBoardClient.registerDeveloper(request)
        .doOnNext(response -> log.info(
            "Network Client successfully received remote response: {}",
            response.getMessage()
        ))
        .doOnError(error -> log.error(
            "Network Client encountered a remote infrastructure crash!",
            error
        ))
        .flatMap(response -> {
          log.info("Proceeding to persist data for Developer ID: {}",
              (response.getDeveloper() != null) ? response.getDeveloper().getId() : "null");
          return persistAndCacheDeveloperData(response)
              .thenReturn(response);
        });
  }

  public Mono<Void> persistAndCacheDeveloperData(
      DeveloperRegisterResponse response){
    if (response == null
        || response.getDeveloper() == null
        || response.getDeveloper().getId() == null){
      return Mono.empty();
    }

    UUID devId = response.getDeveloper().getId();
    RegisteredDeveloperDto dto = response.getDeveloper();

    return saveDeveloperToDb(devId, dto)
        .flatMap(actualDevId ->
            saveDeveloperPermissionsToDb(actualDevId, dto.getPermissions())
                .thenReturn(actualDevId) // Carry the ID forward
        )
        .doOnNext(actualDevId ->
            log.info("Database persistence completed successfully for developer ID: {}", actualDevId)
        )
        .flatMap(actualDevId -> cacheDeveloperProfile(actualDevId, response))
        .doOnError(dbError ->
            log.error("DATABASE PERSISTENCE FAILURE FOR DEVELOPER {}. Aborting caching sequence.", devId, dbError)
        );
  }

  private Mono<List<String>> fetchPermissions(UUID id) {
    return databaseClient.sql("""
            SELECT "permission" FROM "developer_permissions" WHERE "developer_id" = CAST(:id AS uuid)
            """)
        .bind("id", id)
        .map((row, rowMetadata) -> row.get("permission", String.class))
        .all()
        .collectList();
  }

  private Mono<UUID> saveDeveloperToDb(UUID devId, RegisteredDeveloperDto dto){
    return databaseClient.sql("""
            INSERT INTO "developers" ("id", "name", "email", "api_key", "rate_limit_per_hour", "is_active", "tier", "created_at")
            VALUES (CAST(:id AS uuid), :name, :email, :apiKey, :rateLimit, :isActive, :tier, :createdAt)
            ON CONFLICT ("email") DO UPDATE SET
                "name" = EXCLUDED."name",
                "api_key" = EXCLUDED."api_key",
                "rate_limit_per_hour" = EXCLUDED."rate_limit_per_hour",
                "is_active" = EXCLUDED."is_active",
                "tier" = EXCLUDED."tier"
            RETURNING "id"
        """)
        .bind("id", devId)
        .bind("name", dto.getName())
        .bind("email", dto.getEmail())
        .bind("apiKey", dto.getApiKey() != null ? dto.getApiKey() : "NOT_PROVIDED")
        .bind("rateLimit", dto.getRateLimitPerHour())
        .bind("isActive", dto.isActive())
        .bind("tier", dto.getTier())
        .bind("createdAt", dto.getCreatedAt() != null ? dto.getCreatedAt() : OffsetDateTime.now())
        .map((row, rowMetadata) -> Objects.requireNonNull(row.get("id", UUID.class)))
        .one();
  }

  private Mono<Void> saveDeveloperPermissionsToDb(UUID devId, List<String> permissions){
    var validPermissions = permissions.stream()
        .filter(p -> p != null && !p.isBlank()).distinct().toList();

    if (validPermissions.isEmpty()) {
      return Mono.empty();
    }

    List<String> placeholders = new ArrayList<>();
    for (int i = 0; i < validPermissions.size(); i++) {
      placeholders.add("(CAST(:devId AS uuid), :perm" + i + ")");
    }

    String valuesClause = String.join(", ", placeholders);

    var sqlQuery = """
            INSERT INTO "developer_permissions" ("developer_id", "permission")
            VALUES %s
            ON CONFLICT ("developer_id", "permission") DO NOTHING
        """.formatted(valuesClause);

    var spec = databaseClient.sql(sqlQuery).bind("devId", devId.toString());

    for (int index = 0; index < validPermissions.size(); index++) {
      String permission = validPermissions.get(index);
      spec = spec.bind("perm" + index, permission);
    }

    return spec.then()
        .doOnSuccess(unused -> log.debug(
            "Successfully batch-persisted {} permissions for developer ID: {}",
            validPermissions.size(),
            devId
        ))
        .doOnError(e -> {
          log.error(
              "Failed to execute multi-row permission batch insert for developer {}: {}",
              devId, e.getMessage(), e
          );
          throw new RuntimeException(e);
        });

  }

  private Mono<Void> cacheDeveloperProfile(UUID devId, DeveloperRegisterResponse response) {
    return developerCacheService.saveDeveloper(devId, response)
        .doOnSuccess(unused -> log.info("Successfully cached developer profile for ID: {}", devId))
        .doOnError(ex -> log.error("Failed to populate Redis cache for developer {}", devId, ex));
  }
}
