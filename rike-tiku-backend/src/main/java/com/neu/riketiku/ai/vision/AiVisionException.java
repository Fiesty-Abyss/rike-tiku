package com.neu.riketiku.ai.vision;

import com.neu.riketiku.ai.provider.AiProviderErrorType;

public class AiVisionException extends RuntimeException {
    private final AiProviderErrorType errorType;
    private final Integer httpStatus;
    private final String providerErrorCode;
    private final Long retryAfterSeconds;
    public AiVisionException(AiProviderErrorType errorType, String message) { this(errorType,message,null,null); }
    public AiVisionException(AiProviderErrorType errorType, String message, Throwable cause) { this(errorType,message,null,cause); }
    public AiVisionException(AiProviderErrorType errorType, String message,Integer httpStatus) { this(errorType,message,httpStatus,null); }
    public AiVisionException(AiProviderErrorType errorType,String message,Integer httpStatus,String providerErrorCode,Long retryAfterSeconds){
        this(errorType,message,httpStatus,providerErrorCode,retryAfterSeconds,null);
    }
    private AiVisionException(AiProviderErrorType errorType,String message,Integer httpStatus,Throwable cause){this(errorType,message,httpStatus,null,null,cause);}
    private AiVisionException(AiProviderErrorType errorType,String message,Integer httpStatus,String providerErrorCode,Long retryAfterSeconds,Throwable cause){super(message,cause);this.errorType=errorType;this.httpStatus=httpStatus;this.providerErrorCode=providerErrorCode;this.retryAfterSeconds=retryAfterSeconds;}
    public AiProviderErrorType errorType() { return errorType; }
    public Integer httpStatus(){return httpStatus;}
    public String providerErrorCode(){return providerErrorCode;}
    public Long retryAfterSeconds(){return retryAfterSeconds;}
}
