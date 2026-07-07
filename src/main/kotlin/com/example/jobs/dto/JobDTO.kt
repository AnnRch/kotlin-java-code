package com.example.jobs.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import com.example.jobs.entity.Company // Assuming this is your entity package
import com.fasterxml.jackson.annotation.JsonProperty

data class JobSyncDto(
    val id: UUID,
    val title: String,
    val slug: String,
    val description: String?,
    val location: String?,
    val workplace: String?,
    val jobType: String?,
    val experienceLevel: String?,
    val salaryMin: BigDecimal?,
    val salaryMax: BigDecimal?,
    val applyUrl: String?,
    val isFeatured: Boolean?,
    val isSticky: Boolean?,
    val status: String?,
    val qualityScore: Int?,
    val url: String?,
    val tags: List<String> = emptyList(),
    val publishedAt: String?,
    val expiresAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class JobDetailResponse(
    val id: UUID,
    val title: String,
    val slug: String,
    val description: String? = null,

    @JsonProperty("salary_min") val salaryMin: BigDecimal?,
    @JsonProperty("salary_max") val salaryMax: BigDecimal?,

    val location: String?,
    val workplace: String?,

    @JsonProperty("job_type") val jobType: String?,
    @JsonProperty("experience_level") val experienceLevel: String?,

    val tags: List<String> = emptyList(),

    // CRITICAL FIX: Explicit snake_case mappings for the root fields
    @JsonProperty("company_slug") val companySlug: String,
    @JsonProperty("company_name") val companyName: String,
    @JsonProperty("company_logo_url") val companyLogoUrl: String?,

    @JsonProperty("quality_score") val qualityScore: Int?,
    @JsonProperty("created_at") val createdAt: String? = Instant.now().toString(),
    @JsonProperty("apply_url") val applyUrl: String?,
    @JsonProperty("is_featured") val isFeatured: Boolean,
    @JsonProperty("is_sticky") val isSticky: Boolean,

    val status: String?,
    val url: String?
)
/**
 * FIXED: Added `company` context parameter to satisfy required JobDetailResponse constructor arguments.
 * Removed non-existent fields (publishedAt, expiresAt) to fix compiler errors.
 */
fun JobSyncDto.toDetailResponse(company: Company): JobDetailResponse {
    return JobDetailResponse(
        id = this.id,
        title = this.title,
        slug = this.slug,
        description = this.description,
        salaryMin = this.salaryMin,
        salaryMax = this.salaryMax,
        location = this.location,
        workplace = this.workplace,
        jobType = this.jobType,
        experienceLevel = this.experienceLevel,
        applyUrl = this.applyUrl,
        isFeatured = this.isFeatured ?: false,
        isSticky = this.isSticky ?: false,
        status = this.status ?: "active",
        qualityScore = this.qualityScore,
        url = this.url,
        tags = this.tags,
        // Contextually injected from the company object
        companySlug = company.slug,
        companyName = company.name,
        companyLogoUrl = company.logoUrl,
        createdAt = this.createdAt ?: Instant.now().toString()
    )
}

/**
 * Omitted JobDetails mapping stays as is, assuming JobDetails class matches properties exactly.
 */
fun JobDetails.toDetailResponse(): JobDetailResponse {
    return JobDetailResponse(
        id = this.id,
        title = this.title,
        slug = this.slug,
        description = this.description,
        salaryMin = this.salaryMin,
        salaryMax = this.salaryMax,
        location = this.location,
        workplace = this.workplace,
        jobType = this.jobType,
        experienceLevel = this.experienceLevel,
        tags = this.tags ?: emptyList(),
        companySlug = this.companySlug,
        companyName = this.companyName,
        qualityScore = this.qualityScore,
        applyUrl = this.applyUrl,
        isFeatured = this.isFeatured,
        isSticky = this.isSticky,
        status = this.status,
        url = this.url,
        companyLogoUrl = this.companyLogoUrl
    )
}