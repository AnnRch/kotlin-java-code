package com.example.jobs.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "job_posting_tiers")
class JobPostingTier {
    @Id
    var id: String = ""

    var name: String = ""

    var description: String? = null

    @Column("price_cents")
    var priceCents: Int = 0

    @Column("duration_days")
    var durationDays: Int = 0

    @Column("is_featured")
    var isFeatured: Boolean = false

    @Column("is_sticky")
    var isSticky: Boolean = false

    @Column("sort_order")
    var sortOrder: Int = 0

    @Column("created_at")
    var createdAt: Instant = Instant.now()

    @Column("is_active")
    var isActive: Boolean = true
}