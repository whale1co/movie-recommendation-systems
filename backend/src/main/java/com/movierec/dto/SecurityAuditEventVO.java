package com.movierec.dto;

import java.time.LocalDateTime;

public record SecurityAuditEventVO(
        Long id,
        Long actorUserId,
        String eventType,
        String outcome,
        String subject,
        String ipAddress,
        String requestId,
        String metadata,
        LocalDateTime createdAt
) {
}
