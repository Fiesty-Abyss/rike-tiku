package com.riketiku.ai.provider;

public record AiModelResult(String providerCode, String modelCode, String content,
                            AiTokenUsage usage, String finishReason) {
}
