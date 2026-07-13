package com.example.jobs.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CompanyResponse {
  private List<CompanyDto> companies;
}
