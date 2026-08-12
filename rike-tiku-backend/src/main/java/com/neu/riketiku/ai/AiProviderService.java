package com.neu.riketiku.ai;

import com.neu.riketiku.ai.config.AiProviderProperties;
import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import com.neu.riketiku.ai.config.AiTextProviderFactory;
import com.neu.riketiku.ai.log.AiCallLogWriter;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import com.neu.riketiku.ai.provider.AiProviderException;
import com.neu.riketiku.ai.provider.AiProviderStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AiProviderService {
    private final AiProviderProperties properties;
    private final AiModelProvider provider;
    private final AiCallLogWriter logWriter;
    private final AiRuntimeConfigurationService runtimeConfigurations;
    private final AiTextProviderFactory providerFactory;

    public AiProviderService(AiProviderProperties properties, AiModelProvider provider, AiCallLogWriter logWriter) {
        this(properties, provider, logWriter, null, null);
    }

    @Autowired
    public AiProviderService(AiProviderProperties properties, AiModelProvider provider, AiCallLogWriter logWriter,
                             AiRuntimeConfigurationService runtimeConfigurations,
                             AiTextProviderFactory providerFactory) {
        this.properties = properties;
        this.provider = provider;
        this.logWriter = logWriter;
        this.runtimeConfigurations = runtimeConfigurations;
        this.providerFactory = providerFactory;
    }

    public AiModelResult generate(AiModelRequest request) {
        return generate(request, null);
    }

    public AiModelResult generate(AiModelRequest request, Long safeConfigId) {
        long started = System.nanoTime();
        AiRuntimeConfig runtime = safeConfigId == null ? runtime() : runtimeConfigurations.text(safeConfigId);
        AiModelProvider active = null;
        try {
            active = activeProvider(runtime);
            if (!runtime.enabled()) {
                throw new AiProviderException(AiProviderErrorType.DISABLED, "AI provider is disabled");
            }
            if (!active.providerCode().equalsIgnoreCase(runtime.provider())) {
                throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                        "Configured AI provider is unavailable");
            }
            AiModelResult result = active.generate(request);
            logWriter.success(request, result, elapsedMillis(started));
            return result;
        } catch (AiProviderException exception) {
            logWriter.failure(request, active == null ? runtime.normalizedProvider() : active.providerCode(),
                    active == null ? runtime.model() : active.modelCode(),
                    exception.errorType(), elapsedMillis(started));
            throw exception;
        }
    }

    public AiProviderStatus status() {
        AiRuntimeConfig runtime = runtime();
        AiModelProvider active;
        try {
            active = activeProvider(runtime);
        } catch (AiProviderException exception) {
            return AiProviderStatus.unavailable(runtime.normalizedProvider(), runtime.model(),
                    exception.errorType(), exception.getMessage());
        }
        if (!runtime.enabled()) {
            return AiProviderStatus.unavailable(active.providerCode(), active.modelCode(),
                    AiProviderErrorType.DISABLED, "AI provider is disabled");
        }
        if (!active.providerCode().equalsIgnoreCase(runtime.provider())) {
            return AiProviderStatus.unavailable(active.providerCode(), active.modelCode(),
                    AiProviderErrorType.CONFIGURATION_ERROR, "Configured AI provider is unavailable");
        }
        return active.status();
    }

    private AiRuntimeConfig runtime() {
        if (runtimeConfigurations != null) return runtimeConfigurations.text();
        return new AiRuntimeConfig(null, properties.getProvider(), properties.getModel(), properties.getBaseUrl(),
                properties.getApiKey(), "TEXT", properties.isEnabled(), 1200, properties.getRequestTimeout(),
                properties.getRetryCount(), false);
    }

    private AiModelProvider activeProvider(AiRuntimeConfig runtime) {
        if (runtime.databaseBacked() && providerFactory != null) {
            try { return providerFactory.create(runtime); }
            catch (IllegalArgumentException exception) {
                throw new AiProviderException(AiProviderErrorType.CONFIGURATION_ERROR,
                        "Configured AI provider is unavailable", exception);
            }
        }
        return provider;
    }

    private long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000; }
}
