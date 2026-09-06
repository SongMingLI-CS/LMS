package com.npu.lms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("key", 5, 60_000));
        }
        assertFalse(limiter.tryAcquire("key", 5, 60_000));
    }

    @Test
    void differentKeysAreIndependent() {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("a", 1, 60_000));
        assertTrue(limiter.tryAcquire("b", 1, 60_000));
        assertFalse(limiter.tryAcquire("a", 1, 60_000));
    }
}
