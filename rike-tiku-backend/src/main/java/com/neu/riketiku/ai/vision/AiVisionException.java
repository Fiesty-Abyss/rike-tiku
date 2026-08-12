package com.neu.riketiku.ai.vision;

import com.neu.riketiku.ai.provider.AiProviderErrorType;

public class AiVisionException extends RuntimeException {
    private final AiProviderErrorType errorType;
    private final Integer httpStatus;
    public AiVisionException(AiProviderErrorType errorType, String message) { this(errorType,message,null,null); }
    public AiVisionException(AiProviderErrorType errorType, String message, Throwable cause) { this(errorType,message,null,cause); }
    public AiVisionException(AiProviderErrorType errorType, String message,Integer httpStatus) { this(errorType,message,httpStatus,null); }
    private AiVisionException(AiProviderErrorType errorType,String message,Integer httpStatus,Throwable cause){super(message,cause);this.errorType=errorType;this.httpStatus=httpStatus;}
    public AiProviderErrorType errorType() { return errorType; }
    public Integer httpStatus(){return httpStatus;}
}
