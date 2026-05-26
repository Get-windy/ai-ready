package cn.aiedge.cache.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for the rate limiting algorithms:
 * - Token Bucket
 * - Leaky Bucket
 * - Sliding Window Counter
 */
public class RateLimiterTest {

    @BeforeEach
    void setUp() {
        // Setup code if needed
    }

    @Test
    void testTokenBucketBasicFunctionality() {
        // Create a token bucket with capacity of 10 tokens and refill rate of 2 tokens/sec
        TokenBucket tokenBucket = new TokenBucket(10, 2.0);

        // Initially, should be able to consume up to the capacity
        assertTrue(tokenBucket.tryConsume(5), "Should be able to consume 5 tokens initially");
        assertEquals(5, tokenBucket.getCurrentTokens(), "Should have 5 tokens left");

        // Consume more tokens
        assertTrue(tokenBucket.tryConsume(3), "Should be able to consume 3 more tokens");
        assertEquals(2, tokenBucket.getCurrentTokens(), "Should have 2 tokens left");

        // Try to consume more than available
        assertFalse(tokenBucket.tryConsume(5), "Should not be able to consume 5 tokens when only 2 available");
    }

    @Test
    void testTokenBucketRefillMechanism() throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(5, 10.0); // 10 tokens/sec refill rate

        // Empty the bucket
        assertTrue(tokenBucket.tryConsume(5), "Should be able to consume 5 tokens");

        // Wait for refill
        Thread.sleep(100); // Sleep for 100ms, should add ~1 token (10*0.1)

        assertTrue(tokenBucket.tryConsume(1), "Should be able to consume 1 token after refill");
    }

    @Test
    void testLeakyBucketBasicFunctionality() {
        LeakyBucket leakyBucket = new LeakyBucket(10, 2.0); // 10 capacity, 2 requests/sec leak rate

        // Should be able to accept requests up to capacity
        assertTrue(leakyBucket.request(), "Should accept first request");
        assertTrue(leakyBucket.request(3), "Should accept 3 more requests");
        assertEquals(4, leakyBucket.getCurrentWaterLevel(), "Should have 4 requests in bucket");

        // Add more requests up to capacity
        assertTrue(leakyBucket.request(6), "Should accept 6 more requests to fill capacity");
        assertEquals(10, leakyBucket.getCurrentWaterLevel(), "Should have 10 requests in bucket");

        // Should reject additional requests
        assertFalse(leakyBucket.request(), "Should reject request when bucket is full");
    }

    @Test
    void testSlidingWindowCounterBasicFunctionality() {
        // Create a sliding window with limit of 5 requests per 1000ms
        SlidingWindowCounter slidingWindow = new SlidingWindowCounter(5, 1000L);

        // Should be able to increment up to the limit
        for (int i = 0; i < 5; i++) {
            assertTrue(slidingWindow.increment(), "Should allow increment " + (i + 1));
        }

        // Should reject further increments
        assertFalse(slidingWindow.increment(), "Should reject increment after reaching limit");
    }

    @Test
    void testRateLimiterInterfaceImplementation() {
        // Test TokenBucket implements RateLimiter
        RateLimiter tokenBucket = new TokenBucket(10, 2.0);
        assertTrue(tokenBucket.tryAcquire(), "TokenBucket should implement tryAcquire");
        assertTrue(tokenBucket.tryAcquire(2), "TokenBucket should implement tryAcquire with permits");
        assertEquals(7, tokenBucket.availablePermits(), "Available permits should match");

        // Test LeakyBucket implements RateLimiter
        RateLimiter leakyBucket = new LeakyBucket(10, 2.0);
        assertTrue(leakyBucket.tryAcquire(), "LeakyBucket should implement tryAcquire");
        assertTrue(leakyBucket.tryAcquire(2), "LeakyBucket should implement tryAcquire with permits");

        // Test SlidingWindowCounter implements RateLimiter
        RateLimiter slidingWindow = new SlidingWindowCounter(5, 1000L);
        assertTrue(slidingWindow.tryAcquire(), "SlidingWindow should implement tryAcquire");
        assertTrue(slidingWindow.tryAcquire(2), "SlidingWindow should implement tryAcquire with permits");
    }

    @Test
    void testRateLimiterFactory() {
        // Test Token Bucket creation
        RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(10, 2.0);
        assertNotNull(tokenBucket, "Token bucket should be created");
        assertTrue(tokenBucket instanceof TokenBucket, "Should be instance of TokenBucket");

        // Test Leaky Bucket creation
        RateLimiter leakyBucket = RateLimiterFactory.createLeakyBucket(10, 2.0);
        assertNotNull(leakyBucket, "Leaky bucket should be created");
        assertTrue(leakyBucket instanceof LeakyBucket, "Should be instance of LeakyBucket");

        // Test Sliding Window creation
        RateLimiter slidingWindow = RateLimiterFactory.createSlidingWindow(5, 1000L);
        assertNotNull(slidingWindow, "Sliding window should be created");
        assertTrue(slidingWindow instanceof SlidingWindowCounter, "Should be instance of SlidingWindowCounter");
    }

    @Test
    void testTokenBucketThreadSafety() throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(100, 10.0);
        int numThreads = 10;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit tasks to multiple threads
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    if (tokenBucket.tryConsume(1)) {
                        successCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        // Wait for all threads to complete
        latch.await();
        executor.shutdown();

        // Verify that total successful requests is reasonable
        assertTrue(successCount.get() > 0, "At least some requests should succeed");
        assertTrue(successCount.get() <= 100, "Should not exceed bucket capacity");
    }

    @Test
    void testLeakyBucketThreadSafety() throws InterruptedException {
        LeakyBucket leakyBucket = new LeakyBucket(100, 20.0);
        int numThreads = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit tasks to multiple threads
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    if (leakyBucket.request(1)) {
                        successCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        // Wait for all threads to complete
        latch.await();
        executor.shutdown();

        // The leaky bucket should handle the requests according to its rate
        assertTrue(successCount.get() > 0, "At least some requests should succeed");
    }

    @Test
    void testSlidingWindowThreadSafety() throws InterruptedException {
        SlidingWindowCounter slidingWindow = new SlidingWindowCounter(50, 2000L); // 50 requests per 2 seconds
        int numThreads = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit tasks to multiple threads
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    if (slidingWindow.increment(1)) {
                        successCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        // Wait for all threads to complete
        latch.await();
        executor.shutdown();

        // Verify that total successful requests is reasonable
        assertTrue(successCount.get() > 0, "At least some requests should succeed");
        assertTrue(successCount.get() <= 50, "Should not exceed window limit in short time");
    }

    @Test
    void testConfigInfoMethods() {
        TokenBucket tokenBucket = new TokenBucket(10, 2.0);
        String tokenConfig = tokenBucket.getConfigInfo();
        assertTrue(tokenConfig.contains("TokenBucket"), "Config should contain class name");
        assertTrue(tokenConfig.contains("capacity=10"), "Config should contain capacity");
        assertTrue(tokenConfig.contains("refillRate=2.00"), "Config should contain refill rate");

        LeakyBucket leakyBucket = new LeakyBucket(20, 5.0);
        String leakyConfig = leakyBucket.getConfigInfo();
        assertTrue(leakyConfig.contains("LeakyBucket"), "Config should contain class name");
        assertTrue(leakyConfig.contains("capacity=20"), "Config should contain capacity");
        assertTrue(leakyConfig.contains("leakRate=5.00"), "Config should contain leak rate");

        SlidingWindowCounter slidingWindow = new SlidingWindowCounter(15, 1000L);
        String slidingConfig = slidingWindow.getConfigInfo();
        assertTrue(slidingConfig.contains("SlidingWindowCounter"), "Config should contain class name");
        assertTrue(slidingConfig.contains("limit=15"), "Config should contain limit");
        assertTrue(slidingConfig.contains("windowSize=1000"), "Config should contain window size");
    }
}