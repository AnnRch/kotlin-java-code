package com.example.jobs.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.annotation.Id

@Table(name = "companies")
class Company() {
//    @Transient
//    var isNewRecord: Boolean = true

    @Id
    var id: Long? = null

    @Column("api_key")
    var apiKey: String? = null

    var slug: String = ""

    var name: String = ""

    var website: String? = null

    @Column("logo_url")
    var logoUrl: String? = null

    @Column("total_jobs")
    var totalJobs: Int = 0

    @Column("avg_salary_all")
    var avgSalaryAll: Double? = null

    @Column("salary_count_all")
    var salaryCountAll: Int = 0

    var clicks: Int = 0

    var views: Int = 0

    @Column("views_7d")
    var views7d: Int = 0

    constructor(slug: String) : this() {
        this.slug = slug
    }
}