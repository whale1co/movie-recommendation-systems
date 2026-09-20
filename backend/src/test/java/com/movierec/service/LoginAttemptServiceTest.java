package com.movierec.service;

import com.movierec.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {
    @Test
    void blocksUsernameAndIpAfterConfiguredFailures() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC);
        LoginAttemptService service = new LoginAttemptService(2, Duration.ofMinutes(15),
                Duration.ofMinutes(15), clock);

        service.recordFailure("alice", "127.0.0.1");
        assertDoesNotThrow(() -> service.checkAllowed("alice", "127.0.0.1"));
        service.recordFailure("alice", "127.0.0.1");

        assertThrows(TooManyRequestsException.class,
                () -> service.checkAllowed("alice", "127.0.0.1"));
        assertDoesNotThrow(() -> service.checkAllowed("alice", "127.0.0.2"));
    }
}
