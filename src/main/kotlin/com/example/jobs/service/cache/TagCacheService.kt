package com.example.jobs.service.cache

import com.example.jobs.entity.Tag
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class TagCacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private fun itemKey(name: String) = "tag:name:$name"
    private val listKey = "tags:all"

    /**
     * Fetch a specific Tag payload by its plain name string
     */
    suspend fun getTagByName(name: String): Tag? {
        val rawData = redisTemplate.opsForValue().get(itemKey(name)).awaitFirstOrNull() ?: return null

        return if (rawData is Map<*, *>) {
            objectMapper.convertValue(rawData, Tag::class.java)
        } else {
            rawData as? Tag
        }
    }

    /**
     * Cache a single specific Tag instance node
     */
    suspend fun saveTag(tag: Tag) {
        val operationMono: Mono<Boolean> = redisTemplate.opsForValue()
            .set(itemKey(tag.name), tag, Duration.ofHours(6))
        operationMono.awaitSingle()
        evictAllTagsCollection()
    }

    /**
     * Cache an entire structural array list of system Tags
     */
    suspend fun saveAllTagsCollection(tags: List<Tag>) {
        redisTemplate.opsForValue().set(listKey, tags, Duration.ofHours(3))
            .awaitFirstOrNull()
    }

    /**
     * Retrieve the cached uniform structural array list of tags
     */
    suspend fun getAllTagsCollection(): List<Tag>? {
        val rawData = redisTemplate.opsForValue().get(listKey).awaitFirstOrNull() ?: return null

        return if (rawData is List<*>) {
            // Safe collection type mapping from List<LinkedHashMap> to List<Tag>
            val targetType = objectMapper.typeFactory.constructCollectionType(List::class.java, Tag::class.java)
            objectMapper.convertValue(rawData, targetType)
        } else {
            @Suppress("UNCHECKED_CAST")
            rawData as? List<Tag>
        }
    }

    /**
     * Invalidate specific single key mapping elements
     */
    suspend fun evictTag(name: String) {
        redisTemplate.delete(itemKey(name)).awaitFirstOrNull()
        evictAllTagsCollection()
    }

    /**
     * Invalidate the global aggregated list cache entry node
     */
    suspend fun evictAllTagsCollection() {
        redisTemplate.delete(listKey).awaitFirstOrNull()
    }
}