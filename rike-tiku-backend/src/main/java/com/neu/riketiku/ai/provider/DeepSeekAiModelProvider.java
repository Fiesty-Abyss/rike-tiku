package com.neu.riketiku.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.config.AiProviderProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DeepSeekAiModelProvider implements AiModelProvider {
    private static final String PROVIDER = "deepseek";
    private final AiProviderProperties properties;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public DeepSeekAiModelProvider(AiProviderProperties properties, HttpClient client, ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerCode() { return PROVIDER; }

    @Override
    public String modelCode() { return properties.getModel(); }

    @Override
    public AiProviderStatus status() {
        if (!properties.isEnabled()) {
            return AiProviderStatus.unavailable(providerCode(), modelCode(), AiProviderErrorType.DISABLED,
                    "AI provider is disabled");
        }
        try {
            validateConfiguration();
            return AiProviderStatus.available(providerCode(), modelCode());
        } catch (AiProviderException exception) {
            return AiProviderStatus.unavailable(providerCode(), modelCode(), exception.errorType(), exception.getMessage());
        }
    }

    @Override
    public AiModelResult generate(AiModelRequest request) {
        if (!properties.isEnabled()) {
            throw new AiProviderException(AiProviderErrorType.DISABLED, "AI provider is disabled");
        }
        validateConfiguration();
        HttpRequest httpRequest = buildRequest(request);
        int maxAttempts = properties.getRetryCount() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) return mapSuccess(response.body());
                AiProviderException failure = mapHttpFailure(response.statusCode());
                if (attempt < maxAttempts && retryable(failure.errorType())) continue;
                throw failure;
            } catch (HttpTimeoutException exception) {
                if (attempt < maxAttempts) continue;
                throw new AiProviderException(AiProviderErrorType.TIMEOUT, "AI provider request timed out", exception);
            } catch (IOException exception) {
                if (attempt < maxAttempts) continue;
                throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE,
                        "AI provider is temporarily unavailable", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE,
                        "AI provider request was interrupted", exception);
            }
        }
        throw new AiProviderException(AiProviderErrorType.UNKNOWN, "AI provider request failed");
    }

    private void validateConfiguration() {
        if (blank(properties.getBaseUrl()) || blank(properties.getModel()) || blank(properties.getApiKey())) {
            throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                    "AI provider configuration is incomplete");
        }
        if (properties.getConnectTimeout() == null || properties.getConnectTimeout().isZero()
                || properties.getConnectTimeout().isNegative() || properties.getRequestTimeout() == null
                || properties.getRequestTimeout().isZero() || properties.getRequestTimeout().isNegative()) {
            throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                    "AI provider timeout configuration is invalid");
        }
        try {
            URI.create(endpoint());
        } catch (IllegalArgumentException exception) {
            throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                    "AI provider base URL is invalid");
        }
    }

    private HttpRequest buildRequest(AiModelRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("messages", request.messages());
            body.put("stream", false);
            if (request.jsonOutput()) body.put("response_format", Map.of("type", "json_object"));
            return HttpRequest.newBuilder(URI.create(endpoint()))
                    .timeout(properties.getRequestTimeout())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                    "AI request could not be encoded", exception);
        }
    }

    private AiModelResult mapSuccess(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            JsonNode first = choices.isArray() && !choices.isEmpty() ? choices.get(0) : null;
            JsonNode content = first == null ? null : first.path("message").path("content");
            if (content == null || !content.isTextual() || content.asText().isBlank()) {
                throw new AiProviderException(AiProviderErrorType.INVALID_RESPONSE,
                        "AI provider returned an invalid response");
            }
            String responseModel = textOrDefault(root.path("model"), modelCode());
            JsonNode usage = root.path("usage");
            AiTokenUsage tokenUsage = new AiTokenUsage(integerOrNull(usage.path("prompt_tokens")),
                    integerOrNull(usage.path("completion_tokens")), integerOrNull(usage.path("total_tokens")));
            return new AiModelResult(providerCode(), responseModel, content.asText(), tokenUsage,
                    first.path("finish_reason").isTextual() ? first.path("finish_reason").asText() : null);
        } catch (JsonProcessingException exception) {
            throw new AiProviderException(AiProviderErrorType.INVALID_RESPONSE,
                    "AI provider returned an invalid response", exception);
        }
    }

    private AiProviderException mapHttpFailure(int status) {
        if (status == 401 || status == 403) {
            return new AiProviderException(AiProviderErrorType.AUTHENTICATION_ERROR,
                    "AI provider authentication failed");
        }
        if (status == 429) {
            return new AiProviderException(AiProviderErrorType.RATE_LIMITED,
                    "AI provider rate limit reached");
        }
        if (status >= 500 && status <= 599) {
            return new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE,
                    "AI provider is temporarily unavailable");
        }
        return new AiProviderException(AiProviderErrorType.UNKNOWN, "AI provider rejected the request");
    }

    private boolean retryable(AiProviderErrorType type) {
        return type == AiProviderErrorType.RATE_LIMITED || type == AiProviderErrorType.PROVIDER_UNAVAILABLE;
    }

    private String endpoint() {
        String base = properties.getBaseUrl().trim();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/chat/completions";
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private Integer integerOrNull(JsonNode node) { return node != null && node.canConvertToInt() ? node.intValue() : null; }
    private String textOrDefault(JsonNode node, String fallback) { return node.isTextual() ? node.asText() : fallback; }
}
