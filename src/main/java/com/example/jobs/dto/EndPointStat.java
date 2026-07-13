package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EndPointStat(
    String endpoint,

    long count,

    @JsonProperty("avg_results") Double avgResults) {

}
