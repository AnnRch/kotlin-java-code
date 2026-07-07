package com.example.jobs.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID

data class JobResponse(
    val access: AccessMetadata,
    val degraded: Boolean,
    val estimated: Boolean,
    @JsonProperty("has_next") val hasNext: Boolean,
    val jobs: List<JobDetails>
)

data class AccessMetadata(
    @JsonProperty("advertiser_pricing_url") val advertiserPricingUrl: String,
    @JsonProperty("catalog_url") val catalogUrl: String,
    val description: String,
    @JsonProperty("docs_url") val docsUrl: String,
    val mode: String,
    @JsonProperty("register_url") val registerUrl: String
)

data class JobDetails(
    val id: UUID,
    @JsonProperty("company_id") val companyId: UUID,
    val title: String,
    val slug: String,
    val description: String?,
    @JsonProperty("salary_min") val salaryMin: BigDecimal?,
    @JsonProperty("salary_max") val salaryMax: BigDecimal?,
    val location: String?,
    val workplace: String?,
    @JsonProperty("job_type") val jobType: String?,
    @JsonProperty("experience_level") val experienceLevel: String?,
    val tags: List<String>?,
    @JsonProperty("apply_url") val applyUrl: String?,
    @JsonProperty("is_featured") val isFeatured: Boolean,
    @JsonProperty("is_sticky") val isSticky: Boolean,
    val status: String?,
    @JsonProperty("quality_score") val qualityScore: Int?,
    val url: String?,
    @JsonProperty("company_name") val companyName: String,
    @JsonProperty("company_slug") val companySlug: String,
    @JsonProperty("company_logo_url") val companyLogoUrl: String?
){

}