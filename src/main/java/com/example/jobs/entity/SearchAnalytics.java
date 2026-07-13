package com.example.jobs.entity;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("mv_search_analytics_summary_30_days")
public class SearchAnalytics {
  @Column("metric_type")
  private String metricType;

  @Column("metric_key")
  private String metricKey;

  @Column("metric_count")
  private Long metricCount;

  @Column("avg_results")
  private Double avgResults;

  @Column("zero_result_searches")
  private Long zeroResultSearches;
}
