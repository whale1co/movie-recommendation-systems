package com.movierec.dto.response;

import java.time.LocalDateTime;

public record AiAdvisorHistorySummary(Long id, String question, String answerPreview,
                                      int recommendationCount, boolean aiGenerated,
                                      boolean degraded, LocalDateTime createdAt) {
}
