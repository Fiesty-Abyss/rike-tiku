package com.neu.riketiku.aixuesheng;

import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;

public interface StudentAiProviderClient {
    AiModelResult generate(AiModelRequest request);
    default AiModelResult generate(AiModelRequest request, Long safeConfigId) { return generate(request); }
}
