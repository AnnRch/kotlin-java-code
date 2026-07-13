package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JobDetails(
    UUID id,
    @JsonProperty("company_id") UUID companyId,
    String title,
    String slug,
    String description,
    @JsonProperty("salary_min")
    BigDecimal salaryMin,
    @JsonProperty("salary_max")
    BigDecimal salaryMax,
    String location,
    String workplace,
    @JsonProperty("job_type")
    String jobType,
    @JsonProperty("experience_level")
    String experienceLevel,
    List<String> tags,
    @JsonProperty("apply_url")
    String applyUrl,
    @JsonProperty("is_featured")
    boolean isFeatured,
    @JsonProperty("is_sticky")
    boolean isSticky,
    String status,
    @JsonProperty("quality_score")
    Integer qualityScore,
    @JsonProperty("created_at")
    String createdAt,
    String url,
    @JsonProperty("company_name")
    String companyName,
    @JsonProperty("company_slug")
    String companySlug,
    @JsonProperty("company_logo_url")
    String companyLogoUrl
) {

}
