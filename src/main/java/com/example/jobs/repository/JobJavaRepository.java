package com.example.jobs.repository;

import com.example.jobs.entity.Job;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface JobJavaRepository extends ReactiveCrudRepository<Job, UUID> {
  @Modifying
  @Query("DELETE FROM public.jobs WHERE company_id = :companyId")
  Mono<Integer> deleteByCompanyId(Long companyId);

  @Query("SELECT id FROM jobs WHERE company_id = :companyId")
  Flux<UUID> findAllIdsByCompanyId(Long companyId);

  @Modifying
  @Query("DELETE FROM jobs WHERE id IN (:jobIds)")
  Mono<Long> deleteAllByIdIn(Collection<UUID> jobs);
}
