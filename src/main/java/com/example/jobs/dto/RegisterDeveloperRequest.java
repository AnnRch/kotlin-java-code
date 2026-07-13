package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterDeveloperRequest {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("email")
  private String email = "";

  @JsonProperty("name")
  private String name = "";

  @JsonProperty("api_key")
  private String apiKey = "";

  @JsonProperty("permissions")
  private List<String> permissions = Collections.emptyList();

  @JsonProperty("rate_limit_per_hour")
  private int rateLimitPerHour = 100;

  @JsonProperty("is_active")
  private boolean isActive = true;

  @JsonProperty("tier")
  private String tier = "free";

  @JsonProperty("created_at")
  private ZonedDateTime createdAt;

  @JsonCreator
  public RegisterDeveloperRequest() {
  }
}
