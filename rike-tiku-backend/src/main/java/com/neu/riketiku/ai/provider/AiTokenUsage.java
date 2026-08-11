package com.neu.riketiku.ai.provider;

public record AiTokenUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
    public static AiTokenUsage unknown() {
        return new AiTokenUsage(null, null, null);
    }
}
