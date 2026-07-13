package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeveloperRegisterResponse {
  @JsonProperty("developer")
  private RegisteredDeveloperDto developer;

  @JsonProperty("message")
  private String message;
}
