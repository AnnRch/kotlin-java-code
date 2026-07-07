package com.example.jobs.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("tags")
class Tag {
    @Id
    @Column("tag_id")
    var tagId: Int? = null

    var name: String = ""

    @Column("job_count")
    var jobCount: Int = 0
}