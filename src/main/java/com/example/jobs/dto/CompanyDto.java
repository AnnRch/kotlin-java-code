package com.example.jobs.dto;

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
public class CompanyDto {
  private String slug;
  private String name;
  private int jobs;
  private String website;
  private String logoUrl;
  private Integer salaryCount;
  private Double avgSalary;

}
