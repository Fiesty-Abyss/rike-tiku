package com.neu.riketiku.ai.vision;

import com.neu.riketiku.ai.provider.AiTokenUsage;

public record AiVisionResult(String provider, String model, AiVisionContext context, AiTokenUsage usage) { }
