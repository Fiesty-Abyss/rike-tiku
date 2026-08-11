package com.neu.riketiku.ai.provider;

import java.time.Duration;

public final class FakeAiModelProvider implements AiModelProvider {
    public enum Mode { SUCCESS, FAILURE, TIMEOUT }

    private final String model;
    private final Mode mode;
    private final Duration delay;
    private final String content;
    private final AiTokenUsage usage;

    public FakeAiModelProvider(String model, Mode mode, Duration delay, String content, AiTokenUsage usage) {
        this.model = model;
        this.mode = mode;
        this.delay = delay == null ? Duration.ZERO : delay;
        this.content = content;
        this.usage = usage == null ? AiTokenUsage.unknown() : usage;
    }

    public static FakeAiModelProvider successful() {
        return new FakeAiModelProvider("fake-fixed-model", Mode.SUCCESS, Duration.ZERO,
                "deterministic fake response", new AiTokenUsage(11, 7, 18));
    }

    @Override public String providerCode() { return "fake"; }
    @Override public String modelCode() { return model; }

    @Override
    public AiModelResult generate(AiModelRequest request) {
        pause();
        if (mode == Mode.FAILURE) {
            throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE,
                    "Fake AI provider failure");
        }
        if (mode == Mode.TIMEOUT) {
            throw new AiProviderException(AiProviderErrorType.TIMEOUT, "Fake AI provider timeout");
        }
        return new AiModelResult(providerCode(), modelCode(), content, usage, "stop");
    }

    @Override
    public AiProviderStatus status() { return AiProviderStatus.available(providerCode(), modelCode()); }

    private void pause() {
        if (delay.isZero() || delay.isNegative()) return;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE,
                    "Fake AI provider interrupted", exception);
        }
    }
}
