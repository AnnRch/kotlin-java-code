package com.example.jobs.service.cache

import com.example.jobs.dto.DeveloperRegisterResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class DeveloperCacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {

    private fun cacheKey(id: UUID) = "developer:$id"

    suspend fun getDeveloper(id: UUID): DeveloperRegisterResponse? {
        val rawData = redisTemplate.opsForValue().get(cacheKey(id)).awaitFirstOrNull() ?: return null

        return try {
            objectMapper.convertValue(rawData, DeveloperRegisterResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDeveloper(id: UUID, response: DeveloperRegisterResponse) {
        redisTemplate
            .opsForValue().set(cacheKey(id), response, Duration.ofHours(2))
            .awaitFirstOrNull()
    }

    suspend fun evictDeveloper(id: UUID) {
        redisTemplate.delete(cacheKey(id))
            .awaitFirstOrNull()
    }
}