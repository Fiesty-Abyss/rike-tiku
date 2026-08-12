package com.neu.riketiku.ai.vision;

import com.neu.riketiku.ai.provider.AiTokenUsage;
import java.util.List;

public final class FakeVisionProvider implements AiVisionProvider {
    private final AiVisionResult result;
    public FakeVisionProvider() {
        this.result = new AiVisionResult("fake-vision", "fake-vision-v1",
                new AiVisionContext("SCIENTIFIC_DIAGRAM", "确定性测试图像摘要", List.of("A"),
                        List.of("A 与 B 相连"), List.of()), new AiTokenUsage(20, 30, 50));
    }
    @Override public String providerCode() { return result.provider(); }
    @Override public String modelCode() { return result.model(); }
    @Override public AiVisionResult analyze(AiVisionRequest request) { return result; }
}
