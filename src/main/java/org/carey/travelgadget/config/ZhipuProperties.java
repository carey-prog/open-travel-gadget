package org.carey.travelgadget.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zhipu")
public record ZhipuProperties(String apiKey, String baseUrl) {
}
