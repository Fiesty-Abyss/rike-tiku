package com.neu.riketiku.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.DeepSeekAiModelProvider;
import com.neu.riketiku.ai.provider.FakeAiModelProvider;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AiProviderProperties.class, VisionProviderProperties.class})
public class AiProviderConfiguration {
    @Bean
    AiModelProvider deepSeekAiModelProvider(AiProviderProperties properties) {
        if ("fake".equalsIgnoreCase(properties.getProvider())) return FakeAiModelProvider.successful();
        Duration configuredTimeout = properties.getConnectTimeout();
        Duration clientTimeout = configuredTimeout == null || configuredTimeout.isZero() || configuredTimeout.isNegative()
                ? Duration.ofSeconds(3) : configuredTimeout;
        HttpClient client = HttpClient.newBuilder().connectTimeout(clientTimeout).build();
        return new DeepSeekAiModelProvider(properties, client, new ObjectMapper());
    }
}
