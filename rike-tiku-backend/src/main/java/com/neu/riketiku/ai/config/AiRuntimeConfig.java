package com.neu.riketiku.ai.config;

import java.time.Duration;

public record AiRuntimeConfig(Long id, String provider, String model, String baseUrl, String apiKey,
                              String usage, boolean enabled, int maxTokens, Duration timeout,
                              int retryCount, boolean databaseBacked) {
    public String normalizedProvider() {
        return provider == null ? "" : provider.trim().toLowerCase();
    }
}
