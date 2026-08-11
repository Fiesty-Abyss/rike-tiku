package com.neu.riketiku.ai;

import com.neu.riketiku.ai.config.AiProviderProperties;
import com.neu.riketiku.ai.log.AiCallLogWriter;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import com.neu.riketiku.ai.provider.AiProviderException;
import com.neu.riketiku.ai.provider.AiProviderStatus;
import org.springframework.stereotype.Service;

@Service
public class AiProviderService {
    private final AiProviderProperties properties;
    private final AiModelProvider provider;
    private final AiCallLogWriter logWriter;

    public AiProviderService(AiProviderProperties properties, AiModelProvider provider, AiCallLogWriter logWriter) {
        this.properties = properties;
        this.provider = provider;
        this.logWriter = logWriter;
    }

    public AiModelResult generate(AiModelRequest request) {
        long started = System.nanoTime();
        try {
            if (!properties.isEnabled()) {
                throw new AiProviderException(AiProviderErrorType.DISABLED, "AI provider is disabled");
            }
            if (!provider.providerCode().equalsIgnoreCase(properties.getProvider())) {
                throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                        "Configured AI provider is unavailable");
            }
            AiModelResult result = provider.generate(request);
            logWriter.success(request, result, elapsedMillis(started));
            return result;
        } catch (AiProviderException exception) {
            logWriter.failure(request, provider.providerCode(), provider.modelCode(),
                    exception.errorType(), elapsedMillis(started));
            throw exception;
        }
    }

    public AiProviderStatus status() {
        if (!properties.isEnabled()) {
            return AiProviderStatus.unavailable(provider.providerCode(), provider.modelCode(),
                    AiProviderErrorType.DISABLED, "AI provider is disabled");
        }
        if (!provider.providerCode().equalsIgnoreCase(properties.getProvider())) {
            return AiProviderStatus.unavailable(provider.providerCode(), provider.modelCode(),
                    AiProviderErrorType.CONFIGURATION_ERROR, "Configured AI provider is unavailable");
        }
        return provider.status();
    }

    private long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000; }
}
