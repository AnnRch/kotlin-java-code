package com.example.jobs.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchAnalyticsResponse(
    val days: Int,
    val endpoints: List<EndpointStat>,

    @field:JsonProperty("top_queries")
    val topQueries: List<QueryStat>,

    @field:JsonProperty("top_searched_tags")
    val topSearchedTags: List<TagStat>,

    @field:JsonProperty("top_user_agents")
    val topUserAgents: List<UserAgentStat> = emptyList(), // Defaulted to safe empty list

    @field:JsonProperty("total_requests")
    val totalRequests: Long,

    @field:JsonProperty("zero_result_searches")
    val zeroResultSearches: Long
)

data class EndpointStat(
    val endpoint: String,
    val count: Long,
    @field:JsonProperty("avg_results") val avgResults: Long
)

data class QueryStat(
    val query: String,
    val count: Long,
    @field:JsonProperty("avg_results") val avgResults: Long
)

data class TagStat(
    val tag: String,
    val count: Long
)

data class UserAgentStat(
    @field:JsonProperty("user_agent") val userAgent: String,
    val count: Long
)