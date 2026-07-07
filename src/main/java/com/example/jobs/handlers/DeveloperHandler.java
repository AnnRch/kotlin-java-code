package com.example.jobs.handlers;

import com.example.jobs.dto.RegisterDeveloperRequest;
import com.example.jobs.service.DeveloperService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DeveloperHandler {

  private final DeveloperService developerService;

  public DeveloperHandler(DeveloperService developerService) {
    this.developerService = developerService;
  }

  /**
   * POST /api/v1/register/developer
   */
 public Mono<ServerResponse> handleDeveloperRegistration(ServerRequest request) {
    return request.bodyToMono(RegisterDeveloperRequest.class)
        .flatMap(developerService::registerDeveloperReactive)
        .flatMap(response -> ServerResponse.status(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(response))
        .onErrorResume(WebClientResponseException.class, ex ->
            ServerResponse.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "Remote developer registration rejected", "details",
                    ex.getResponseBodyAsString())))
        .onErrorResume(throwable -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("error", "Registration pipeline failed: " + throwable.getMessage())));
  }
}
