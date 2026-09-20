package com.movierec.ai.service;

import com.movierec.ai.client.LlmClient;
import com.movierec.ai.client.LlmClientException;
import com.movierec.ai.model.LlmRecommendation;
import com.movierec.ai.model.LlmRecommendationResult;
import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.model.RetrievedMovie;
import com.movierec.dto.response.AiAdvisorResponse;
import com.movierec.dto.response.AiMovieRecommendation;
import com.movierec.entity.Movie;
import com.movierec.service.AiUsageLimitService;
import com.movierec.service.SecurityAuditService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiAdvisorService {
    private final AiUsageLimitService usageLimitService;
    private final AiInputGuard inputGuard;
    private final LocalIntentParser localIntentParser;
    private final HybridMovieRetriever retriever;
    private final LlmClient llmClient;
    private final SecurityAuditService auditService;

    public AiAdvisorService(AiUsageLimitService usageLimitService, AiInputGuard inputGuard,
                            LocalIntentParser localIntentParser, HybridMovieRetriever retriever,
                            LlmClient llmClient, SecurityAuditService auditService) {
        this.usageLimitService = usageLimitService;
        this.inputGuard = inputGuard;
        this.localIntentParser = localIntentParser;
        this.retriever = retriever;
        this.llmClient = llmClient;
        this.auditService = auditService;
    }

    public AiAdvisorResponse advise(Long userId, String rawQuestion) {
        long startedAt = System.nanoTime();
        String question = rawQuestion.trim();
        usageLimitService.consume(userId);
        inputGuard.validate(userId, question);

        boolean degraded = false;
        String degradationReason = null;
        MovieIntent intent = localIntentParser.parse(question);
        try {
            intent = localIntentParser.merge(intent, llmClient.extractIntent(question));
        } catch (LlmClientException ex) {
            degraded = true;
            degradationReason = "intent_" + classify(ex);
        }

        List<RetrievedMovie> candidates = retriever.retrieve(userId, question, intent);
        if (candidates.isEmpty()) {
            audit(userId, "NO_CANDIDATES", startedAt, 0, 0);
            return new AiAdvisorResponse("本地片库中没有找到满足这些条件的电影，请尝试放宽类型、片长或评分要求。",
                    intent, List.of(), false, degraded, llmClient.providerName());
        }

        LlmRecommendationResult generated = null;
        try {
            generated = llmClient.recommend(question, candidates);
        } catch (LlmClientException ex) {
            degraded = true;
            degradationReason = "recommend_" + classify(ex);
        }

        List<AiMovieRecommendation> recommendations = validateAndMap(generated, candidates);
        if (recommendations.size() < Math.min(3, candidates.size())) {
            recommendations = List.of();
            if (generated != null) degradationReason = "recommend_invalid_references";
        }
        boolean aiGenerated = generated != null && !recommendations.isEmpty();
        if (!aiGenerated) {
            degraded = true;
            recommendations = fallback(candidates);
        }
        String answer = aiGenerated ? safeText(generated.answer(), 500)
                : "智能解释暂不可用，已按本地片库检索与推荐分数返回结果。";
        if (answer.isBlank()) answer = "已根据本地片库为你筛选以下电影。";
        audit(userId, degraded ? "DEGRADED" : "SUCCESS", startedAt,
                candidates.size(), recommendations.size(), degradationReason);
        return new AiAdvisorResponse(answer, intent, recommendations, aiGenerated, degraded, llmClient.providerName());
    }

    private List<AiMovieRecommendation> validateAndMap(LlmRecommendationResult result,
                                                         List<RetrievedMovie> candidates) {
        if (result == null || result.recommendations() == null) return List.of();
        Map<Long, RetrievedMovie> allowed = new LinkedHashMap<>();
        candidates.forEach(candidate -> allowed.put(candidate.movie().getId(), candidate));
        Set<Long> used = new HashSet<>();
        List<AiMovieRecommendation> values = new ArrayList<>();
        for (LlmRecommendation item : result.recommendations()) {
            if (item == null || item.movieId() == null || !used.add(item.movieId())) continue;
            RetrievedMovie candidate = allowed.get(item.movieId());
            if (candidate == null) continue;
            List<String> criteria = safeCriteria(item.matchedCriteria(), candidate.matchedCriteria());
            values.add(toResponse(candidate.movie(), safeText(item.reason(), 300), criteria));
            if (values.size() == 5) break;
        }
        return values;
    }

    private List<AiMovieRecommendation> fallback(List<RetrievedMovie> candidates) {
        return candidates.stream().limit(5).map(candidate -> toResponse(candidate.movie(),
                fallbackReason(candidate), candidate.matchedCriteria())).toList();
    }

    private AiMovieRecommendation toResponse(Movie movie, String reason, List<String> criteria) {
        BigDecimal rating = movie.getDoubanRating() != null ? movie.getDoubanRating() : movie.getAvgRating();
        return new AiMovieRecommendation(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getGenre(),
                movie.getRuntime(), movie.getReleaseDate(), rating, reason, criteria,
                "/movie/" + movie.getId());
    }

    private String fallbackReason(RetrievedMovie candidate) {
        if (!candidate.matchedCriteria().isEmpty()) {
            return "符合你的条件：" + String.join("、", candidate.matchedCriteria()) + "。";
        }
        return "该片在本地片库的综合评分和热度较高。";
    }

    private List<String> safeCriteria(List<String> supplied, List<String> fallback) {
        List<String> source = supplied == null || supplied.isEmpty() ? fallback : supplied;
        return source.stream().map(value -> safeText(value, 60)).filter(value -> !value.isBlank())
                .distinct().limit(8).toList();
    }

    private String safeText(String value, int maxLength) {
        if (value == null) return "";
        String sanitized = value.replaceAll("<[^>]*>", "")
                .replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private void audit(Long userId, String outcome, long startedAt, int candidateCount, int resultCount) {
        audit(userId, outcome, startedAt, candidateCount, resultCount, null);
    }

    private void audit(Long userId, String outcome, long startedAt, int candidateCount, int resultCount,
                       String degradationReason) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        String metadata = "provider=" + safeMetadata(llmClient.providerName())
                + ",durationMs=" + durationMs + ",candidates=" + candidateCount + ",results=" + resultCount;
        if (degradationReason != null) metadata += ",reason=" + degradationReason;
        auditService.record("AI_MODEL_CALL", outcome, userId, "movie_advisor", metadata);
    }

    private String classify(LlmClientException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.net.http.HttpTimeoutException) return "timeout";
            current = current.getCause();
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("HTTP ")) return "http_" + message.substring(message.lastIndexOf("HTTP ") + 5).trim();
        if (message.contains("JSON")) return "invalid_json";
        if (message.contains("message.content")) return "invalid_response";
        return "client_error";
    }

    private String safeMetadata(String value) {
        if (value == null) return "unknown";
        return value.replaceAll("[^a-zA-Z0-9._:-]", "_");
    }
}
