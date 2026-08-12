package com.neu.riketiku.ai.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class AiModelConfigDtos {
    private AiModelConfigDtos() { }

    public record Save(@NotBlank String provider, @NotBlank String model, @NotBlank String baseUrl,
                       String apiKey, @NotBlank String usage, @NotNull Boolean enabled,
                       @NotNull Boolean defaultConfig, @NotNull @Min(1000) @Max(120000) Integer timeoutMillis,
                       @NotNull @Min(64) @Max(8192) Integer maxTokens,
                       @NotNull @Min(0) @Max(1) Integer retryCount) { }

    public record Item(Long id, String provider, String model, String baseUrl, String usage,
                       boolean enabled, boolean defaultConfig, int timeoutMillis, int maxTokens,
                       int retryCount, boolean apiKeyConfigured, String lastTestStatus,
                       Long lastTestLatencyMillis, LocalDateTime lastTestAt,
                       LocalDateTime createdAt, LocalDateTime updatedAt) { }

    public record ConnectionResult(boolean success, String provider, String model, long latencyMillis,
                                   String status, String visionSummaryPreview, String safeError,
                                   String safeErrorCode,Integer httpStatus,LocalDateTime testedAt) { }

    public record Page(List<Item> records) { }
}
