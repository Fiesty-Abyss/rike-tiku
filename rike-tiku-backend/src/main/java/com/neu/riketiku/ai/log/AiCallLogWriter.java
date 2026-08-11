package com.neu.riketiku.ai.log;

import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderErrorType;

public interface AiCallLogWriter {
    void success(AiModelRequest request, AiModelResult result, long latencyMillis);
    void failure(AiModelRequest request, String provider, String model,
                 AiProviderErrorType errorType, long latencyMillis);
}
