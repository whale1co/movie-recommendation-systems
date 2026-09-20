package com.movierec.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.ai.config.AiProperties;
import com.movierec.ai.model.LlmRecommendationResult;
import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.model.RetrievedMovie;
import com.movierec.ai.service.PromptTemplateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleLlmClient implements LlmClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService prompts;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(AiProperties properties, ObjectMapper objectMapper,
                                     PromptTemplateService prompts) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.prompts = prompts;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .build();
    }

    @Override
    public MovieIntent extractIntent(String question) {
        String json = complete(prompts.intentPrompt(), question);
        try {
            return objectMapper.readValue(extractJsonObject(json), MovieIntent.class).normalized();
        } catch (JsonProcessingException ex) {
            throw new LlmClientException("模型返回的意图 JSON 无效", ex);
        }
    }

    @Override
    public LlmRecommendationResult recommend(String question, List<RetrievedMovie> candidates) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("question", question);
        input.put("candidates", candidates.stream().map(candidate -> {
            var movie = candidate.movie();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("movieId", movie.getId());
            value.put("title", movie.getTitle());
            value.put("genre", movie.getGenre());
            value.put("runtime", movie.getRuntime());
            value.put("releaseDate", movie.getReleaseDate());
            value.put("rating", movie.getDoubanRating());
            value.put("director", movie.getDirector());
            value.put("summary", limit(movie.getSummary(), 500));
            value.put("matchedCriteria", candidate.matchedCriteria());
            return value;
        }).toList());
        try {
            String json = complete(prompts.recommendationPrompt(), objectMapper.writeValueAsString(input));
            return objectMapper.readValue(extractJsonObject(json), LlmRecommendationResult.class);
        } catch (JsonProcessingException ex) {
            throw new LlmClientException("模型返回的推荐 JSON 无效", ex);
        }
    }

    @Override
    public String providerName() {
        return "openai-compatible:" + properties.getModel();
    }

    private String complete(String systemPrompt, String userPrompt) {
        validateConfiguration();
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxOutputTokens(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(chatCompletionsUri())
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json");
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + properties.getApiKey());
            }
            HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(body))).build();

            int attempts = Math.max(1, properties.getMaxRetries() + 1);
            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode message = root.path("choices").path(0).path("message");
                        String content = messageText(message.path("content"));
                        if (!content.isBlank()) return content;
                        String reasoningContent = messageText(message.path("reasoning_content"));
                        if (!reasoningContent.isBlank()) return reasoningContent;
                        throw new LlmClientException("模型响应缺少 message.content");
                    }
                    if (attempt == attempts || (response.statusCode() < 500 && response.statusCode() != 429)) {
                        throw new LlmClientException("模型服务返回 HTTP " + response.statusCode());
                    }
                } catch (IOException ex) {
                    if (attempt == attempts) throw new LlmClientException("模型调用失败", ex);
                }
            }
            throw new LlmClientException("模型服务不可用");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("模型调用被中断", ex);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new LlmClientException("模型调用失败", ex);
        }
    }

    private URI chatCompletionsUri() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return URI.create(baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions");
    }

    private void validateConfiguration() {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()
                || properties.getModel() == null || properties.getModel().isBlank()) {
            throw new LlmClientException("LLM_BASE_URL 和 LLM_MODEL 必须配置");
        }
    }

    private String extractJsonObject(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                trimmed = trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return start >= 0 && end > start ? trimmed.substring(start, end + 1) : trimmed;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private String messageText(JsonNode value) {
        if (value.isTextual()) return value.asText().trim();
        if (!value.isArray()) return "";
        StringBuilder content = new StringBuilder();
        for (JsonNode item : value) {
            String text = item.isTextual() ? item.asText() : item.path("text").asText("");
            if (!text.isBlank()) content.append(text);
        }
        return content.toString().trim();
    }
}
