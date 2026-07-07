package com.example.jobs.service

import com.example.jobs.dto.CompanyDto
import com.example.jobs.entity.Company
import com.example.jobs.repository.CompanyRepository
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CompanyService(private val companyRepository: CompanyRepository) {
    private val log = LoggerFactory.getLogger(CompanyService::class.java)

    fun updateCompany(dto: CompanyDto): Mono<Company> = mono {
        log.info("Processing company update/insert via coroutine pipeline for slug: {}", dto.slug)

        val existingCompany = companyRepository.findBySlug(dto.slug)

        val companyToSave = if (existingCompany != null) {
            log.debug("Found existing company record for slug: {}", dto.slug)
            updateCompanyFields(existingCompany, dto)
        } else {
            log.info("No existing company found. Preparing a fresh record for slug: {}", dto.slug)
            val newCompany = Company(dto.slug)
            updateCompanyFields(newCompany, dto)
        }

        return@mono companyRepository.save(companyToSave)
    }

    private fun updateCompanyFields(company: Company, dto: CompanyDto): Company {
        company.name = dto.name
        company.totalJobs = dto.jobs
        company.avgSalaryAll = dto.avg_salary ?: 0.0
        company.salaryCountAll = dto.salary_count ?: 0
        company.website = dto.website
        company.logoUrl = dto.logo_url
        return company
    }
}