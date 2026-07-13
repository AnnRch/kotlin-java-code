package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserAgentStat(
    @JsonProperty("user_agent")
    String userAgent,
    Long count
) {

}
