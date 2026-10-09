package com.movierec.service;

import com.movierec.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiUsageLimitServiceTest {
    @Test
    void enforcesPerUserMinuteLimitAndAuditsRejection() {
        SecurityAuditService auditService = mock(SecurityAuditService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC);
        AiUsageLimitService service = new AiUsageLimitService(2, 10, clock, auditService);

        service.consume(7L);
        service.consume(7L);

        assertThrows(TooManyRequestsException.class, () -> service.consume(7L));
        verify(auditService).record("AI_RATE_LIMIT", "DENIED", 7L, null, "reason=minute_limit");
    }

    @Test
    void enforcesDailyQuota() {
        SecurityAuditService auditService = mock(SecurityAuditService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC);
        AiUsageLimitService service = new AiUsageLimitService(10, 1, clock, auditService);

        service.consume(8L);

        assertThrows(TooManyRequestsException.class, () -> service.consume(8L));
        verify(auditService).record("AI_RATE_LIMIT", "DENIED", 8L, null, "reason=daily_limit");
    }
}
