package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JobResponse(
    AccessMetadata access,
    boolean degraded,
    boolean estimated,
    @JsonProperty("has_next")
    boolean hasNext,
    List<JobDetails> jobs
) {

}
