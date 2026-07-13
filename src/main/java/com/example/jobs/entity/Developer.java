package com.example.jobs.entity;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "developers")
public class Developer implements Persistable<UUID> {
  @Id
  @Column("id")
  private UUID devId;

  @Transient
  private boolean isNewEntity = true;

  @Column("name")
  private String name = "";

  @Column("email")
  private String email = "";

  @Column("api_key")
  private String apiKey = "";

  @Column("rate_limit_per_hour")
  private int rateLimitPerHour = 100;

  @Column("is_active")
  private boolean isActive = true;

  @Column("tier")
  private String tier = "free";

  @Column("created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Transient
  private List<String> permissions = Collections.emptyList();

  @Override
  public UUID getId() { return this.devId; }

  @Override
  public boolean isNew() { return this.isNewEntity; }

  public void setId(UUID id) { this.devId = id; }
  public void setNew(boolean isNew) { this.isNewEntity = isNew; }
}
