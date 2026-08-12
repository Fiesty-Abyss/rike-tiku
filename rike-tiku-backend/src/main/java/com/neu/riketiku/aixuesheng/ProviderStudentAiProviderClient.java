package com.neu.riketiku.aixuesheng;

import com.neu.riketiku.ai.AiProviderService;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import org.springframework.stereotype.Service;

@Service
public class ProviderStudentAiProviderClient implements StudentAiProviderClient {
    private final AiProviderService providerService;

    public ProviderStudentAiProviderClient(AiProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public AiModelResult generate(AiModelRequest request) {
        return providerService.generate(request);
    }

    @Override
    public AiModelResult generate(AiModelRequest request, Long safeConfigId) {
        return providerService.generate(request, safeConfigId);
    }
}
