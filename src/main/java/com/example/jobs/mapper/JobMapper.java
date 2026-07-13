package com.example.jobs.mapper;

import com.example.jobs.dto.JobDetailResponse;
import com.example.jobs.dto.JobDetails;
import com.example.jobs.dto.JobSyncDto;
import com.example.jobs.entity.Company;
import com.example.jobs.entity.Job;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {
  public JobDetailResponse toDetailResponse(JobSyncDto dto, Company company) {
    return new JobDetailResponse(
        dto.id(),
        dto.title(),
        dto.slug(),
        dto.description(),
        dto.salaryMin(),
        dto.salaryMax(),
        dto.location(),
        dto.workplace(),
        dto.jobType(),
        dto.experienceLevel(),
        dto.tags() != null ? dto.tags() : List.of(),
        company.getSlug(),
        company.getName(),
        company.getLogoUrl(),
        dto.qualityScore(),
        dto.createdAt() != null ? dto.createdAt() : Instant.now().toString(),
        dto.applyUrl(),
        Boolean.TRUE.equals(dto.isFeatured()),
        Boolean.TRUE.equals(dto.isSticky()),
        dto.status() != null ? dto.status() : "active",
        dto.url()
    );
  }

  public JobDetailResponse toDetailResponse(JobDetails details) {
    return new JobDetailResponse(
        details.id(),
        details.title(),
        details.slug(),
        details.description(),
        details.salaryMin(),
        details.salaryMax(),
        details.location(),
        details.workplace(),
        details.jobType(),
        details.experienceLevel(),
        details.tags() != null ? details.tags() : List.of(),
        details.companySlug(),
        details.companyName(),
        details.companyLogoUrl(),
        details.qualityScore(),
        null,
        details.applyUrl(),
        details.isFeatured(),
        details.isSticky(),
        details.status(),
        details.url()
    );
  }

  public Job toJobEntityFromDetail(JobDetailResponse dto, Company companyEntity, boolean isNewRecord) {
    Job job = new Job();

    job.setId(dto.id());
    job.setNew(isNewRecord);
    job.setIsNewRecord(isNewRecord);

    job.setCompanyId(companyEntity.getId());
    job.setTitle(dto.title());
    job.setSlug(dto.slug());
    job.setDescription(dto.description());
    job.setLocation(dto.location());
    job.setWorkplace(dto.workplace());
    job.setJobType(dto.jobType());
    job.setExperienceLevel(dto.experienceLevel());
    job.setSalaryMin(dto.salaryMin());
    job.setSalaryMax(dto.salaryMax());
    job.setQualityScore(dto.qualityScore());
    job.setApplyUrl(dto.applyUrl());
    job.setIsFeatured(dto.isFeatured());
    job.setIsSticky(dto.isSticky());
    job.setStatus(dto.status() != null ? dto.status() : "active");
    job.setUrl(dto.url());

    if (dto.createdAt() != null && !dto.createdAt().isBlank()) {
      try {
        job.setCreatedAt(LocalDateTime.ofInstant(
            Instant.parse(dto.createdAt()),
            ZoneId.systemDefault()
        ));
      } catch (Exception e) {
        job.setCreatedAt(LocalDateTime.now());
      }
    } else {
      job.setCreatedAt(LocalDateTime.now());
    }

    job.setUpdatedAt(LocalDateTime.now());

    return job;
  }
}
