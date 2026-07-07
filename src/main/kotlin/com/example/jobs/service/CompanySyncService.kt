package com.example.jobs.service

import com.example.jobs.client.AiDevBoardClient
import com.example.jobs.dto.CompanyDto
import com.example.jobs.dto.CompanyRegisterRequest
import com.example.jobs.dto.CompanyRegisterResponse
import com.example.jobs.entity.Company
import com.example.jobs.mapper.SyncMapper
import com.example.jobs.repository.CompanyRepository
import com.example.jobs.service.cache.CompanyCacheService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CompanySyncService(
    private val webClient: AiDevBoardClient,
    private val companyService: CompanyService,
    private val cacheService: CompanyCacheService,
    private val mapper: SyncMapper,
    private val companyRepository: CompanyRepository
): DisposableBean {
    companion object {
        private val log = LoggerFactory.getLogger(CompanySyncService::class.java)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun syncAllCompaniesOverview() {
        log.info("Starting global companies overview synchronization pipeline")
        try {
            val response = webClient.getAllCompanies()
            val companies = response?.companies
            log.info("Fetched ${companies?.size} companies from upstream API")

            companies?.asFlow()
                ?.buffer(100)
                ?.flatMapMerge(concurrency = 10) { dto ->
                    flow { emit(processIncomingData(dto)) }
                }
                ?.collect()

            log.info("Pipeline completed all items processing successfully.")
        } catch (e: Exception) {
            log.error("Pipeline Failure during overview sync: {}", e.message)
            throw e
        }
    }

    suspend fun processIncomingData(dto: CompanyDto) {
        val cached = try {
            cacheService.getCompany(dto.slug)
        } catch (e: Exception) {
            log.warn("Cache lookup failed for ${dto.slug}: ${e.message}")
            null
        }

        if (cached == null || mapper.hasChanged(cached, dto)) {
            log.info("Company ${dto.slug} needs sync. Updating database...")
            try {
                companyService.updateCompany(dto).awaitSingle()
                serviceScope.launch {
                    try { cacheService.saveCompany(dto) } catch (e: Exception) {
                        log.error("Non-fatal caching failure for ${dto.slug}: ${e.message}")
                    }
                }
                log.info("Successfully updated {} in DB", dto.slug)
            } catch (error: Exception) {
                log.error("Failed to update company {} in database: {}", dto.slug, error.message)
            }
        }
    }

    suspend fun registerAndSaveCompany(request: CompanyRegisterRequest): CompanyRegisterResponse {
        val response = webClient.registerCompany(request)
        val remoteCompany = response.company

        if (remoteCompany != null) {
            val companyEntity = companyRepository.findBySlug(remoteCompany.slug)
                ?: Company(remoteCompany.slug)

            companyEntity.apply {
                name = remoteCompany.name
                website = remoteCompany.website
                if (!remoteCompany.api_key.isNullOrBlank()) {
                    apiKey = remoteCompany.api_key
                }
            }
            companyRepository.save(companyEntity)
        }
        return response
    }

    suspend fun updateRemoteCompanyProfile(slug: String, name: String, website: String?, logoUrl: String?) {
        val companyEntity = companyRepository.findBySlug(slug)
            ?: throw NoSuchElementException("Company profile not tracked locally.")

        val apiKey = companyEntity.apiKey
            ?: throw IllegalStateException("No API credentials found for this slug. Register company first.")

        val updatePayload = mapOf("name" to name, "website" to website, "logoUrl" to logoUrl)
        webClient.updateCompanyOnRemote(slug, apiKey, updatePayload)
    }

    fun syncAllCompaniesOverviewReactive(): Mono<Void> = mono {
        syncAllCompaniesOverview()
        null
    }

    fun registerAndSaveCompanyReactive(request: CompanyRegisterRequest): Mono<CompanyRegisterResponse> = mono {
        registerAndSaveCompany(request)
    }

    fun updateRemoteCompanyProfileReactive(slug: String, name: String, website: String?, logoUrl: String?): Mono<Void> = mono {
        updateRemoteCompanyProfile(slug, name, website, logoUrl)
        null
    }

    override fun destroy() {
        log.info("Shutting down CompanySyncService background worker scope. Clearing active links...")
        try {
            serviceScope.cancel()
            log.info("CompanySyncService background coroutine scope closed successfully.")
        } catch (e: Exception) {
            log.error("Error gracefully closing CompanySyncService coroutine scope: ${e.message}", e)
        }
    }
}