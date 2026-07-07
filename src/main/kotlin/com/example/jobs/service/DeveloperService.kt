package com.example.jobs.service

import com.example.jobs.client.AiDevBoardClient
import com.example.jobs.dto.DeveloperRegisterResponse
import com.example.jobs.dto.RegisterDeveloperRequest
import com.example.jobs.dto.RegisteredDeveloperDto
import com.example.jobs.entity.Developer
import com.example.jobs.repository.DeveloperRepository
import com.example.jobs.service.cache.DeveloperCacheService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

@Service
class DeveloperService(
    private val developerRepository: DeveloperRepository,
    private val databaseClient: DatabaseClient,
    private val webClient: AiDevBoardClient,
    private val developerCacheService: DeveloperCacheService
) {
    companion object {
        private val log = LoggerFactory.getLogger(DeveloperService::class.java)
    }

    suspend fun persistAndCacheDeveloperData(response: DeveloperRegisterResponse) {
        val dto = response.developer ?: return
        val devId = dto.id ?: return

        try {
            log.info("Attempting database persistence for developer ID: {}", devId)

            val actualDevId = saveDeveloperToDb(devId, dto)
            saveDeveloperPermissionsToDb(actualDevId, dto.permissions)

            log.info(
                "Database persistence completed successfully for developer ID: {}",
                actualDevId
            )

            cacheDeveloperProfile(actualDevId, response)

        } catch (dbError: Exception) {
            log.error(
                "CRITICAL DATABASE PERSISTENCE FAILURE FOR DEVELOPER $devId. Aborting caching sequence.",
                dbError
            )

            throw dbError
        }
    }

    suspend fun getDeveloperProfile(id: UUID): DeveloperRegisterResponse {
        val cached = try {
            developerCacheService.getDeveloper(id)
        } catch (e: Exception) {
            log.warn("Non-fatal Redis cache read failure for developer ID '{}': {}", id, e.message)
            null
        }

        if (cached != null) return cached

        val entity = developerRepository.findById(id).awaitFirstOrNull()
            ?: throw NoSuchElementException("Developer profile not found.")

        val permissionsList = databaseClient.sql(
            """
                SELECT "permission" FROM "developer_permissions" WHERE "developer_id" = CAST(:id AS uuid)
            """
        )
            .bind("id", id.toString())
            .map { row, _ -> row.get("permission", String::class.java)!! }
            .all()
            .collectList()
            .awaitSingle()

        entity.permissions = permissionsList
        val mappedResponse = mapEntityToResponse(entity)

        try {
            developerCacheService.saveDeveloper(id, mappedResponse)
        } catch (e: Exception) {
            log.warn("Non-fatal Redis cache write failure for developer ID '{}': {}", id, e.message)
        }

        return mappedResponse
    }

    suspend fun registerDeveloper(request: RegisterDeveloperRequest): DeveloperRegisterResponse {
        log.info("Incoming registration request hit Service layer for: {}", request.email)

        val response = webClient.registerDeveloper(request)
            .doOnNext {
                log.info(
                    "Network Client successfully received remote response: {}",
                    it.message
                )
            }
            .doOnError {
                log.error(
                    "Network Client encountered a remote infrastructure crash!",
                    it
                )
            }
            .awaitSingle()

        log.info("Proceeding to persist data for Developer ID: {}", response.developer?.id)
        persistAndCacheDeveloperData(response)

        return response
    }

    private fun mapEntityToResponse(entity: Developer): DeveloperRegisterResponse {
        val developerDto = RegisteredDeveloperDto(
            id = entity.id ?: throw IllegalStateException("Developer entity missing ID"),
            email = entity.email,
            name = entity.name,
            apiKey = entity.apiKey,
            permissions = entity.permissions,
            rateLimitPerHour = entity.rateLimitPerHour,
            isActive = entity.isActive,
            tier = entity.tier,
            createdAt = entity.createdAt.toZonedDateTime()
        )
        return DeveloperRegisterResponse().apply {
            this.developer = developerDto
            this.message = "Profile loaded successfully."
        }
    }

    // Reactive layer compatibility
    fun registerDeveloperReactive(request: RegisterDeveloperRequest): Mono<DeveloperRegisterResponse> =
        mono {
            registerDeveloper(request)
        }

    private suspend fun saveDeveloperToDb(devId: UUID, dto: RegisteredDeveloperDto): UUID {
        return databaseClient.sql(
            """
        INSERT INTO "developers" ("id", "name", "email", "api_key", "rate_limit_per_hour", "is_active", "tier", "created_at")
        VALUES (CAST(:id AS uuid), :name, :email, :apiKey, :rateLimit, :isActive, :tier, :createdAt)
        ON CONFLICT ("email") DO UPDATE SET
            "name" = EXCLUDED."name",
            "api_key" = EXCLUDED."api_key",
            "rate_limit_per_hour" = EXCLUDED."rate_limit_per_hour",
            "is_active" = EXCLUDED."is_active",
            "tier" = EXCLUDED."tier"
        RETURNING "id"
    """
        )
            .bind("id", devId.toString())
            .bind("name", dto.name)
            .bind("email", dto.email)
            .bind("apiKey", dto.apiKey)
            .bind("rateLimit", dto.rateLimitPerHour)
            .bind("isActive", dto.isActive)
            .bind("tier", dto.tier)
            .bind("createdAt", dto.createdAt ?: OffsetDateTime.now())
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .awaitSingle()
    }

    private suspend fun saveDeveloperPermissionsToDb(devId: UUID, permissions: List<String>) {
        val validPermissions = permissions.distinct().filter { it.isNotBlank() }
        if (validPermissions.isEmpty()) return

        val valuesClause = validPermissions.indices.joinToString(", ") { index ->
            "(CAST(:devId AS uuid), :perm$index)"
        }

        val sqlQuery = """
            INSERT INTO "developer_permissions" ("developer_id", "permission")
            VALUES $valuesClause
            ON CONFLICT ("developer_id", "permission") DO NOTHING
        """.trimIndent()

        var spec = databaseClient.sql(sqlQuery).bind("devId", devId.toString())

        validPermissions.forEachIndexed { index, permission ->
            spec = spec.bind("perm$index", permission)
        }

        try {
            spec.then().awaitFirstOrNull()
            log.debug(
                "Successfully batch-persisted {} permissions for developer ID: {}",
                validPermissions.size,
                devId
            )
        } catch (e: Exception) {
            log.error(
                "Failed to execute multi-row permission batch insert for developer $devId: ${e.message}",
                e
            )
            throw e
        }
    }

    private suspend fun cacheDeveloperProfile(devId: UUID, response: DeveloperRegisterResponse) {
        try {
            developerCacheService.saveDeveloper(devId, response)
            log.info("Successfully cached developer profile for ID: $devId")
        } catch (ex: Exception) {
            log.error("Failed to populate Redis cache for developer $devId", ex)
        }
    }
}