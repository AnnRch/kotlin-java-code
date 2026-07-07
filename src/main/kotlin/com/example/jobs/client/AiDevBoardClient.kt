package com.example.jobs.client

import com.example.jobs.dto.CompanyProfileResponse
import com.example.jobs.dto.CompanyRegisterRequest
import com.example.jobs.dto.CompanyRegisterResponse
import com.example.jobs.dto.CompanyResponse
import com.example.jobs.dto.DeveloperRegisterResponse
import com.example.jobs.dto.JobDetailResponse
import com.example.jobs.dto.JobResponse
import com.example.jobs.dto.RegisterDeveloperRequest
import com.example.jobs.service.JobSyncService
import kotlinx.coroutines.reactive.awaitSingle
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.util.UUID

@Slf4j
@Component
class AiDevBoardClient(
    private val webClient: WebClient
) {
    private val retryStrategy = Retry.backoff(3, Duration.ofSeconds(2))
        companion object {
            private val log = LoggerFactory.getLogger(JobSyncService::class.java)
        }


    suspend fun getAllCompanies(): CompanyResponse? {
        return webClient.get()
            .uri("/companies")
            .retrieve()
            .bodyToMono<CompanyResponse>()
            .retryWhen(retryStrategy)
            .awaitSingle()
    }

    suspend fun getCompanyProfile(slug: String): CompanyProfileResponse {
        return webClient.get()
            .uri("/companies/{slug}", slug)
            .retrieve()
            .bodyToMono<CompanyProfileResponse>()
            .retryWhen(retryStrategy)
            .awaitSingle()
    }

    suspend fun getJobDetail(id: UUID): JobDetailResponse {
        return webClient.get()
            .uri("/jobs/{id}", id)
            .retrieve()
            .bodyToMono<JobDetailResponse>()
            .doOnError { error ->
            // This hooks into the error BEFORE the retry strategy masks it!
            if (error is WebClientResponseException) {
                log.error("--- TARGET SYNC HTTP FAILURE ---")
                log.error("HTTP Status Code: ${error.statusCode}")
                log.error("Response Content: ${error.responseBodyAsString}")
                log.error("---------------------------------")
            } else {
                log.error("Direct network/connection error: ${error.message}")
            }
        }
            .retryWhen(retryStrategy)
            .awaitSingle()
    }

    suspend fun fetchRecentJobs(): JobResponse {
        return webClient.get()
            .uri("/jobs")
            .retrieve()
            .bodyToMono<JobResponse>()
            .retryWhen(retryStrategy)
            .awaitSingle()
    }

    suspend fun registerCompany(request: CompanyRegisterRequest): CompanyRegisterResponse {
        return webClient.post()
            .uri("/register/company")
            .bodyValue(request)
            .retrieve()
            .bodyToMono<CompanyRegisterResponse>()
            .awaitSingle()
    }

fun registerDeveloper(request: RegisterDeveloperRequest): Mono<DeveloperRegisterResponse> {
    return webClient.post()
        .uri("/register/developer")
        .bodyValue(request)
        .retrieve()
        .bodyToMono<DeveloperRegisterResponse>()
}

    suspend fun updateCompanyOnRemote(slug: String, apiKey: String, updateMap: Map<String, Any?>): Unit {
        webClient.patch()
            .uri("/companies/{slug}", slug)
            .header("Authorization", "Bearer $apiKey")
            .bodyValue(updateMap)
            .retrieve()
            .bodyToMono<Void>()
            .awaitSingle()
    }
}