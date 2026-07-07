package com.example.jobs.handlers;

import com.example.jobs.dto.CompanyRegisterRequest;
import com.example.jobs.service.CompanySyncService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CompanySyncHandler {

  private final CompanySyncService companySyncService;

  public CompanySyncHandler(CompanySyncService companySyncService) {
    this.companySyncService = companySyncService;
  }

  /**
   * POST /api/v1/admin/sync/companies
   */
  public Mono<ServerResponse> triggerCompaniesOverviewSync(ServerRequest request) {
    return companySyncService.syncAllCompaniesOverviewReactive()
        .then(ServerResponse.ok().bodyValue(Map.of("status", "Sync Complete")))
        .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .bodyValue(Map.of("error", e.getMessage())));
  }

  /**
   * POST /api/v1/admin/companies/register
   */
  public Mono<ServerResponse> handleCompanyRegistration(ServerRequest request) {
    return request.bodyToMono(CompanyRegisterRequest.class)
        .flatMap(companySyncService::registerAndSaveCompanyReactive)
        .flatMap(regResponse -> ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(regResponse));
  }

  /**
   * PATCH /api/v1/admin/companies/{slug}/remote-update
   */
  @SuppressWarnings("unchecked")
  public Mono<ServerResponse> handleRemoteUpdate(ServerRequest request) {
    String slug = request.pathVariable("slug");

    return request.bodyToMono(Map.class)
        .flatMap(body -> validateAndExtractRemoteUpdate(slug, body))
        .then(ServerResponse.ok()
            .bodyValue(Map.of("status", "Remote patch update executed successfully")));
  }

  private Mono<Void> validateAndExtractRemoteUpdate(String slug, Map<?, ?> body) {
    Object nameObj = body.get("name");
    Object websiteObj = body.get("website");
    Object logoUrlObj = body.get("logoUrl");

    if (nameObj != null && !(nameObj instanceof String)) {
      return Mono.error(new IllegalArgumentException("Field 'name' must be a valid text string."));
    }
    if (websiteObj != null && !(websiteObj instanceof String)) {
      return Mono.error(
          new IllegalArgumentException("Field 'website' must be a valid text string."));
    }

    if (logoUrlObj != null && !(logoUrlObj instanceof  String)) {
      return Mono.error(
          new IllegalArgumentException("Field 'logoUrl' must be a valid text string."));
    }

    String name = (String) nameObj;
    String website = (String) websiteObj;
    String logoUrl = (String) logoUrlObj;

    if (name == null || name.isBlank()) {
      return Mono.error(
          new IllegalArgumentException("Mandatory body parameter 'name' is missing or blank."));
    }

    return companySyncService.updateRemoteCompanyProfileReactive(slug, name, website, logoUrl);
  }

}
