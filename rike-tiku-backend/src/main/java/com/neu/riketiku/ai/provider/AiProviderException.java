package com.neu.riketiku.ai.provider;

public final class AiProviderException extends RuntimeException {
    private final AiProviderErrorType errorType;

    public AiProviderException(AiProviderErrorType errorType, String safeMessage) {
        super(safeMessage);
        this.errorType = errorType;
    }

    public AiProviderException(AiProviderErrorType errorType, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.errorType = errorType;
    }

    public AiProviderErrorType errorType() {
        return errorType;
    }
}
