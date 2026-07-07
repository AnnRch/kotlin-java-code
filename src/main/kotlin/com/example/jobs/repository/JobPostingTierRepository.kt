package com.example.jobs.repository

import com.example.jobs.entity.JobPostingTier
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface JobPostingTierRepository: ReactiveCrudRepository<JobPostingTier, String> {
}