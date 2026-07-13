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
public class CompanyDetailsDto {
  private String slug;
  private String name;
  private String website;
  private String logoUrl;
  private Integer jobs;
  private Double avgSalary;
  private Integer salaryCount;
  private Integer clicks;
  private Integer views;
  private Integer views7d;
}
