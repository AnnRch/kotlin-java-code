package com.example.jobs.dto;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredCompanyDto {
  private UUID id;
  private String name;
  private String slug;
  private String website;
  private String email;
  private String apiKey;
  private ZonedDateTime createdAt;

}
