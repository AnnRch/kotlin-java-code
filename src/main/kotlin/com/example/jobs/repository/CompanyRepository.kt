package com.example.jobs.repository

import com.example.jobs.entity.Company
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyRepository : CoroutineCrudRepository<Company, Long> {
    suspend fun findBySlug(slug: String): Company?
    suspend fun existsBySlug(slug: String): Boolean
}
