package com.example.jobs.handlers;

import com.example.jobs.service.JobSyncJavaService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class JobSyncHandler {
  private final JobSyncJavaService jobSyncService;

  public JobSyncHandler(JobSyncJavaService jobSyncService) {
    this.jobSyncService = jobSyncService;
  }

  /**
   * POST /api/v1/admin/sync/companies/{slug}
   */
  public Mono<ServerResponse> triggerSingleCompanyJobsSync(ServerRequest request) {
    String slug = request.pathVariable("slug");

    return jobSyncService.syncCompanyJobs(slug)
        .flatMap(profilePayload ->
            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(profilePayload))
        .onErrorResume(WebClientResponseException.NotFound.class, notFoundEx ->
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "The requested company slug '" + slug
                    + "' does not exist on remote provider API.")))
        .onErrorResume(throwable ->
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "Sync execution failed: " + throwable.getMessage())));
  }

  /**
   * POST /api/v1/admin/sync/jobs
   */
  public Mono<ServerResponse> triggerBulkJobsSync(ServerRequest request) {
    return jobSyncService.fetchJobs()
        .collectList()
        .flatMap(summary -> ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(summary))
        .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("error", "Bulk jobs sync pipeline failed: " + e.getMessage())));
  }

  /**
   * POST /api/v1/admin/sync/jobs/{id}
   */
  public Mono<ServerResponse> triggerSingleJobSync(ServerRequest request) {
    UUID jobId;
    try {
      jobId = UUID.fromString(request.pathVariable("id"));
    } catch (IllegalArgumentException ex) {
      return ServerResponse.status(HttpStatus.BAD_REQUEST)
          .bodyValue(Map.of("error", "Invalid structural UUID parameter provided."));
    }

    return jobSyncService.syncSpecificJob(jobId)
        .flatMap(jobPayload -> ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(jobPayload))
        .onErrorResume(throwable -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("error", "Job Sync failed: " + throwable.getMessage())));
  }
}
