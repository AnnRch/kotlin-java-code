package com.example.jobs.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "jobs")
class Job : Persistable<UUID> {

    private var id: UUID? = null

    @Transient
    private var isNewRecord: Boolean = true

    @Column("company_id")
    var companyId: Long? = null

    @Column("title")
    var title: String = ""

    @Column("slug")
    var slug: String = ""

    @Column("description")
    var description: String? = null

    @Column("salary_min")
    var salaryMin: BigDecimal? = null

    @Column("salary_max")
    var salaryMax: BigDecimal? = null

    @Column("location")
    var location: String? = null

    @Column("workplace")
    var workplace: String? = null

    @Column("job_type")
    var jobType: String? = null

    @Column("experience_level")
    var experienceLevel: String? = null

    @Column("apply_url")
    var applyUrl: String? = null

    @Column("is_featured")
    var isFeatured: Boolean? = false

    @Column("is_sticky")
    var isSticky: Boolean? = false

    @Column("status")
    var status: String? = null

    @Column("quality_score")
    var qualityScore: Int? = null

    @Column("url")
    var url: String? = null

    @Column("published_at")
    var publishedAt: LocalDateTime? = null

    @Column("expires_at")
    var expiresAt: LocalDateTime? = null

    @Column("created_at")
    var createdAt: LocalDateTime? = null

    @Column("updated_at")
    var updatedAt: LocalDateTime? = null

    @Id
    override fun getId(): UUID? = this.id

    override fun isNew(): Boolean = this.isNewRecord

    fun setId(id: UUID) {
        this.id = id
    }

    // Update the correct consolidated flag
    fun setNew(isNew: Boolean) {
        this.isNewRecord = isNew
    }

    fun setIsNewRecord(newRecord: Boolean) {
        this.isNewRecord = newRecord
    }
}