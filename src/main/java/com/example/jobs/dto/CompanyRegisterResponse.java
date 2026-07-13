package com.example.jobs.dto;

import com.example.jobs.dto.RegisteredCompanyDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CompanyRegisterResponse {
  private RegisteredCompanyDto dto;
  private String message;
}
