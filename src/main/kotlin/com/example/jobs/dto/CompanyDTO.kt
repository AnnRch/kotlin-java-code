package com.example.jobs.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime
import java.util.UUID

data class CompanyResponse(val companies: List<CompanyDto>)

data class CompanyDto(
    val slug: String,
    val name: String,
    val jobs: Int,
    val website: String?,
    val logo_url: String?,
    val salary_count: Int?,

    @JsonProperty("avg_salary") val avg_salary: Double? = null
)

data class CompanyDetailsDto(
    val slug: String,
    val name: String,
    val website: String?,
    val logo_url: String?,
    val jobs: Int,
    val avg_salary: Double?,
    val salary_count: Int,
    val clicks: Int,
    val views: Int,
    val views_7d: Int
)

data class CompanyProfileResponse(
    val company: CompanyDetailsDto,
    val jobs: List<JobSyncDto>
)

fun CompanyDto.hasChanged(other: CompanyDto): Boolean {
    if (this.jobs != other.jobs) return true

    val currentSalary = this.avg_salary ?: 0.0
    val incomingSalary = other.avg_salary ?: 0.0

    // Check for serialization casting anomalies
    return kotlin.math.abs(currentSalary - incomingSalary) > 0.0001
}

data class CompanyRegisterRequest(
    val name: String,
    val email: String,
    val website: String?
)

data class RegisteredCompanyDto(
    val id: UUID,
    val name: String,
    val slug: String,
    val website: String?,
    val email: String?,
    val api_key: String?,
    val created_at: ZonedDateTime
)

data class CompanyRegisterResponse(
    val company: RegisteredCompanyDto,
    val message: String?
)