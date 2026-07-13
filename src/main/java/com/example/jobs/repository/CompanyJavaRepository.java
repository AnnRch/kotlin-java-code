package com.example.jobs.repository;

import com.example.jobs.entity.Company;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyJavaRepository extends ReactiveCrudRepository<Company, String> {
  Mono<Company> findBySlug(String slug);
  Mono<Boolean> existsBySlug(String slug);

  @Query("""
        INSERT INTO companies (slug, name, website, total_jobs, avg_salary_all, salary_count_all)
        VALUES (:slug, :name, :website, :totalJobs, :avgSalary, :salaryCount)
        ON CONFLICT (slug) DO UPDATE SET
            name = EXCLUDED.name,
            website = EXCLUDED.website,
            total_jobs = EXCLUDED.total_jobs,
            avg_salary_all = EXCLUDED.avg_salary_all,
            salary_count_all = EXCLUDED.salary_count_all
        RETURNING *
        """)
  Mono<Company> upsert(String slug, String name, String website,
      Integer totalJobs, Double avgSalary, Integer salaryCount);
}
