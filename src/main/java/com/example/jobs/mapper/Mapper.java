package com.example.jobs.mapper;

import com.example.jobs.dto.CompanyDto;
import com.example.jobs.dto.DeveloperRegisterResponse;
import com.example.jobs.dto.JobDetailResponse;
import com.example.jobs.dto.JobDetails;
import com.example.jobs.dto.JobSyncDto;
import com.example.jobs.dto.RegisteredDeveloperDto;
import com.example.jobs.entity.Company;
import com.example.jobs.entity.Job;
import com.example.jobs.entity.KotlinCompany;
import com.example.jobs.entity.KotlinDeveloper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class Mapper {
  public DeveloperRegisterResponse mapEntityToResponse(KotlinDeveloper entity) {
    if (entity.getId() == null) {
      throw new IllegalStateException("Developer entity missing ID");
    }

    RegisteredDeveloperDto dto = new RegisteredDeveloperDto();
    dto.setId(entity.getId());
    dto.setEmail(entity.getEmail());
    dto.setName(entity.getName());
    dto.setApiKey(entity.getApiKey());
    dto.setPermissions(entity.getPermissions());
    dto.setRateLimitPerHour(entity.getRateLimitPerHour());
    dto.setActive(entity.isActive());
    dto.setTier(entity.getTier());
    dto.setCreatedAt(entity.getCreatedAt().toZonedDateTime());

    DeveloperRegisterResponse response = new DeveloperRegisterResponse();
    response.setDeveloper(dto);
    response.setMessage("Profile loaded successfully.");

    return response;
  }

  public boolean hasChanged(CompanyDto cached, CompanyDto incoming){
    return !Objects.equals(cached.getName(), incoming.getName()) ||
        cached.getJobs() != incoming.getJobs() ||
        !Objects.equals(cached.getAvgSalary(), incoming.getAvgSalary()) ||
        !Objects.equals(cached.getWebsite(), incoming.getWebsite()) ||
        !Objects.equals(cached.getLogoUrl(), incoming.getLogoUrl()) ||
        !Objects.equals(cached.getSalaryCount(), incoming.getSalaryCount());
  }

  public Job toJobEntityFromDetail(JobDetailResponse dto, Company companyEntity) {
    Job job = new Job();

    job.setId(dto.id());
    job.setNew(true);

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
    if (dto.createdAt() != null && !dto.createdAt().isBlank()) {
      job.setCreatedAt(LocalDateTime.ofInstant(
          Instant.parse(dto.createdAt()),
          ZoneId.systemDefault()
      ));
    } else {
      job.setCreatedAt(LocalDateTime.now());
    }

    return job;
  }

  public JobDetailResponse toDetailResponse(JobDetails jobDetails) {
    return new JobDetailResponse(
        jobDetails.id(),
        jobDetails.title(),
        jobDetails.slug(),
        jobDetails.description(),
        jobDetails.salaryMin(),
        jobDetails.salaryMax(),
        jobDetails.location(),
        jobDetails.workplace(),
        jobDetails.jobType(),
        jobDetails.experienceLevel(),
        jobDetails.tags() != null ? jobDetails.tags() : Collections.emptyList(),
        jobDetails.applyUrl(),
        jobDetails.companyName(),
        jobDetails.companyLogoUrl(),
        jobDetails.qualityScore(),
        jobDetails.createdAt(),
        jobDetails.url(),
        jobDetails.isFeatured(),
        jobDetails.isSticky(),
        jobDetails.status(),
        jobDetails.companySlug()
    );
  }

  public JobDetailResponse toDetailResponse(JobSyncDto dto, KotlinCompany company) {
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
        dto.tags() != null ? dto.tags() : Collections.emptyList(),
        company.getSlug(),
        company.getName(),
        company.getLogoUrl(),
        dto.qualityScore(),
        dto.createdAt() != null ? dto.createdAt() : Instant.now().toString(),
        dto.applyUrl(),
        dto.isFeatured() != null ? dto.isFeatured() : false,
        dto.isSticky() != null ? dto.isSticky() : false,
        dto.status() != null ? dto.status() : "active",
        dto.url()
    );
  }

}
