package com.movierec.dto.response;

import java.time.LocalDateTime;

public record AiAdvisorHistoryDetail(Long id, String question, AiAdvisorResponse result,
                                     LocalDateTime createdAt) {
}
