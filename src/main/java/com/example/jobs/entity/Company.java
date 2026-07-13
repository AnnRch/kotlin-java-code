package com.example.jobs.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "companies")
public class Company {

  @Id
  private Long id;

  @Column("api_key")
  private String apiKey;

  private String slug = "";

  private String name = "";

  private String website;

  @Column("logo_url")
  private String logoUrl;

  @Column("total_jobs")
  private Integer totalJobs = 0;

  @Column("avg_salary_all")
  private Double avgSalaryAll;

  @Column("salary_count_all")
  private int salaryCountAll = 0;

  private int clicks = 0;

  private int views = 0;

  @Column("views_7d")
  private int views7d = 0;

}
