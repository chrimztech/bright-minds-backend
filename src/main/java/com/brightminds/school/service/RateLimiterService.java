package com.brightminds.school.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, per-key request limiter — single-instance, in-memory. Fine for this app's
 * current deployment (no clustering); a bucket's count simply resets once its window has
 * elapsed since it first opened. Not meant to survive a restart or to be shared across nodes.
 */
@Service
public class RateLimiterService {

    private static class Bucket {
        volatile long windowStart = System.currentTimeMillis();
        int count = 0;
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (bucket) {
            long now = System.currentTimeMillis();
            if (now - bucket.windowStart > window.toMillis()) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            if (bucket.count >= maxAttempts) {
                return false;
            }
            bucket.count++;
            return true;
        }
    }

    // Behind a reverse proxy (as in production) the real client address is in X-Forwarded-For,
    // not getRemoteAddr() — which would otherwise report the proxy's own address for every
    // caller and collapse everyone into a single rate-limit bucket.
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Bucket count only ever grows with distinct keys (one per client IP seen); without pruning,
    // an app left running for weeks would accumulate one entry per IP forever. Idle buckets
    // (nothing consumed for well past any window this service is used with) are dropped hourly.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    void pruneIdleBuckets() {
        long cutoff = System.currentTimeMillis() - Duration.ofHours(2).toMillis();
        buckets.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
    }
}
