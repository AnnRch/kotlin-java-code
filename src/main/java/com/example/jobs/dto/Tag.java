package com.example.jobs.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Builder
@Table(name = "tags")
public class Tag {
  @Id
  @Column("tag_id")
  Integer tagId;

  String name;

  @Column("job_count")
  Integer jobCount;
}
