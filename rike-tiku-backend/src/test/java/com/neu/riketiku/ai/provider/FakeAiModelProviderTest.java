package com.neu.riketiku.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class FakeAiModelProviderTest {
    private static final AiModelRequest REQUEST = AiModelRequest.text("provider-test", "hello");

    @Test
    void successIsDeterministicAndReturnsFixedUsage() {
        AiModelResult result = FakeAiModelProvider.successful().generate(REQUEST);
        assertThat(result.providerCode()).isEqualTo("fake");
        assertThat(result.modelCode()).isEqualTo("fake-fixed-model");
        assertThat(result.content()).isEqualTo("deterministic fake response");
        assertThat(result.usage()).isEqualTo(new AiTokenUsage(11, 7, 18));
    }

    @Test
    void configuredFailureIsControlled() {
        FakeAiModelProvider provider = new FakeAiModelProvider("fake-model",
                FakeAiModelProvider.Mode.FAILURE, Duration.ZERO, "unused", AiTokenUsage.unknown());
        assertThatThrownBy(() -> provider.generate(REQUEST))
                .isInstanceOfSatisfying(AiProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.PROVIDER_UNAVAILABLE));
    }

    @Test
    void configuredDelayAndTimeoutAreDeterministic() {
        FakeAiModelProvider delayed = new FakeAiModelProvider("fake-model",
                FakeAiModelProvider.Mode.SUCCESS, Duration.ofMillis(30), "delayed", new AiTokenUsage(1, 2, 3));
        long started = System.nanoTime();
        assertThat(delayed.generate(REQUEST).content()).isEqualTo("delayed");
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(20));

        FakeAiModelProvider timeout = new FakeAiModelProvider("fake-model",
                FakeAiModelProvider.Mode.TIMEOUT, Duration.ZERO, "unused", AiTokenUsage.unknown());
        assertThatThrownBy(() -> timeout.generate(REQUEST))
                .isInstanceOfSatisfying(AiProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.TIMEOUT));
    }
}
