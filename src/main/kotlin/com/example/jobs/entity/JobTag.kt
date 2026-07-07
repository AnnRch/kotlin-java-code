package com.example.jobs.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "job_tags")
class JobTag {
    @Column("job_id")
    lateinit var jobId: UUID

    @Column("tag_id")
    var tagId: Int? = null
}