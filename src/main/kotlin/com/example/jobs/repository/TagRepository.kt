package com.example.jobs.repository

import com.example.jobs.entity.Tag
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface TagRepository: ReactiveCrudRepository<Tag, Int> {
    fun findByName(name: String): Mono<Tag>
}