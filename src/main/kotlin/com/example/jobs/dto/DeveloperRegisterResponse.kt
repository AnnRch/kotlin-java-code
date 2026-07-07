package com.example.jobs.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class DeveloperRegisterResponse {
    @field:JsonProperty("developer")
    var developer: RegisteredDeveloperDto? = null

    @field:JsonProperty("message")
    var message: String? = null
}