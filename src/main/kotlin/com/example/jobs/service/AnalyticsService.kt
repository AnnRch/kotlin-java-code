package com.example.jobs.service

import com.example.jobs.dto.EndpointStat
import com.example.jobs.dto.QueryStat
import com.example.jobs.dto.SearchAnalyticsResponse
import com.example.jobs.dto.TagStat
import com.example.jobs.repository.AnalyticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

@Service
class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(AnalyticsService::class.java)
    }

    suspend fun getSearchAnalytics(requestedDays: Int): SearchAnalyticsResponse {
        val targetDays = if (requestedDays in 1..90) requestedDays else 30
        log.info(
            "Fetching search analytics metrics aggregated over a target window of {} days",
            targetDays
        )

        val allRows = analyticsRepository.findAllBy().toList()

        if (allRows.isEmpty()) {
            throw NoSuchElementException("No pre-aggregated analytics data available.")
        }

        val endpoints = mutableListOf<EndpointStat>()
        val topQueries = mutableListOf<QueryStat>()
        val topSearchedTags = mutableListOf<TagStat>()
        var totalRequests = 0L
        var zeroResultSearches = 0L

        for (row in allRows) {
            when (row.metricType) {
                "endpoint" -> endpoints.add(
                    EndpointStat(
                        endpoint = row.metricKey,
                        count = row.metricCount,
                        avgResults = row.avgResults.toLong()
                    )
                )

                "query" -> topQueries.add(
                    QueryStat(
                        query = row.metricKey,
                        count = row.metricCount,
                        avgResults = row.avgResults.toLong()
                    )
                )

                "tag" -> topSearchedTags.add(
                    TagStat(tag = row.metricKey, count = row.metricCount)
                )

                "counter" -> {
                    totalRequests = row.metricCount
                    zeroResultSearches = row.zeroResultSearches
                }
            }
        }

        return SearchAnalyticsResponse(
            days = targetDays,
            endpoints = endpoints,
            topQueries = topQueries,
            topSearchedTags = topSearchedTags,
            topUserAgents = emptyList(),
            totalRequests = totalRequests,
            zeroResultSearches = zeroResultSearches
        )
    }

    suspend fun refreshMaterializedView() {
        log.info("Starting view maintenance execution...")
        runCatching {
            analyticsRepository.refreshMaterializedView()
        }.onSuccess {
            log.info("Successfully completed concurrent view maintenance execution.")
        }.onFailure { ex ->
            log.error("An exceptional condition prevented view refreshment execution sequence.", ex)
        }
    }

    //wrapper to be called from inside reactive handler
    fun getSearchAnalyticsAsMono(requestedDays: Int): Mono<SearchAnalyticsResponse> =
        mono { getSearchAnalytics(requestedDays) }
}