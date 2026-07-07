package com.example.jobs.repository

import com.example.jobs.entity.Job
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobRepository : CoroutineCrudRepository<Job, UUID> {

    @Modifying
    @Query("DELETE FROM public.jobs WHERE company_id = :companyId")
    suspend fun deleteByCompanyId(companyId: Long): Int

    @Query("SELECT id FROM jobs WHERE company_id = :companyId")
    suspend fun findAllIdsByCompanyId(companyId: Long): List<UUID>

    @Modifying
    @Query("DELETE FROM jobs WHERE id IN (:jobIds)")
    suspend fun deleteAllByIdIn(jobIds: Collection<UUID>): Long
}