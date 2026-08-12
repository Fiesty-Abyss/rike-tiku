package com.neu.riketiku.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.DeepSeekAiModelProvider;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AiTextProviderFactory {
    public AiModelProvider create(AiRuntimeConfig runtime) {
        if (!"deepseek".equals(runtime.normalizedProvider())) {
            throw new IllegalArgumentException("Unsupported text provider");
        }
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(runtime.enabled());
        properties.setProvider(runtime.normalizedProvider());
        properties.setBaseUrl(runtime.baseUrl());
        properties.setModel(runtime.model());
        properties.setApiKey(runtime.apiKey());
        properties.setRequestTimeout(runtime.timeout());
        properties.setConnectTimeout(Duration.ofMillis(Math.min(5000, runtime.timeout().toMillis())));
        properties.setRetryCount(runtime.retryCount());
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new DeepSeekAiModelProvider(properties, client, new ObjectMapper());
    }
}
