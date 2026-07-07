package com.example.jobs.repository

import com.example.jobs.entity.JobTag
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface JobTagRepository: ReactiveCrudRepository<JobTag, Int> {
    fun findByJobId(jobId: UUID): Flux<JobTag>
    fun findByTagId(tagId: Int): Flux<JobTag>
    fun deleteByJobIdAndTagId(jobId: UUID, tagId: Int): Mono<Void>
    fun deleteByJobId(jobId: UUID): Mono<Void>
}