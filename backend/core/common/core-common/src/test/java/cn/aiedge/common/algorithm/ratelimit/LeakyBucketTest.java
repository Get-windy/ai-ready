package cn.aiedge.common.algorithm.ratelimit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeakyBucket rate limiting algorithm
 */
public class LeakyBucketTest {

    @Test
    public void testBasicLeakyBucketFunctionality() {
        // Create a bucket with capacity 10 and leak rate 2 requests/second
        LeakyBucket bucket = new LeakyBucket(10, 2);

        // Initially, bucket should accept requests up to capacity
        assertTrue(bucket.request(), "Should accept first request");
        assertTrue(bucket.request(), "Should accept second request");
        assertTrue(bucket.request(), "Should accept third request");

        // Test with a larger bucket that's initially empty
        LeakyBucket largeBucket = new LeakyBucket(5, 1);

        // First 5 requests should be accepted
        for (int i = 0; i < 5; i++) {
            assertTrue(largeBucket.request(), "Request " + (i + 1) + " should be accepted");
        }

        // The 6th request should be rejected (bucket is full)
        assertFalse(largeBucket.request(), "6th request should be rejected - bucket full");
    }

    @Test
    public void testLeakRateLimiting() throws InterruptedException {
        LeakyBucket bucket = new LeakyBucket(2, 1); // Capacity 2, leak rate 1 req/sec

        // Fill the bucket
        assertTrue(bucket.request(), "First request should be accepted");
        assertTrue(bucket.request(), "Second request should be accepted");

        // Third request should be rejected immediately
        assertFalse(bucket.request(), "Third request should be rejected when bucket is full");

        // Wait for 1.5 seconds to allow 1.5 requests worth of "leakage"
        Thread.sleep(1500);

        // Now we should be able to add one more request
        assertTrue(bucket.request(), "Request should be accepted after waiting for leak time");
    }

    @Test
    public void testMultipleRequestsAtOnce() {
        LeakyBucket bucket = new LeakyBucket(5, 2); // Capacity 5, leak rate 2 req/sec

        // Single request should be accepted
        assertTrue(bucket.request(1), "Single request should be accepted");

        // Multiple requests up to capacity should be accepted
        assertTrue(bucket.request(3), "Three requests together should be accepted");
        assertEquals(4, bucket.getCurrentWaterLevel(), "Water level should be 4");

        // Adding more would exceed capacity
        assertFalse(bucket.request(2), "Adding 2 more should exceed capacity of 5");
    }

    @Test
    public void testTimeBasedLeaking() throws InterruptedException {
        LeakyBucket bucket = new LeakyBucket(5, 2); // Capacity 5, leak rate 2 req/sec

        // Fill the bucket
        assertTrue(bucket.request(5), "Should accept 5 requests initially");

        // Wait 1 second - should leak 2 units
        Thread.sleep(1000);
        assertTrue(bucket.request(2), "Should accept 2 requests after 1 second (2 leaked)");

        // Wait another 1.5 seconds - should leak 3 more units (2*1.5)
        Thread.sleep(1500);
        assertTrue(bucket.request(3), "Should accept 3 requests after additional 1.5 seconds");
    }

    @Test
    public void testNoImmediateRejectionAfterWaiting() throws InterruptedException {
        LeakyBucket bucket = new LeakyBucket(3, 1); // Capacity 3, leak rate 1 req/sec

        // Fill the bucket
        assertTrue(bucket.request());
        assertTrue(bucket.request());
        assertTrue(bucket.request());

        // Should not accept more requests immediately
        assertFalse(bucket.request(), "Should not accept request when bucket is full");

        // Wait for 2 seconds - should leak 2 units, allowing 2 new requests
        Thread.sleep(2000);
        assertTrue(bucket.request(), "Should accept request after waiting 2 seconds");
        assertTrue(bucket.request(), "Should accept second request after waiting");
        assertFalse(bucket.request(), "Should not accept third request - bucket full again");
    }

    @Test
    public void testCapacityAndLeakRateGetters() {
        LeakyBucket bucket = new LeakyBucket(10, 5);

        assertEquals(10, bucket.getCapacity(), "Capacity should be 10");
        assertEquals(5, bucket.getLeakRate(), "Leak rate should be 5");
    }

    @Test
    public void testGetCurrentWaterLevel() throws InterruptedException {
        LeakyBucket bucket = new LeakyBucket(10, 2);

        // Initially should be 0
        assertEquals(0, bucket.getCurrentWaterLevel(), "Initial water level should be 0");

        // Add some requests
        assertTrue(bucket.request(3));
        assertEquals(3, bucket.getCurrentWaterLevel(), "Water level should be 3 after adding 3");

        // Wait and check that level decreases due to leak
        Thread.sleep(1000); // 1 second with leak rate 2 = 2 units leaked
        long levelAfterLeak = bucket.getCurrentWaterLevel();
        assertTrue(levelAfterLeak <= 1, "Water level should decrease after 1 second with leak rate 2");
    }
}