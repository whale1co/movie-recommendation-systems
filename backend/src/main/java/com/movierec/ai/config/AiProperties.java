package com.movierec.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private String provider = "fake";
    private String baseUrl = "http://localhost:11434/v1";
    private String apiKey = "";
    private String model = "qwen2.5:7b";
    private int timeoutSeconds = 20;
    private int maxRetries = 1;
    private int maxOutputTokens = 2048;
    private double temperature = 0.2;
    private int retrievalLimit = 8;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getRetrievalLimit() { return retrievalLimit; }
    public void setRetrievalLimit(int retrievalLimit) { this.retrievalLimit = retrievalLimit; }
}
