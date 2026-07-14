package fhes.cat.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiProperties {

    @Value("${ai.openai.enabled:false}")
    private boolean enabled;
    @Value("${ai.openai.api-key:}")
    private String apiKey;
    @Value("${ai.openai.model:gpt-5.6-terra}")
    private String model;
    @Value("${ai.openai.url:https://api.openai.com/v1/responses}")
    private String url;
    @Value("${ai.openai.timeout-seconds:90}")
    private int timeoutSeconds;
    @Value("${ai.openai.max-image-bytes:10485760}")
    private long maxImageBytes;

    public boolean isEnabled() { return enabled; }
    public String getApiKey() { return apiKey == null ? "" : apiKey.trim(); }
    public String getModel() { return model; }
    public String getUrl() { return url; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public long getMaxImageBytes() { return maxImageBytes; }
}
