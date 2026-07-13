package com.example.jobs.handlers;

import com.example.jobs.dto.kotlin.SearchAnalyticsResponse;
import com.example.jobs.service.AnalyticsJavaService;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class AnalyticsHandler {
  private final AnalyticsJavaService analyticsService;

  public AnalyticsHandler(AnalyticsJavaService analyticsService) {
    this.analyticsService = analyticsService;
  }

  /**
   * GET /api/v1/analytics/searches
   */
  public Mono<ServerResponse> handleGetSearchAnalytics(ServerRequest request) {
    int requestedDays = request.queryParam("days")
        .map(val -> {
          try {
            return Integer.parseInt(val);
          } catch (NumberFormatException e) {
            return 30;
          }
        })
        .orElse(30);

    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(analyticsService.getSearchAnalytics(requestedDays), SearchAnalyticsResponse.class)

        .onErrorResume(NoSuchElementException.class, ex ->
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", ex.getMessage())))

        .onErrorResume(throwable ->
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "Failed to retrieve analytics: " + throwable.getMessage())));
  }
}
