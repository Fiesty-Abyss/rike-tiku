package com.neu.riketiku.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.vision")
public class VisionProviderProperties {
    private boolean enabled;
    private String provider = "glm";
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
    private String model = "glm-4.6v-flash";
    private String apiKey = "";
    private Duration requestTimeout = Duration.ofSeconds(30);
    private int retryCount = 1;
    private int maxTokens = 1000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public int getRetryCount() { return Math.min(1, Math.max(0, retryCount)); }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}
