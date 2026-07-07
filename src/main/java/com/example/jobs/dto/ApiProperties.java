package com.example.jobs.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties(prefix = "app.api.aidevboard")
public record ApiProperties(
    @Name("base-url") String baseUrl,
    long timeoutSeconds
) {}
