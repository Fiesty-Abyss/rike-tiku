package com.riketiku.aixuesheng;

import com.riketiku.ai.provider.AiModelRequest;
import com.riketiku.ai.provider.AiModelResult;

public interface StudentAiProviderClient {
    AiModelResult generate(AiModelRequest request);
    default AiModelResult generate(AiModelRequest request, Long safeConfigId) { return generate(request); }
}
