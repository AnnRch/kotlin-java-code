package com.example.jobs.entity

import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.relational.core.mapping.Column

@Table("mv_search_analytics_summary_30_days")
data class SearchAnalyticsSummary(
    @Column("metric_type")
    val metricType: String,

    @Column("metric_key")
    val metricKey: String,

    @Column("metric_count")
    val metricCount: Long,

    @Column("avg_results")
    val avgResults: Int,

    @Column("zero_result_searches")
    val zeroResultSearches: Long
) {
}