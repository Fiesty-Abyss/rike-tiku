package com.neu.riketiku.ai.vision;

import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import com.neu.riketiku.ai.log.AiCallLogWriter;
import com.neu.riketiku.ai.provider.AiMessage;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiThinkingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiVisionService {
    private final AiRuntimeConfigurationService configurations;
    private final AiVisionProviderFactory factory;
    private final AiCallLogWriter logWriter;
    public AiVisionService(AiRuntimeConfigurationService configurations, AiVisionProviderFactory factory,
                           AiCallLogWriter logWriter) {
        this.configurations=configurations; this.factory=factory; this.logWriter=logWriter;
    }
    public AiVisionResult analyze(AiVisionRequest request) {
        AiRuntimeConfig config = configurations.vision();
        AiVisionProvider provider = factory.create(config);
        AiModelRequest logRequest = new AiModelRequest(List.of(new AiMessage("user", "[VISION_METADATA_ONLY]")),
                request.purpose(), "question:"+request.questionId(), true, config.maxTokens(), AiThinkingMode.DISABLED);
        long started=System.nanoTime();
        try {
            AiVisionResult result=provider.analyze(request);
            logWriter.success(logRequest, new AiModelResult(result.provider(),result.model(),"[REDACTED]",result.usage(),"stop"),elapsed(started));
            return result;
        } catch (AiVisionException exception) {
            logWriter.failure(logRequest,provider.providerCode(),provider.modelCode(),exception.errorType(),elapsed(started));
            throw exception;
        }
    }
    private long elapsed(long started){return (System.nanoTime()-started)/1_000_000;}
}
