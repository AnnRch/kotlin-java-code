package com.example.jobs.repository;

import com.example.jobs.entity.SearchAnalytics;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AnalyticsJavaRepository extends ReactiveCrudRepository<SearchAnalytics,Long> {
  Flux<SearchAnalytics> findAllBy();

  @Query("REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_search_analytics_summary_30_days")
  Mono<Void> refreshMaterializedView();
}
