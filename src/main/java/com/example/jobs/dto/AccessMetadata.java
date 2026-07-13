package com.example.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccessMetadata(
    @JsonProperty("advertiser_pricing_url") String advertiserPricingUrl,
    @JsonProperty("catalog_url") String catalogUrl,
    String description,
    @JsonProperty("docs_url") String docsUrl,
    String mode,
    @JsonProperty("register_url") String registerUrl
) {

}
