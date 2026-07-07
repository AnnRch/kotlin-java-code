package com.example.jobs.schedule

import com.example.jobs.service.AnalyticsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Scheduled

@Component
class AnalyticsRefreshScheduler(private val analyticsService: AnalyticsService) {

    companion object {
        private val log = LoggerFactory.getLogger(AnalyticsRefreshScheduler::class.java)
    }

    @Scheduled(cron = "0 0 * * * *")
    suspend fun refreshAnalyticsView() {
        log.info("Triggering scheduled Materialized View refresh non-blockingly via coroutine...")
        try {
            analyticsService.refreshMaterializedView()
        } catch (error: Exception) {
            log.error("Scheduled analytics refresh execution encountered an error: ${error.message}", error)
        }
    }
}