package cn.aiedge.erp.price.engine.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存性能基准测试
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class CacheBenchmark {
    
    private JedisPool jedisPool;
    private Map<String, String> testData;
    
    @Setup(Level.Trial)
    public void setup() {
        // 初始化Redis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        
        // 准备测试数据
        testData = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            testData.put("strategy:" + i, 
                "{\"id\":\"STRATEGY-" + i + 
                "\",\"name\":\"策略-" + i + 
                "\",\"discount\":" + (i % 50) + "}");
        }
        
        // 预热缓存
        try (Jedis jedis = jedisPool.getResource()) {
            for (Map.Entry<String, String> entry : testData.entrySet()) {
                jedis.setex(entry.getKey(), 300, entry.getValue()); // 5分钟过期
            }
        }
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
    
    @Benchmark
    public void benchmarkCacheGet(Blackhole blackhole) {
        // 缓存读取测试
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "strategy:" + (System.currentTimeMillis() % 1000);
            String value = jedis.get(key);
            blackhole.consume(value);
        }
    }
    
    @Benchmark
    public void benchmarkCacheSet(Blackhole blackhole) {
        // 缓存写入测试
        try (Jedis jedis = jedisPool.getResource()) {
            long timestamp = System.currentTimeMillis();
            String key = "temp:strategy:" + timestamp;
            String value = "{\"id\":\"TEMP-" + timestamp + "\",\"name\":\"临时策略\"}";
            
            String result = jedis.setex(key, 60, value);
            blackhole.consume(result);
        }
    }
    
    @Benchmark
    public void benchmarkBatchGet(Blackhole blackhole) {
        // 批量读取测试
        try (Jedis jedis = jedisPool.getResource()) {
            String[] keys = new String[10];
            for (int i = 0; i < 10; i++) {
                keys[i] = "strategy:" + ((System.currentTimeMillis() + i) % 1000);
            }
            
            for (String key : keys) {
                String value = jedis.get(key);
                blackhole.consume(value);
            }
        }
    }
    
    @Benchmark
    @Threads(4)
    public void benchmarkConcurrentGet(Blackhole blackhole) {
        // 并发读取测试
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "strategy:" + (Thread.currentThread().getId() % 1000);
            String value = jedis.get(key);
            blackhole.consume(value);
        }
    }
    
    @Benchmark
    public void benchmarkPipelineGet(Blackhole blackhole) {
        // 管道批量读取测试
        try (Jedis jedis = jedisPool.getResource()) {
            var pipeline = jedis.pipelined();
            
            for (int i = 0; i < 100; i++) {
                pipeline.get("strategy:" + i);
            }
            
            var results = pipeline.syncAndReturnAll();
            blackhole.consume(results);
        }
    }
    
    @Benchmark
    public void benchmarkCacheMiss(Blackhole blackhole) {
        // 缓存未命中测试
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "nonexistent:" + System.currentTimeMillis();
            String value = jedis.get(key);
            blackhole.consume(value);
        }
    }
    
    @Benchmark
    public void benchmarkCacheWithExpiry(Blackhole blackhole) {
        // 带过期时间的缓存测试
        try (Jedis jedis = jedisPool.getResource()) {
            long timestamp = System.currentTimeMillis();
            String key = "expiring:strategy:" + timestamp;
            String value = "{\"id\":\"EXP-" + timestamp + "\"}";
            
            // 设置1秒过期
            String result = jedis.setex(key, 1, value);
            
            // 立即读取
            String readValue = jedis.get(key);
            
            blackhole.consume(result);
            blackhole.consume(readValue);
        }
    }
}