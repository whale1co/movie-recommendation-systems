package com.movierec.service;

import com.movierec.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final ConcurrentHashMap<String, AttemptState> failures = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(@Value("${security.login.max-attempts:5}") int maxAttempts,
                               @Value("${security.login.window-seconds:900}") long windowSeconds,
                               @Value("${security.login.lock-seconds:900}") long lockSeconds) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Duration.ofSeconds(lockSeconds), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration window, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public void checkAllowed(String username, String ipAddress) {
        AttemptState state = failures.get(key(username, ipAddress));
        if (state == null) return;
        synchronized (state) {
            if (state.lockedUntil != null && state.lockedUntil.isAfter(clock.instant())) {
                throw new TooManyRequestsException("登录失败次数过多，请稍后重试");
            }
            if (state.lockedUntil != null) state.clear();
            purge(state.attempts);
        }
    }

    public void recordFailure(String username, String ipAddress) {
        AttemptState state = failures.computeIfAbsent(key(username, ipAddress), ignored -> new AttemptState());
        synchronized (state) {
            purge(state.attempts);
            state.attempts.addLast(clock.instant());
            if (state.attempts.size() >= maxAttempts) {
                state.lockedUntil = clock.instant().plus(lockDuration);
            }
        }
    }

    public void recordSuccess(String username, String ipAddress) {
        failures.remove(key(username, ipAddress));
    }

    private void purge(Deque<Instant> attempts) {
        Instant threshold = clock.instant().minus(window);
        while (!attempts.isEmpty() && attempts.getFirst().isBefore(threshold)) attempts.removeFirst();
    }

    private String key(String username, String ipAddress) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return normalized + '|' + (ipAddress == null ? "unknown" : ipAddress);
    }

    private static final class AttemptState {
        private final Deque<Instant> attempts = new ArrayDeque<>();
        private Instant lockedUntil;

        private void clear() {
            attempts.clear();
            lockedUntil = null;
        }
    }
}
