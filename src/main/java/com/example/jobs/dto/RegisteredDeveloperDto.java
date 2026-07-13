package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RegisteredDeveloperDto {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("email")
  private String email;

  @JsonProperty("name")
  private String name;

  @JsonProperty("api_key")
  private String apiKey;

  @JsonProperty("permissions")
  private List<String> permissions = Collections.emptyList();

  @JsonProperty("rate_limit_per_hour")
  private int rateLimitPerHour;

  @JsonProperty("is_active")
  private boolean isActive;

  @JsonProperty("tier")
  private String tier;

  @JsonProperty("created_at")
  private ZonedDateTime createdAt;
}
