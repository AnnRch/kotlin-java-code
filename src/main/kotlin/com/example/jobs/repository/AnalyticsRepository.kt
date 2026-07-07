package com.example.jobs.repository

import com.example.jobs.entity.SearchAnalyticsSummary
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AnalyticsRepository: CoroutineCrudRepository<SearchAnalyticsSummary, Long> {
    fun findAllBy(): Flow<SearchAnalyticsSummary>

    @Query("REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_search_analytics_summary_30_days")
    suspend fun refreshMaterializedView()
}