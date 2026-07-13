package com.example.jobs.config;

import com.example.jobs.handlers.AnalyticsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class AnalyticsRouterConfig {
  @Bean
  public RouterFunction<ServerResponse> analyticsRoute(AnalyticsHandler handler) {
    return RouterFunctions.route()
        .GET("/api/v1/analytics/searches",
            RequestPredicates.accept(MediaType.APPLICATION_JSON),
            handler::handleGetSearchAnalytics)
        .build();
  }
}
