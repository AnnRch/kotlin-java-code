package com.example.jobs.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "jobs")
public class Job implements Persistable<UUID> {

  @Id
  private UUID id;

  @Transient
  private boolean isNewRecord = true;

  @Column("company_id")
  private Long companyId;

  @Column("title")
  private String title = "";

  @Column("slug")
  private String slug = "";

  @Column("description")
  private String description;

  @Column("salary_min")
  private BigDecimal salaryMin;

  @Column("salary_max")
  private BigDecimal salaryMax;

  @Column("location")
  private String location;

  @Column("workplace")
  private String workplace;

  @Column("job_type")
  private String jobType;

  @Column("experience_level")
  private String experienceLevel;

  @Column("apply_url")
  private String applyUrl;

  @Column("is_featured")
  private Boolean isFeatured = false;

  @Column("is_sticky")
  private Boolean isSticky = false;

  @Column("status")
  private String status;

  @Column("quality_score")
  private Integer qualityScore;

  @Column("url")
  private String url;

  @Column("published_at")
  private LocalDateTime publishedAt;

  @Column("expires_at")
  private LocalDateTime expiresAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Override
  public @Nullable UUID getId() {
    return null;
  }

  @Override
  public boolean isNew() {
    return false;
  }

  public void setNew(Boolean isNew) {
    this.isNewRecord = isNew;
  }

  public void setIsNewRecord(Boolean newRecord) {
    this.isNewRecord = newRecord;
  }
}
