package com.example.jobs.config;

import com.example.jobs.dto.ApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class WebClientConfig {
  @Bean
  public WebClient aiDevBoardWebClient(ApiProperties apiProperties) {
    String targetUrl = apiProperties.baseUrl() + "/api/v1";

    return WebClient.builder()
        .baseUrl(targetUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();
  }

}
