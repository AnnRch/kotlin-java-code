package com.example.jobs.service;

import com.example.jobs.dto.EndPointStat;
import com.example.jobs.dto.QueryStat;
import com.example.jobs.dto.SearchAnalyticsResponse;
import com.example.jobs.dto.TagStat;
import com.example.jobs.repository.AnalyticsJavaRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsJavaService {

  private final AnalyticsJavaRepository analyticsJavaRepository;
  private final ObservationRegistry observationRegistry;

  public Mono<SearchAnalyticsResponse> getSearchAnalytics(Integer requestedDays) {
    return Observation.createNotStarted("analytics.get-summary", observationRegistry)
        .observe(() -> analyticsJavaRepository.findAllBy()
            .collect(() -> new SearchAnalyticsResponse(requestedDays), (response, row) -> {
              switch(row.getMetricType()) {
                case "endpoint" -> response.addEndpoint(new EndPointStat(
                    row.getMetricKey(),
                    row.getMetricCount(),
                    row.getAvgResults()
                ));
                case "query" -> response.addQuery(new QueryStat(
                    row.getMetricKey(),
                    row.getMetricCount(),
                    row.getAvgResults()
                ));
                case "tag" -> response.addTag(new TagStat(
                    row.getMetricKey(),
                    row.getMetricCount()
                ));
                case "counter" -> response.setCounters(
                    row.getMetricCount(),
                    row.getZeroResultSearches()
                );
              }
            }));
  }

  public Mono<Void> refreshMaterializedView(){
    return Observation.createNotStarted("analytics.refresh-view", observationRegistry)
        .observe(() -> analyticsJavaRepository.refreshMaterializedView()
            .doOnNext(_ -> log.info("Starting view maintenance execution..."))
            .doOnSuccess(_ -> log.info("Successfully completed concurrent view maintenance execution."))
            .doOnError(error -> log.error("An exceptional condition prevented view refreshment execution sequence.", error)));
  }
}
