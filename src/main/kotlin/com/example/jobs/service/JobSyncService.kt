package com.example.jobs.service

import com.example.jobs.client.AiDevBoardClient
import com.example.jobs.dto.CompanyDetailsDto
import com.example.jobs.dto.CompanyDto
import com.example.jobs.dto.CompanyProfileResponse
import com.example.jobs.dto.JobDetailResponse
import com.example.jobs.dto.JobDetails
import com.example.jobs.dto.JobSyncDto
import com.example.jobs.dto.hasChanged
import com.example.jobs.dto.toDetailResponse
import com.example.jobs.entity.Company
import com.example.jobs.entity.Job
import com.example.jobs.mapper.SyncMapper
import com.example.jobs.repository.CompanyRepository
import com.example.jobs.repository.JobRepository
import com.example.jobs.service.cache.CompanyCacheService
import com.example.jobs.service.cache.JobCacheService
import com.example.jobs.service.cache.TagCacheService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.awaitRowsUpdated
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    name: String,
    value: T?
): DatabaseClient.GenericExecuteSpec {
    return value?.let { this.bind(name, it) } ?: this.bindNull(name, T::class.javaObjectType)
}

@Service
class JobSyncService(
    private val webClient: AiDevBoardClient,
    private val companyCacheService: CompanyCacheService,
    private val jobCacheService: JobCacheService,
    private val tagCacheService: TagCacheService,
    private val mapper: SyncMapper,
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository,
    private val databaseClient: DatabaseClient
) : DisposableBean {
    companion object {
        private val log = LoggerFactory.getLogger(JobSyncService::class.java)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun syncCompanyJobs(slug: String): CompanyProfileResponse {
        try {
            val response = webClient.getCompanyProfile(slug)
            val incomingDetails = response.company
            val incomingJobs = response.jobs ?: emptyList()

            val incomingCompanyDto = CompanyDto(
                slug = incomingDetails.slug,
                name = incomingDetails.name,
                jobs = incomingDetails.jobs,
                website = incomingDetails.website,
                logo_url = incomingDetails.logo_url,
                salary_count = incomingDetails.salary_count,
                avg_salary = incomingDetails.avg_salary
            )

            val cachedCompany = companyCacheService.getCompany(slug)
            if (cachedCompany != null && !cachedCompany.hasChanged(incomingCompanyDto)) {
                if (companyRepository.existsBySlug(slug)) {
                    log.info("Company '{}' is fresh in cache and DB. Short-circuiting.", slug)
                    return response
                }
            }

            refreshCompanyCache(slug, incomingDetails, incomingCompanyDto)

            val dbCompany = syncCompanyEntity(slug, incomingDetails)
            val companyId = dbCompany.id
                ?: throw IllegalStateException("Failed to assign primary ID for: $slug")

            val shouldContinue = utilizeStaleJobs(companyId, slug, incomingJobs)
            if (!shouldContinue) return response

            processActiveJobsPipeline(incomingJobs, dbCompany, companyId)

            return response
        } catch (error: Exception) {
            log.error(
                "Failed synchronization pipeline execution for slug '{}'. Error: {}",
                slug,
                error.message
            )
            throw error
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun fetchJobs(): List<JobDetailResponse> {
        try {
            val response = webClient.fetchRecentJobs()
            val jobsList = response.jobs
            log.info("Fetched ${jobsList.size} jobs from upstream API")

            val processedJobs = jobsList.asFlow()
                .buffer(100)
                .flatMapMerge(concurrency = 10) { jobDetails ->
                    flow {
                        try {
                            val cachedJob = jobCacheService.getJob(jobDetails.id)

                            if (cachedJob != null) {
                                emit(cachedJob)
                            } else {
                                val companyEntity = processCompanyUpsert(jobDetails)
                                val detailResponse = jobDetails.toDetailResponse()

                                val entity = mapper.toJobEntityFromDetail(
                                    detailResponse,
                                    companyEntity,
                                    isNewRecord = true
                                )
                                upsertJobRelational(entity, companyEntity.id!!)

                                if (!jobDetails.tags.isNullOrEmpty()) {
                                    syncJobTagsBatch(jobDetails.id, jobDetails.tags)
                                }

                                serviceScope.launch {
                                    runCatching { jobCacheService.saveJob(detailResponse) }
                                    runCatching { tagCacheService.evictAllTagsCollection() }
                                }

                                emit(detailResponse)
                            }
                        } catch (e: Exception) {
                            log.error("Error processing job ${jobDetails.id}: ${e.message}", e)
                        }
                    }
                }
                .toList()
            return processedJobs
        } catch (e: Exception) {
            log.error("Pipeline Failure during jobs fetching: {}", e.message)
            throw e
        }
    }

    suspend fun syncSpecificJob(id: UUID): JobDetailResponse {
        log.info("Starting targeted sync pipeline for Job ID: '{}'", id)
        try {
            val cachedJob = jobCacheService.getJob(id)
            if (cachedJob != null) {
                log.info(
                    "Cache hit for Job ID '{}' in Redis. Short-circuiting pipeline execution.",
                    id
                )
                return cachedJob
            }

            log.info("Cache miss for Job ID '{}'. Fetching live data from upstream...", id)
            val response = webClient.getJobDetail(id)
            log.info(
                "Successfully retrieved job detail from upstream for slug: '{}'. Parent company: '{}'",
                response.slug,
                response.companySlug
            )

            val companyEntity = processCompanyRelation(response)
            processJobRelation(response, companyEntity)

            asyncCacheUpdates(response)

            return response
        } catch (error: Exception) {
            log.error(
                "Failed targeted sync pipeline execution for job ID [{}]. Error: {}",
                id,
                error.message
            )
            throw error
        }
    }

    // Extensions & Helpers
    private suspend fun processCompanyRelation(response: JobDetailResponse): Company {
        val existingCompany = companyRepository.findBySlug(response.companySlug)
        val company = existingCompany ?: Company(response.companySlug).apply {
            this.name = response.companyName
            this.logoUrl = response.companyLogoUrl
        }

        if (existingCompany == null) {
            log.info(
                "Parent company '{}' not found in DB. Initializing new company record.",
                response.companySlug
            )
        } else {
            log.debug(
                "Found existing parent company record for slug '{}' with ID: {}",
                response.companySlug,
                existingCompany.id
            )
        }

        return companyRepository.save(company)
    }

    private suspend fun processJobRelation(response: JobDetailResponse, companyEntity: Company) {
        val jobId = response.id
        val existingJob = jobRepository.findById(jobId)
        val isNewRecord = (existingJob == null)

        if (isNewRecord) {
            log.info(
                "Job ID '{}' is unknown to relational store. Preparing SQL INSERT pipeline.",
                jobId
            )
        } else {
            log.info("Job ID '{}' already exists in DB. Preparing SQL UPDATE pipeline.", jobId)
        }

        val entity = mapper.toJobEntityFromDetail(response, companyEntity, isNewRecord).apply {
            setIsNewRecord(isNewRecord)
        }

        jobRepository.save(entity)
        log.info("Successfully persisted job metadata to relational store for ID: '{}'", jobId)

        if (!response.tags.isNullOrEmpty()) {
            log.info("Synchronizing {} relational tags for Job ID: '{}'", response.tags.size, jobId)
            syncJobTagsBatch(jobId, response.tags)
        } else {
            log.debug(
                "No tags attached to incoming job record for ID: '{}'. Skipping tag sync.",
                jobId
            )
        }
    }

    private fun asyncCacheUpdates(response: JobDetailResponse) {
        val jobId = response.id
        serviceScope.launch {
            log.info("Dispatching asynchronous cache updates for Job ID: '{}'", jobId)

            runCatching {
                jobCacheService.saveJob(response)
                log.info("Asynchronously refreshed Redis cache layer for Job ID: '{}'", jobId)
            }.onFailure { e ->
                log.error("Async Redis sync failed for Job ID '{}': {}", jobId, e.message)
            }

            runCatching {
                tagCacheService.evictAllTagsCollection()
                log.debug("Evicted tag collections cache safely.")
            }.onFailure { e ->
                log.error("Async tag cache eviction failed: {}", e.message)
            }
        }
    }

    private suspend fun refreshCompanyCache(
        slug: String,
        incomingDetails: CompanyDetailsDto,
        dto: CompanyDto
    ) {
        log.info(
            "Company cache missing, stale, or DB dropped for '{}'. Refreshing cache layer.",
            slug
        )
        companyCacheService.saveCompany(incomingDetails)
        log.info("company dto : {}", dto)
    }

    private suspend fun syncCompanyEntity(
        slug: String,
        incomingDetails: CompanyDetailsDto
    ): Company {
        val dbCompany = companyRepository.findBySlug(slug) ?: Company().apply {
            this.slug = incomingDetails.slug
        }

        dbCompany.name = incomingDetails.name
        dbCompany.website = incomingDetails.website
        dbCompany.logoUrl = incomingDetails.logo_url
        dbCompany.totalJobs = incomingDetails.jobs
        dbCompany.avgSalaryAll = incomingDetails.avg_salary
        dbCompany.salaryCountAll = incomingDetails.salary_count
        dbCompany.clicks = incomingDetails.clicks
        dbCompany.views = incomingDetails.views
        dbCompany.views7d = incomingDetails.views_7d

        log.info("Saving company entity to PostgreSQL. Current ID: {}", dbCompany.id)
        return companyRepository.save(dbCompany)
    }

    private suspend fun utilizeStaleJobs(
        companyId: Long,
        slug: String,
        incomingJobs: List<JobSyncDto>
    ): Boolean {
        if (incomingJobs.isEmpty()) {
            log.info("No active jobs sent for '{}'. Clearing company database footprint.", slug)
            jobRepository.deleteByCompanyId(companyId)
            return false
        }

        val upstreamJobIds = incomingJobs.map { it.id }.toSet()
        val existingDbJobIds = jobRepository.findAllIdsByCompanyId(companyId).toSet()
        val jobsToDelete = existingDbJobIds.minus(upstreamJobIds)

        if (jobsToDelete.isNotEmpty()) {
            log.info("Detected ${jobsToDelete.size} stale jobs for company ID $companyId. Evicting...")
            jobRepository.deleteAllByIdIn(jobsToDelete)

            serviceScope.launch {
                jobsToDelete.forEach { jobId ->
                    runCatching { jobCacheService.evictJob(jobId) }
                }
            }
        }
        return true
    }

    private suspend fun processActiveJobsPipeline(
        incomingJobs: List<JobSyncDto>,
        dbCompany: Company,
        companyId: Long
    ): List<JobDetailResponse> {
        return incomingJobs.asFlow()
            .buffer(100)
            .flatMapMerge(concurrency = 10) { jobDetails ->
                flow {
                    try {
                        val cachedJob = jobCacheService.getJob(jobDetails.id)
                        if (cachedJob != null) {
                            emit(cachedJob)
                        } else {
                            val existsInDb = jobRepository.existsById(jobDetails.id)
                            val detailResponse = jobDetails.toDetailResponse(dbCompany)

                            val entity = mapper.toJobEntityFromDetail(
                                detailResponse,
                                dbCompany,
                                isNewRecord = !existsInDb
                            )
                            upsertJobRelational(entity, companyId)

                            if (!jobDetails.tags.isNullOrEmpty()) {
                                syncJobTagsBatch(jobDetails.id, jobDetails.tags)
                            }

                            serviceScope.launch {
                                runCatching { jobCacheService.saveJob(detailResponse) }
                                runCatching { tagCacheService.evictAllTagsCollection() }
                            }
                            emit(detailResponse)
                        }
                    } catch (e: Exception) {
                        log.error("Error processing job ${jobDetails.id}: ${e.message}", e)
                    }
                }
            }
            .toList()
    }

    private suspend fun upsertJobRelational(entity: Job, companyId: Long) {
        try {
            val sqlQuery = """
            INSERT INTO public.jobs (
                id, company_id, title, slug, description, salary_min, salary_max, 
                location, workplace, job_type, experience_level, apply_url, 
                is_featured, is_sticky, status, quality_score, url, 
                published_at, expires_at, created_at, updated_at
            ) VALUES (
                CAST(:id AS uuid), CAST(:companyId AS BIGINT), :title, :slug, :description, CAST(:salaryMin AS NUMERIC), CAST(:salaryMax AS NUMERIC), 
                :location, :workplace, :jobType, :experienceLevel, :applyUrl, 
                CAST(:isFeatured AS BOOLEAN), CAST(:isSticky AS BOOLEAN), :status, CAST(:qualityScore AS INTEGER), :url, 
                CAST(:publishedAt AS TIMESTAMP), CAST(:expiresAt AS TIMESTAMP), CAST(:createdAt AS TIMESTAMP), CAST(:updatedAt AS TIMESTAMP)
            )
            ON CONFLICT (id) DO UPDATE SET
                company_id = EXCLUDED.company_id,
                title = EXCLUDED.title,
                slug = EXCLUDED.slug,
                description = EXCLUDED.description,
                salary_min = EXCLUDED.salary_min,
                salary_max = EXCLUDED.salary_max,
                location = EXCLUDED.location,
                workplace = EXCLUDED.workplace,
                job_type = EXCLUDED.job_type,
                experience_level = EXCLUDED.experience_level,
                apply_url = EXCLUDED.apply_url,
                is_featured = EXCLUDED.is_featured,
                is_sticky = EXCLUDED.is_sticky,
                status = EXCLUDED.status,
                quality_score = EXCLUDED.quality_score,
                url = EXCLUDED.url,
                published_at = EXCLUDED.published_at,
                expires_at = EXCLUDED.expires_at,
                updated_at = NOW()
        """

            var spec = databaseClient.sql(sqlQuery)
                .bind("id", entity.getId()?.toString() ?: entity.id.toString())
                .bind("companyId", companyId)
                .bind("title", entity.title)
                .bind("slug", entity.slug)
                .bind("isFeatured", entity.isFeatured ?: false)
                .bind("isSticky", entity.isSticky ?: false)

            spec = spec.bindNullable("description", entity.description)
            spec = spec.bindNullable("location", entity.location)
            spec = spec.bindNullable("workplace", entity.workplace)
            spec = spec.bindNullable("jobType", entity.jobType)
            spec = spec.bindNullable("experienceLevel", entity.experienceLevel)
            spec = spec.bindNullable("salaryMin", entity.salaryMin)
            spec = spec.bindNullable("salaryMax", entity.salaryMax)
            spec = spec.bindNullable("qualityScore", entity.qualityScore)
            spec = spec.bindNullable("status", entity.status)
            spec = spec.bindNullable("applyUrl", entity.applyUrl)
            spec = spec.bindNullable("url", entity.url)
            spec = spec.bindNullable("publishedAt", entity.publishedAt)
            spec = spec.bindNullable("expiresAt", entity.expiresAt)
            spec = spec.bindNullable("createdAt", entity.createdAt ?: LocalDateTime.now())
            spec = spec.bindNullable("updatedAt", entity.updatedAt ?: LocalDateTime.now())

            spec.then().awaitFirstOrNull()

        } catch (e: Exception) {
            log.error(
                "Relational sync upsert hit a block on ID [{}]: {}",
                entity.getId() ?: entity.id,
                e.message,
                e
            )
            throw e
        }
    }

    private suspend fun processCompanyUpsert(jobDetails: JobDetails): Company {
        val existingCompany = companyRepository.findBySlug(jobDetails.companySlug)

        return if (existingCompany != null) {
            databaseClient.sql("""
                UPDATE "companies" 
                SET "total_jobs" = "total_jobs" + 1 
                WHERE "id" = :companyId
            """)
                .bind("companyId", existingCompany.id!!)
                .then()
                .awaitFirstOrNull()

            existingCompany.apply { totalJobs += 1 }
        } else {
            val newCompany = Company(jobDetails.companySlug).apply {
                this.name = jobDetails.companyName
                this.logoUrl = jobDetails.companyLogoUrl
                this.totalJobs = 1
            }
            try {
                companyRepository.save(newCompany)
            } catch (e: Exception) {
                companyRepository.findBySlug(jobDetails.companySlug)?.apply {
                    databaseClient.sql("""
                        UPDATE "companies" SET "total_jobs" = "total_jobs" + 1 WHERE "id" = :companyId
                    """)
                        .bind("companyId", this.id!!)
                        .then()
                        .awaitFirstOrNull()
                    this.totalJobs += 1
                } ?: throw e
            }
        }
    }

    private suspend fun syncJobTagsBatch(jobId: UUID, tagNames: List<String>) {
        val uniqueTags = tagNames.distinct().filter { it.isNotBlank() }
        if (uniqueTags.isEmpty()) return

        for (name in uniqueTags) {
            try {
                val resolvedTagId = databaseClient.sql(
                    """
                INSERT INTO "tags" ("name", "job_count") 
                VALUES (:name, 0) 
                ON CONFLICT (name) 
                DO UPDATE SET "name" = EXCLUDED."name" 
                RETURNING tag_id
            """
                )
                    .bind("name", name)
                    .fetch()
                    .first()
                    .map { row -> row["tag_id"] as Int }
                    .awaitFirstOrNull() ?: continue

                // Link the tag to the job
                val rowsUpdated = databaseClient.sql(
                    """
                INSERT INTO "job_tags" ("job_id", "tag_id") 
                VALUES (CAST(:jobId AS uuid), :tagId) 
                ON CONFLICT (job_id, tag_id) 
                DO NOTHING
            """
                )
                    .bind("jobId", jobId.toString())
                    .bind("tagId", resolvedTagId)
                    .fetch()
                    .awaitRowsUpdated()

                if (rowsUpdated > 0) {
                    databaseClient.sql(
                        """
                    UPDATE "tags" 
                    SET "job_count" = "job_count" + 1 
                    WHERE "tag_id" = :tagId
                """
                    )
                        .bind("tagId", resolvedTagId)
                        .then()
                        .awaitFirstOrNull()
                }
            } catch (e: Exception) {
                log.error("Failed handling tag link for [$name] on job [$jobId]: ${e.message}")
            }
        }
    }

    // Reactive layer outputs
    fun syncAllJobsReactive(): Mono<List<JobDetailResponse>> = mono { fetchJobs() }
    fun syncSpecificCompanyJobsReactive(slug: String): Mono<CompanyProfileResponse> =
        mono { syncCompanyJobs(slug) }

    fun syncSpecificJobReactive(id: UUID): Mono<JobDetailResponse> = mono { syncSpecificJob(id) }
    override fun destroy() {
        log.info("Shutting down JobSyncService worker scope. Cancelling all pending background tasks...")
        try {
            serviceScope.cancel()
            log.info("JobSyncService background coroutine scope cancelled successfully.")
        } catch (e: Exception) {
            log.error(
                "Error occurred while cancelling JobSyncService coroutine scope: ${e.message}",
                e
            )
        }
    }
}