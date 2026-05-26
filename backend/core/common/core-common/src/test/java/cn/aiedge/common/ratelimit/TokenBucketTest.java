package cn.aiedge.common.ratelimit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TokenBucketTest {

    @Test
    public void testBasicTokenConsumption() {
        TokenBucket bucket = new TokenBucket(10, 1.0); // Capacity 10, refill 1 token/sec
        
        // Initially should have full capacity
        assertTrue(bucket.tryConsume(5));
        assertEquals(5, bucket.getCurrentTokens());
        
        assertTrue(bucket.tryConsume(3));
        assertEquals(2, bucket.getCurrentTokens());
        
        // Try to consume more than available
        assertFalse(bucket.tryConsume(5));
        assertEquals(2, bucket.getCurrentTokens());
    }

    @Test
    public void testTokenRefill() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(5, 2.0); // Capacity 5, refill 2 tokens/sec
        
        // Consume all tokens
        assertTrue(bucket.tryConsume(5));
        assertFalse(bucket.tryConsume(1));
        
        // Wait for refill (should add ~2 tokens after 1 second)
        Thread.sleep(1100);
        
        assertTrue(bucket.tryConsume(1)); // Should succeed after refill
        assertTrue(bucket.tryConsume(1)); // Should succeed with remaining tokens
        assertFalse(bucket.tryConsume(1)); // Should fail as only 1 token would remain
    }

    @Test
    public void testCapacityLimit() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(5, 1.0); // Capacity 5, refill 1 token/sec
        
        // Consume all tokens
        assertTrue(bucket.tryConsume(5));
        assertEquals(0, bucket.getCurrentTokens());
        
        // Wait much longer than refill time to ensure bucket reaches capacity
        Thread.sleep(10000);
        
        // Bucket should not exceed capacity
        assertEquals(5, bucket.getCurrentTokens());
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(100, 10.0); // Capacity 100, refill 10 tokens/sec
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger successes = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Launch threads that attempt to consume tokens
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    // Each thread tries to consume 10 tokens
                    if (bucket.tryConsume(10)) {
                        successes.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify that some attempts succeeded but not all (since we have 100 tokens max)
        assertTrue(successes.get() > 0, "Some threads should have succeeded");
        assertTrue(successes.get() <= 10, "No more than 10 threads should succeed (100 tokens / 10 per thread)");
    }

    @Test
    public void testZeroTokensRequest() {
        TokenBucket bucket = new TokenBucket(10, 1.0);
        
        // Requesting 0 tokens should always succeed
        assertTrue(bucket.tryConsume(0));
        assertEquals(10, bucket.getCurrentTokens());
    }

    @Test
    public void testLargeTokenRequest() {
        TokenBucket bucket = new TokenBucket(5, 1.0);
        
        // Requesting more tokens than capacity should always fail
        assertFalse(bucket.tryConsume(10));
        assertEquals(5, bucket.getCurrentTokens());
    }
}