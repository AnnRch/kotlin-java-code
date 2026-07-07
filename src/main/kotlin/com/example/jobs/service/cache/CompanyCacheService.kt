package com.example.jobs.service.cache

import com.example.jobs.dto.CompanyDetailsDto
import com.example.jobs.dto.CompanyDto
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CompanyCacheService(
        private val redisTemplate: ReactiveRedisTemplate<String, Any>,
        private val objectMapper: ObjectMapper
    ) {
    suspend fun getCompany(slug: String): CompanyDto? {
        val rawData = redisTemplate.opsForValue().get("company:$slug").awaitFirstOrNull() ?: return null

        return if (rawData is Map<*, *>) {
            objectMapper.convertValue(rawData, CompanyDto::class.java)
        } else {
            rawData as? CompanyDto
        }
    }

    suspend fun saveCompany(dto: CompanyDto) {
        redisTemplate.opsForValue().set("company:${dto.slug}", dto, Duration.ofHours(1))
            .awaitFirstOrNull()
    }

    suspend fun saveCompany(details: CompanyDetailsDto) {
        val dto = CompanyDto(
            slug = details.slug,
            name = details.name,
            jobs = details.jobs,
            website = details.website,
            logo_url = details.logo_url,
            salary_count = details.salary_count,
            avg_salary = details.avg_salary
        )
        redisTemplate.opsForValue().set("company:${dto.slug}", dto, Duration.ofHours(1))
            .awaitFirstOrNull()
    }
}