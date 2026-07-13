package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class SearchAnalyticsResponse {
  private Integer days;
  private List<EndPointStat> endPoints = new ArrayList<>();

  @JsonProperty("top_queries")
  private List<QueryStat> topQueries = new ArrayList<>();

  @JsonProperty("top_searched_tags")
  private List<TagStat> tags = new ArrayList<>();

  @JsonProperty("top_user_agents")
  private List<UserAgentStat> topUserAgents = new ArrayList<>();

  private Long totalRequests = 0L;
  private Long zeroResultSearches = 0L;

  public SearchAnalyticsResponse(Integer days) {
    this.days = days;
  }

  public void addEndpoint(EndPointStat e) { this.endPoints.add(e); }
  public void addQuery(QueryStat q) { this.topQueries.add(q); }
  public void addTag(TagStat t) { this.tags.add(t); }
  public void setCounters(long total, long zero) {
    this.totalRequests = total;
    this.zeroResultSearches = zero;
  }
}