package cn.aiedge.cache.ratelimit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance benchmarking for rate limiting algorithms.
 * Compares Token Bucket, Leaky Bucket, and Sliding Window Counter algorithms.
 */
public class RateLimiterBenchmark {
    
    private static final int NUM_THREADS = 10;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int WARMUP_REQUESTS = 100;
    
    public static void main(String[] args) {
        System.out.println("Starting Rate Limiter Performance Benchmark...\n");
        
        // Test Token Bucket
        System.out.println("=== Token Bucket Performance ===");
        RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(100, 100.0);
        benchmarkRateLimiter("Token Bucket", tokenBucket);
        
        // Test Leaky Bucket
        System.out.println("\n=== Leaky Bucket Performance ===");
        RateLimiter leakyBucket = RateLimiterFactory.createLeakyBucket(100, 100.0);
        benchmarkRateLimiter("Leaky Bucket", leakyBucket);
        
        // Test Sliding Window
        System.out.println("\n=== Sliding Window Performance ===");
        RateLimiter slidingWindow = RateLimiterFactory.createSlidingWindow(1000, 1000L); // 1000 requests per second
        benchmarkRateLimiter("Sliding Window", slidingWindow);
        
        System.out.println("\nBenchmark completed.");
        
        // Compare algorithms
        compareAlgorithms();
    }
    
    /**
     * Benchmarks a rate limiter implementation
     * @param name Name of the algorithm for reporting
     * @param rateLimiter The rate limiter to benchmark
     */
    public static void benchmarkRateLimiter(String name, RateLimiter rateLimiter) {
        try {
            // Warmup period
            System.out.println("Warming up...");
            warmup(rateLimiter);
            
            // Actual benchmark
            long startTime = System.nanoTime();
            
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            CountDownLatch latch = new CountDownLatch(NUM_THREADS);
            List<BenchmarkResult> results = new ArrayList<>();
            
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    BenchmarkResult result = new BenchmarkResult();
                    
                    long threadStartTime = System.nanoTime();
                    for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                        if (rateLimiter.tryAcquire()) {
                            result.successfulRequests.incrementAndGet();
                        } else {
                            result.failedRequests.incrementAndGet();
                        }
                        
                        // Track timing for throughput calculation
                        if (j % 100 == 0) {
                            result.sampleTimes.add(System.nanoTime());
                        }
                    }
                    long threadEndTime = System.nanoTime();
                    
                    result.threadExecutionTimeNs = threadEndTime - threadStartTime;
                    results.add(result);
                    latch.countDown();
                });
            }
            
            latch.await();
            executor.shutdown();
            
            long endTime = System.nanoTime();
            long totalTimeNs = endTime - startTime;
            double totalTimeSec = totalTimeNs / 1_000_000_000.0;
            
            // Aggregate results
            int totalSuccessful = results.stream()
                .mapToInt(r -> r.successfulRequests.get())
                .sum();
            int totalFailed = results.stream()
                .mapToInt(r -> r.failedRequests.get())
                .sum();
            int totalRequests = totalSuccessful + totalFailed;
            
            double throughput = totalRequests / totalTimeSec;
            double successRate = (double) totalSuccessful / totalRequests * 100;
            
            System.out.printf("Total Requests: %d%n", totalRequests);
            System.out.printf("Successful: %d (%.2f%%)%n", totalSuccessful, successRate);
            System.out.printf("Failed: %d (%.2f%%)%n", totalFailed, 100 - successRate);
            System.out.printf("Total Time: %.3f seconds%n", totalTimeSec);
            System.out.printf("Throughput: %.2f requests/sec%n", throughput);
            
            // Additional metrics
            System.out.printf("Avg Thread Execution Time: %.2f ms%n", 
                results.stream().mapToDouble(r -> r.threadExecutionTimeNs / 1_000_000.0).average().orElse(0.0));
            
            // Print configuration info
            System.out.println("Configuration: " + rateLimiter.getConfigInfo());
            
        } catch (Exception e) {
            System.err.println("Error during benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Performs a warmup run to prime the JVM and rate limiter
     * @param rateLimiter The rate limiter to warm up
     */
    private static void warmup(RateLimiter rateLimiter) {
        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            rateLimiter.tryAcquire();
        }
    }
    
    /**
     * Inner class to hold benchmark results for a single thread
     */
    private static class BenchmarkResult {
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        List<Long> sampleTimes = new CopyOnWriteArrayList<>();
        long threadExecutionTimeNs = 0;
    }
    
    /**
     * Compares all three rate limiting algorithms side by side
     */
    public static void compareAlgorithms() {
        System.out.println("\n=== Side-by-Side Algorithm Comparison ===");
        
        RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(50, 50.0);
        RateLimiter leakyBucket = RateLimiterFactory.createLeakyBucket(50, 50.0);
        RateLimiter slidingWindow = RateLimiterFactory.createSlidingWindow(500, 1000L);
        
        System.out.println("Token Bucket Config: " + tokenBucket.getConfigInfo());
        System.out.println("Leaky Bucket Config: " + leakyBucket.getConfigInfo());
        System.out.println("Sliding Window Config: " + slidingWindow.getConfigInfo());
        
        // Simulate a burst of requests to see how each handles bursts
        System.out.println("\nSimulating burst of 20 requests:");
        
        int tokenBucketSuccess = 0;
        for (int i = 0; i < 20; i++) {
            if (tokenBucket.tryAcquire()) tokenBucketSuccess++;
        }
        System.out.println("Token Bucket accepted: " + tokenBucketSuccess + "/20 requests");
        
        int leakyBucketSuccess = 0;
        for (int i = 0; i < 20; i++) {
            if (leakyBucket.tryAcquire()) leakyBucketSuccess++;
        }
        System.out.println("Leaky Bucket accepted: " + leakyBucketSuccess + "/20 requests");
        
        int slidingWindowSuccess = 0;
        for (int i = 0; i < 20; i++) {
            if (slidingWindow.tryAcquire()) slidingWindowSuccess++;
        }
        System.out.println("Sliding Window accepted: " + slidingWindowSuccess + "/20 requests");
    }
}