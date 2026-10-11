package com.riketiku.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.riketiku.ai.config.AiProviderProperties;
import com.riketiku.ai.log.AiCallLogWriter;
import com.riketiku.ai.provider.AiModelRequest;
import com.riketiku.ai.provider.AiModelResult;
import com.riketiku.ai.provider.AiProviderErrorType;
import com.riketiku.ai.provider.AiProviderException;
import com.riketiku.ai.provider.FakeAiModelProvider;
import org.junit.jupiter.api.Test;

class AiProviderServiceTest {
    @Test
    void disabledProviderReturnsControlledFailureAndRecordsOnlyMetadata() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(false);
        RecordingLog log = new RecordingLog();
        AiProviderService service = new AiProviderService(properties, FakeAiModelProvider.successful(), log);

        assertThat(service.status().errorType()).isEqualTo(AiProviderErrorType.DISABLED);
        assertThatThrownBy(() -> service.generate(AiModelRequest.text("student-help", "private prompt")))
                .isInstanceOfSatisfying(AiProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.DISABLED));
        assertThat(log.errorType).isEqualTo(AiProviderErrorType.DISABLED);
        assertThat(log.promptWasStored).isFalse();
    }

    @Test
    void enabledFakeProviderSucceedsWithoutApiKey() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(true);
        properties.setProvider("fake");
        RecordingLog log = new RecordingLog();
        AiProviderService service = new AiProviderService(properties, FakeAiModelProvider.successful(), log);

        assertThat(service.generate(AiModelRequest.text("provider-test", "hello")).content())
                .isEqualTo("deterministic fake response");
        assertThat(log.success).isTrue();
    }

    private static final class RecordingLog implements AiCallLogWriter {
        private boolean success;
        private AiProviderErrorType errorType;
        private boolean promptWasStored;

        @Override public void success(AiModelRequest request, AiModelResult result, long latencyMillis) {
            success = true;
        }
        @Override public void failure(AiModelRequest request, String provider, String model,
                                      AiProviderErrorType errorType, long latencyMillis) {
            this.errorType = errorType;
        }
    }
}
