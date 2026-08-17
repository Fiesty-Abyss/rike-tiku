package com.neu.riketiku.ai.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.config.AiRuntimeConfig;
import java.net.http.HttpClient;
import org.springframework.stereotype.Component;

@Component
public class AiVisionProviderFactory {
    public AiVisionProvider create(AiRuntimeConfig config) {
        if ("fake".equals(config.normalizedProvider())) return new FakeVisionProvider();
        HttpClient client = HttpClient.newBuilder().connectTimeout(config.timeout()).build();
        if ("xai".equals(config.normalizedProvider())) return new XaiVisionProvider(config,client,new ObjectMapper());
        if (!"glm".equals(config.normalizedProvider())) throw new AiVisionException(
                com.neu.riketiku.ai.provider.AiProviderErrorType.CONFIGURATION_ERROR, "Unsupported vision provider");
        return new GlmVisionProvider(config, client, new ObjectMapper());
    }
}
