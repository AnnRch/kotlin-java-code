package com.example.jobs.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import com.example.jobs.handlers.CompanySyncHandler;
import com.example.jobs.handlers.JobSyncHandler;
import com.example.jobs.handlers.DeveloperHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

  @Bean
  public RouterFunction<ServerResponse> developerRoutes(DeveloperHandler developerHandler)  {
    return RouterFunctions.route()
        .path("/register", devBuilder -> devBuilder
        .POST("/developer", accept(MediaType.APPLICATION_JSON), developerHandler::handleDeveloperRegistration)
        )
            .build();
  }

  @Bean
  public RouterFunction<ServerResponse> adminSyncRoutes(
      CompanySyncHandler companySyncHandler,
      JobSyncHandler jobSyncHandler
  ) {
    return RouterFunctions.route()
        .path("/admin/sync", adminBuilder -> adminBuilder
        .POST("/companies/register", accept(MediaType.APPLICATION_JSON), companySyncHandler::handleCompanyRegistration)
        .POST("/companies", accept(MediaType.APPLICATION_JSON), companySyncHandler::triggerCompaniesOverviewSync)
        .POST("/companies/{slug}", accept(MediaType.APPLICATION_JSON), jobSyncHandler::triggerSingleCompanyJobsSync)
                .POST("/jobs", accept(MediaType.APPLICATION_JSON), jobSyncHandler::triggerBulkJobsSync)
                .POST("/jobs/{id}", accept(MediaType.APPLICATION_JSON), jobSyncHandler::triggerSingleJobSync)
        .PATCH("/companies/{slug}/remote-update", accept(MediaType.APPLICATION_JSON), companySyncHandler::handleRemoteUpdate)
        )
        .build();

  }

  @Bean
  public RouterFunction<ServerResponse> mainApiRoutes(
      RouterFunction<ServerResponse> developerRoutes,
      RouterFunction<ServerResponse> adminSyncRoutes
  ) {
    return RouterFunctions.route()
        .path("/api/v1", builder -> builder
        .add(developerRoutes)
        .add(adminSyncRoutes)
        )
            .build();
  }
}
