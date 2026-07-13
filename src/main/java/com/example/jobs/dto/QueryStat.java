package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QueryStat(
    String query,
    Long count,
    @JsonProperty("avg_results")
    Double avgResults
) {

}
