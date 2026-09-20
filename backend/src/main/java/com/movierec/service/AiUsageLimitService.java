package com.movierec.service;

import com.movierec.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AiUsageLimitService {
    private final ConcurrentHashMap<Long, Deque<Instant>> minuteUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> dailyUsage = new ConcurrentHashMap<>();
    private final int perMinute;
    private final int perDay;
    private final Clock clock;
    private final SecurityAuditService auditService;

    @Autowired
    public AiUsageLimitService(@Value("${security.ai.requests-per-minute:10}") int perMinute,
                               @Value("${security.ai.requests-per-day:100}") int perDay,
                               SecurityAuditService auditService) {
        this(perMinute, perDay, Clock.system(ZoneId.of("Asia/Shanghai")), auditService);
    }

    AiUsageLimitService(int perMinute, int perDay, Clock clock, SecurityAuditService auditService) {
        this.perMinute = perMinute;
        this.perDay = perDay;
        this.clock = clock;
        this.auditService = auditService;
    }

    public void consume(Long userId) {
        Deque<Instant> recent = minuteUsage.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (recent) {
            Instant threshold = clock.instant().minus(1, ChronoUnit.MINUTES);
            while (!recent.isEmpty() && recent.getFirst().isBefore(threshold)) recent.removeFirst();
            if (recent.size() >= perMinute) reject(userId, "minute_limit");

            String dailyKey = userId + ":" + LocalDate.now(clock);
            AtomicInteger count = dailyUsage.computeIfAbsent(dailyKey, ignored -> new AtomicInteger());
            if (count.get() >= perDay) reject(userId, "daily_limit");
            recent.addLast(clock.instant());
            count.incrementAndGet();
            dailyUsage.keySet().removeIf(key -> key.startsWith(userId + ":") && !key.equals(dailyKey));
        }
    }

    private void reject(Long userId, String reason) {
        auditService.record("AI_RATE_LIMIT", "DENIED", userId, null, "reason=" + reason);
        throw new TooManyRequestsException("AI 请求额度已用尽，请稍后重试");
    }
}
