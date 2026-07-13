package com.example.jobs.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JobSyncDto(
    UUID id,
    String title,
    String slug,
    String description,
    String location,
    String workplace,
    String jobType,
    String experienceLevel,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String applyUrl,
    Boolean isFeatured,
    Boolean isSticky,
    String status,
    Integer qualityScore,
    String url,
    List<String> tags,
    String publishedAt,
    String expiresAt,
    String createdAt,
    String updatedAt
) {

}
