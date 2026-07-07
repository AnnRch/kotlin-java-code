package com.example.jobs.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.ZonedDateTime
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RegisteredDeveloperDto(
    @param:JsonProperty("id") val id: UUID,
    @param:JsonProperty("email") val email: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("api_key") val apiKey: String,
    @param:JsonProperty("permissions") val permissions: List<String> = emptyList(),
    @param:JsonProperty("rate_limit_per_hour") val rateLimitPerHour: Int,
    @param:JsonProperty("is_active") val isActive: Boolean,
    @param:JsonProperty("tier") val tier: String,
    @param:JsonProperty("created_at") val createdAt: ZonedDateTime
)
