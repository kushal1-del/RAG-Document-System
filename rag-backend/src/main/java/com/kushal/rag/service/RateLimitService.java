package com.kushal.rag.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Simple in-memory rate limiter using the token bucket algorithm
 * (via Bucket4j). Keeps one bucket per IP address in memory.
 *
 * NOTE: this is in-memory, so it resets if the app restarts, and
 * doesn't share state across multiple server instances. That's
 * fine for a single-instance deployment; a multi-instance
 * production setup would use a shared store (e.g. Redis) instead —
 * a future upgrade, not needed right now.
 */
@Service
public class RateLimitService {

    @Value("${rate-limit.register.capacity}")
    private int capacity;

    @Value("${rate-limit.register.refill-minutes}")
    private int refillMinutes;

    // One bucket per IP address, kept in memory for the app's lifetime.
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns true if the given key (e.g. an IP address) still
     * has tokens available, and consumes one token if so.
     * Returns false if the caller has exceeded their limit.
     */
    public boolean tryConsume(String key) {

        Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket());

        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket() {

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(capacity, Duration.ofMinutes(refillMinutes))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}