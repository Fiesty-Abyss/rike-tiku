package com.neu.riketiku.ai.provider;

public interface AiModelProvider {
    String providerCode();
    String modelCode();
    AiModelResult generate(AiModelRequest request);
    AiProviderStatus status();
}
