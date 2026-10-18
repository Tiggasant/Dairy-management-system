package com.ghunsai.dairy.security;

import com.ghunsai.dairy.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection: after {@value #MAX_FAILURES} wrong passwords the
 * (client address + username) pair is blocked for {@link #LOCK_DURATION}.
 * Keyed by address AND username so one attacker cannot lock a real worker out from other devices.
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Entry(int failures, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    public void assertNotLocked(String key) {
        Entry entry = attempts.get(key);
        if (entry != null && entry.lockedUntil() != null) {
            Instant now = Instant.now();
            if (entry.lockedUntil().isAfter(now)) {
                long minutes = Math.max(1, Duration.between(now, entry.lockedUntil()).toMinutes() + 1);
                throw new ApiException.TooManyRequests(
                        "Too many failed login attempts. Try again in " + minutes + " minute(s)");
            }
            attempts.remove(key, entry);
        }
    }

    public void recordFailure(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            Instant now = Instant.now();
            attempts.entrySet().removeIf(e -> e.getValue().lockedUntil() != null
                    && e.getValue().lockedUntil().isBefore(now));
        }
        attempts.compute(key, (k, old) -> {
            int failures = (old == null ? 0 : old.failures()) + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? Instant.now().plus(LOCK_DURATION) : null;
            return new Entry(failures, lockedUntil);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
