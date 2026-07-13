package com.example.jobs.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CompanyRegisterRequest {
  private String name;
  private String email;
  private String webSite;
}
