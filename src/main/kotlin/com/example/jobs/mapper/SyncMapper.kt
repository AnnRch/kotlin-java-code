package com.example.jobs.mapper

import com.example.jobs.dto.CompanyDetailsDto
import com.example.jobs.dto.CompanyDto
import com.example.jobs.dto.JobDetailResponse
import com.example.jobs.dto.JobDetails
import com.example.jobs.dto.JobSyncDto
import com.example.jobs.entity.Company
import com.example.jobs.entity.Job
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

@Component
class SyncMapper {
    /**
     * Evaluates structural changes between the cached state and the incoming state.
     */
    fun hasChanged(cached: CompanyDto, incoming: CompanyDto): Boolean {
        return cached.name != incoming.name ||
                cached.jobs != incoming.jobs ||
                cached.avg_salary != incoming.avg_salary ||
                cached.website != incoming.website ||
                cached.logo_url != incoming.logo_url ||
                cached.salary_count != incoming.salary_count
    }

//    fun hasUpdatesForJob(cached: Job)

    /**
     * Maps a database persistence entity to a uniform caching DTO.
     * Useful if you need to build a CompanyDto snapshot directly from the DB.
     */
    fun toCompanyDto(entity: Company): CompanyDto {
        return CompanyDto(
            slug = entity.slug,
            name = entity.name,
            jobs = entity.totalJobs,
            website = entity.website,
            logo_url = entity.logoUrl,
            salary_count = entity.salaryCountAll,
            avg_salary = entity.avgSalaryAll
        )
    }

    /**
     * Maps the incoming upstream detail payload to a uniform caching DTO.
     * This directly fulfills your pipeline's `mapper.toCompanyDto(response.company)` requirement.
     */
    fun toCompanyDto(details: CompanyDetailsDto): CompanyDto {
        return CompanyDto(
            slug = details.slug,
            name = details.name,
            jobs = details.jobs,
            website = details.website,
            logo_url = details.logo_url,
            salary_count = details.salary_count,
            avg_salary = details.avg_salary
        )
    }

    fun updateCompanyFromDto(company: Company, dto: CompanyDetailsDto): Company {
        company.name = dto.name
        company.website = dto.website
        company.logoUrl = dto.logo_url
        company.totalJobs = dto.jobs
        company.avgSalaryAll = dto.avg_salary
        company.salaryCountAll = dto.salary_count
        company.clicks = dto.clicks
        company.views = dto.views
        company.views7d = dto.views_7d
        return company
    }

    fun toJobEntity(dto: JobSyncDto, company: Company, isNewRecord: Boolean): Job {
        return Job().apply {
            val cleanUuid = when (val incomingId = dto.id) {
                is UUID -> incomingId
                is String -> UUID.fromString(incomingId)
                else -> throw IllegalArgumentException("Unsupported ID type: ${incomingId?.javaClass}")
            }

            setId(cleanUuid)
            setNew(isNewRecord)

            companyId = company.id
            title = dto.title
            slug = dto.slug
            location = dto.location
            workplace = dto.workplace
            jobType = dto.jobType
            experienceLevel = dto.experienceLevel

            salaryMin = dto.salaryMin
            salaryMax = dto.salaryMax
            qualityScore = dto.qualityScore

            isFeatured = false
            isSticky = false
            status = "active"

            createdAt = if (!dto.createdAt.isNullOrBlank()) {
                OffsetDateTime.parse(dto.createdAt).toLocalDateTime()
            } else {
                LocalDateTime.now()
            }
        }
    }

fun toJobEntityFromDetail(dto: JobDetailResponse, companyEntity: Company): Job {
    val job = Job()

    // Explicitly mapping the primary key using the helper method
    job.setId(dto.id)

    // FORCE Spring Data R2DBC to run an INSERT statement instead of an UPDATE
    job.setNew(true)

    job.companyId = companyEntity.id

    job.title = dto.title
    job.slug = dto.slug
    job.description = dto.description
    job.location = dto.location
    job.workplace = dto.workplace
    job.jobType = dto.jobType
    job.experienceLevel = dto.experienceLevel
    job.salaryMin = dto.salaryMin as BigDecimal?
    job.salaryMax = dto.salaryMax as BigDecimal?
    job.qualityScore = dto.qualityScore

    // Converting ZonedDateTime to Instant for R2DBC compatibility
    job.createdAt = if (!dto.createdAt.isNullOrBlank()) {
        LocalDateTime.ofInstant(Instant.parse(dto.createdAt), ZoneId.systemDefault())
    } else {
        LocalDateTime.now()
    }

    return job
}

    /**
     * Maps the incoming upstream bulk payload (JobDetails) directly into a persistent database Entity.
     */
    fun toJobEntityFromDetails(dto: JobDetails, companyEntity: Company, isNewRecord: Boolean): Job {
        return Job().apply {
            setId(dto.id)
            setNew(isNewRecord)

            this.companyId = companyEntity.id
            this.title = dto.title
            this.slug = dto.slug
            this.description = dto.description
            this.salaryMin = dto.salaryMin
            this.salaryMax = dto.salaryMax
            this.location = dto.location
            this.workplace = dto.workplace
            this.jobType = dto.jobType
            this.experienceLevel = dto.experienceLevel
            this.applyUrl = dto.applyUrl
            this.isFeatured = dto.isFeatured
            this.isSticky = dto.isSticky
            this.status = dto.status ?: "active"
            this.qualityScore = dto.qualityScore
            this.url = dto.url

            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
    }

    /**
     * Aggregates the separate processing details back into your standard, uniform caching model.
     */
    fun toCacheDto(dto: JobDetails): JobDetailResponse {
        return JobDetailResponse(
            id = dto.id,
            title = dto.title,
            slug = dto.slug,
            description = dto.description,
            salaryMin = dto.salaryMin,
            salaryMax = dto.salaryMax,
            location = dto.location,
            workplace = dto.workplace,
            jobType = dto.jobType,
            experienceLevel = dto.experienceLevel,
            tags = dto.tags ?: emptyList(),
            companySlug = dto.companySlug,
            companyName = dto.companyName,
            qualityScore = dto.qualityScore,
            applyUrl = dto.applyUrl,
            isFeatured = dto.isFeatured,
            isSticky = dto.isSticky,
            status = dto.status,
            url = dto.url,
            companyLogoUrl = dto.companyLogoUrl
            // createdAt will automatically use its default Instant.now() value
        )
    }
    /**
     * Overloaded helper function to map a single targeted `JobDetailResponse` to a database entity,
     * matching your existing template, but using safe BigInteger/BigDecimal casting variations.
     */
    fun toJobEntityFromDetail(dto: JobDetailResponse, companyEntity: Company, isNewRecord: Boolean = true): Job {
        return Job().apply {
            setId(dto.id)
            setNew(isNewRecord)

            companyId = companyEntity.id
            title = dto.title
            slug = dto.slug
            description = dto.description
            location = dto.location
            workplace = dto.workplace
            jobType = dto.jobType
            experienceLevel = dto.experienceLevel
            salaryMin = dto.salaryMin
            salaryMax = dto.salaryMax
            qualityScore = dto.qualityScore
            applyUrl = dto.applyUrl
            isFeatured = dto.isFeatured
            isSticky = dto.isSticky
            status = dto.status ?: "active"
            url = dto.url

            createdAt = if (!dto.createdAt.isNullOrBlank()) {
                runCatching { LocalDateTime.ofInstant(Instant.parse(dto.createdAt), ZoneId.systemDefault()) }
                    .getOrElse { LocalDateTime.now() }
            } else {
                LocalDateTime.now()
            }
            updatedAt = LocalDateTime.now()
        }
    }

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
            applyUrl = this.applyUrl,
            isFeatured = this.isFeatured,
            isSticky = this.isSticky,
            status = this.status,
            url = this.url,
            companyName = this.companyName,
            companySlug = this.companySlug,
            companyLogoUrl = this.companyLogoUrl,
            qualityScore = this.qualityScore
        )
    }


}