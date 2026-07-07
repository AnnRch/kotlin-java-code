package com.example.jobs.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
class RegisterDeveloperRequest {
    @field:JsonProperty("id")
    var id: UUID? = null

    @field:JsonProperty("email")
    var email: String = ""

    @field:JsonProperty("name")
    var name: String = ""

    @field:JsonProperty("api_key")
    var apiKey: String = ""

    @field:JsonProperty("permissions")
    var permissions: List<String> = emptyList()

    @field:JsonProperty("rate_limit_per_hour")
    var rateLimitPerHour: Int = 100

    @field:JsonProperty("is_active")
    var isActive: Boolean = true

    @field:JsonProperty("tier")
    var tier: String = "free"

    @field:JsonProperty("created_at")
    var createdAt: ZonedDateTime? = null

    @JsonCreator
    constructor()
}