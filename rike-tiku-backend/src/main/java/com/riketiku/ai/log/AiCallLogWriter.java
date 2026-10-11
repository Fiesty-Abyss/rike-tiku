package com.riketiku.ai.log;

import com.riketiku.ai.provider.AiModelRequest;
import com.riketiku.ai.provider.AiModelResult;
import com.riketiku.ai.provider.AiProviderErrorType;

public interface AiCallLogWriter {
    void success(AiModelRequest request, AiModelResult result, long latencyMillis);
    void failure(AiModelRequest request, String provider, String model,
                 AiProviderErrorType errorType, long latencyMillis);
}
