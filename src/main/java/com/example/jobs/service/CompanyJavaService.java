package com.example.jobs.service;

import com.example.jobs.dto.CompanyDto;
import com.example.jobs.entity.Company;
import com.example.jobs.repository.CompanyJavaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyJavaService {

  private final CompanyJavaRepository companyJavaRepository;

  public Mono<Company> updateCompany(CompanyDto dto) {
    double avgSalary = (dto.getAvgSalary() != null) ? dto.getAvgSalary() : 0.0;
    int salaryCount = (dto.getSalaryCount() != null) ? dto.getSalaryCount() : 0;

    return companyJavaRepository.upsert(
        dto.getSlug(),
        dto.getName(),
        dto.getWebsite(),
        dto.getJobs(),
        avgSalary,
        salaryCount
    );
  }

  private Company updateCompanyFields(Company company, CompanyDto dto){
    return Company.builder()
        .name(dto.getName())
        .slug(dto.getSlug())
        .totalJobs(dto.getJobs())
        .avgSalaryAll(dto.getAvgSalary() != null ? dto.getAvgSalary() : 0.0)
        .salaryCountAll(dto.getSalaryCount() != null ? dto.getSalaryCount() : 0)
        .website(dto.getWebsite())
        .logoUrl(dto.getLogoUrl())
        .build();
  }
}
