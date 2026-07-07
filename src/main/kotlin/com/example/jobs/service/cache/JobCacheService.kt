package com.example.jobs.service.cache

import com.example.jobs.dto.JobDetailResponse
import com.example.jobs.dto.JobSyncDto
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class JobCacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private fun cacheKey(id: UUID) = "job:$id"

    suspend fun getJob(id: UUID): JobDetailResponse? {
        val rawData = redisTemplate.opsForValue().get(cacheKey(id)).awaitFirstOrNull() ?: return null

        return if (rawData is Map<*, *>) {
            objectMapper.convertValue(rawData, JobDetailResponse::class.java)
        } else {
            rawData as? JobDetailResponse
        }
    }

    suspend fun saveJob(dto: JobDetailResponse) {
        redisTemplate.opsForValue().set(cacheKey(dto.id), dto, Duration.ofHours(2))
            .awaitFirstOrNull()
    }

    suspend fun evictJob(id: UUID) {
        redisTemplate.delete(cacheKey(id))
            .awaitFirstOrNull()
    }

    suspend fun saveJob(dto: JobSyncDto, companySlug: String, companyName: String) {
        val detail = JobDetailResponse(
            id = dto.id,
            title = dto.title,
            slug = dto.slug,
            location = dto.location,
            workplace = dto.workplace,
            jobType = dto.jobType,
            experienceLevel = dto.experienceLevel,
            salaryMin = dto.salaryMin,
            salaryMax = dto.salaryMax,
            tags = dto.tags,
            qualityScore = dto.qualityScore,
            createdAt = dto.createdAt,
            companySlug = companySlug,
            companyName = companyName,
            description = null,
            applyUrl = null,
            isFeatured = false,
            isSticky = false,
            status = null,
            url = null,
            companyLogoUrl = null
        )
        redisTemplate.opsForValue().set(cacheKey(detail.id), detail, Duration.ofHours(2))
            .awaitFirstOrNull()
    }
}
