package com.riketiku.ai.vision;

import com.riketiku.ai.provider.AiTokenUsage;

public record AiVisionResult(String provider, String model, AiVisionContext context, AiTokenUsage usage) { }
