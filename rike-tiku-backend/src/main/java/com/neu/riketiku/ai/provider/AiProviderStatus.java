package com.neu.riketiku.ai.provider;

public record AiProviderStatus(String providerCode, String modelCode, boolean available,
                               AiProviderErrorType errorType, String message) {
    public static AiProviderStatus available(String providerCode, String modelCode) {
        return new AiProviderStatus(providerCode, modelCode, true, null, "AVAILABLE");
    }

    public static AiProviderStatus unavailable(String providerCode, String modelCode,
                                               AiProviderErrorType type, String message) {
        return new AiProviderStatus(providerCode, modelCode, false, type, message);
    }
}
