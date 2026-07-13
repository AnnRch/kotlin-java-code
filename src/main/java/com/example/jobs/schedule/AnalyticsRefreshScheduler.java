package com.example.jobs.schedule;

import com.example.jobs.service.AnalyticsJavaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsRefreshScheduler {

  private final AnalyticsJavaService analyticsService;

  @Scheduled(cron = "0 0 * * * *")
  public void refreshAnalyticsView() {
    log.info("Triggering scheduled Materialized View refresh non-blockingly...");

    analyticsService.refreshMaterializedView()
        .doOnError(error -> log.error("Scheduled analytics refresh execution encountered an error: {}", error.getMessage(), error))
        .subscribe();
  }
}
