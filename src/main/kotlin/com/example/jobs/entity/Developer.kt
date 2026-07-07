package com.example.jobs.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table(name = "developers")
class Developer : Persistable<UUID> {

    @Id
    @Column("id")
    private var devId: UUID? = null

    @Transient
    private var isNewEntity: Boolean = true

    @Column("name")
    var name: String = ""

    @Column("email")
    var email: String = ""

    @Column("api_key")
    var apiKey: String = ""

    @Column("rate_limit_per_hour")
    var rateLimitPerHour: Int = 100

    @Column("is_active")
    var isActive: Boolean = true

    @Column("tier")
    var tier: String = "free"

    @Column("created_at")
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @Transient
    var permissions: List<String> = emptyList()

    override fun getId(): UUID? = this.devId
    override fun isNew(): Boolean = this.isNewEntity

    fun setId(id: UUID) {
        this.devId = id
    }

    fun setNew(isNew: Boolean) {
        this.isNewEntity = isNew
    }
}