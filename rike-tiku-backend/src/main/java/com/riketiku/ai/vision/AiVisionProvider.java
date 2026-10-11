package com.riketiku.ai.vision;

public interface AiVisionProvider {
    String providerCode();
    String modelCode();
    AiVisionResult analyze(AiVisionRequest request);
}
