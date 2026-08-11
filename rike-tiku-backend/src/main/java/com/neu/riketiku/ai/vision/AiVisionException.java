package com.neu.riketiku.ai.vision;

import com.neu.riketiku.ai.provider.AiProviderErrorType;

public class AiVisionException extends RuntimeException {
    private final AiProviderErrorType errorType;
    public AiVisionException(AiProviderErrorType errorType, String message) { super(message); this.errorType = errorType; }
    public AiVisionException(AiProviderErrorType errorType, String message, Throwable cause) { super(message, cause); this.errorType = errorType; }
    public AiProviderErrorType errorType() { return errorType; }
}
