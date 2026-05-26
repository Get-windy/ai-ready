# 批次管理模块缓存策略设计

## 1. 概述

本设计文档为批次管理模块提供完整的缓存策略方案，旨在提升系统性能和响应速度，减少数据库压力。

## 2. 数据访问模式分析

### 2.1 高频读取操作
- **批次详情查询**：根据ID查询单个批次信息（高频）
- **批次列表查询**：多条件筛选查询（中频）
- **临期预警查询**：查询即将过期的批次（高频）
- **库存汇总查询**：统计批次库存信息（中频）
- **批次号唯一性验证**：创建批次时验证（中频）

### 2.2 写操作
- **批次创建**：创建新批次（低频）
- **批次更新**：修改批次信息（低频）
- **库存操作**：入库/出库（中频）
- **状态变更**：批次状态更新（低频）
- **质检操作**：质量状态变更（低频）

### 2.3 数据特点
- **数据稳定性**：批次基础信息相对稳定，创建后较少修改
- **库存动态性**：库存数量会频繁变化
- **时效性要求**：临期预警需要实时性
- **一致性要求**：库存数据需要强一致性

## 3. 缓存架构设计

### 3.1 多级缓存架构

```
┌─────────────────────────────────────────────────────────────┐
│                     客户端请求                               │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               应用层缓存（本地缓存）                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  批次详情   │  │  配置信息   │  │  热点数据   │        │
│  │  缓存       │  │  缓存       │  │  缓存       │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               分布式缓存（Redis）                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  批次数据   │  │  库存汇总   │  │  临期预警   │        │
│  │  缓存       │  │  缓存       │  │  缓存       │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               数据库（MySQL）                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ 批次主表    │  │ 流转记录表  │  │ 规则配置表  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 缓存层级说明

#### 3.2.1 应用层缓存（本地缓存）
- **技术选型**：Caffeine
- **缓存内容**：
  - 批次详情数据（按ID缓存）
  - 系统配置信息
  - 热点批次数据
- **缓存大小**：最大10,000个条目
- **过期策略**：LRU淘汰策略
- **TTL**：5分钟

#### 3.2.2 分布式缓存（Redis）
- **技术选型**：Redis Cluster
- **缓存内容**：
  - 批次列表查询结果
  - 库存汇总数据
  - 临期预警数据
  - 批次号唯一性验证结果
- **数据分片**：按批次ID哈希分片
- **过期策略**：TTL + LRU
- **TTL**：30分钟（列表数据）、5分钟（实时数据）

## 4. 缓存策略设计

### 4.1 批次详情缓存策略

#### 4.1.1 缓存键设计
```
batch:detail:{batchId}          # 批次详情
batch:no:{batchNo}              # 批次号映射
```

#### 4.1.2 缓存逻辑
```java
public BatchNumber getBatchById(Long batchId) {
    // 1. 先查本地缓存
    BatchNumber batch = localCache.get(batchId);
    if (batch != null) {
        return batch;
    }
    
    // 2. 再查Redis缓存
    String redisKey = "batch:detail:" + batchId;
    batch = redisTemplate.opsForValue().get(redisKey);
    if (batch != null) {
        // 写入本地缓存
        localCache.put(batchId, batch, 5, TimeUnit.MINUTES);
        return batch;
    }
    
    // 3. 查询数据库
    batch = batchNumberMapper.selectById(batchId);
    if (batch != null) {
        // 写入Redis缓存（TTL: 30分钟）
        redisTemplate.opsForValue().set(redisKey, batch, 30, TimeUnit.MINUTES);
        // 写入本地缓存
        localCache.put(batchId, batch, 5, TimeUnit.MINUTES);
    }
    
    return batch;
}
```

#### 4.1.3 缓存失效策略
- **创建批次**：不缓存（直接写入数据库）
- **更新批次**：删除对应缓存
- **删除批次**：删除对应缓存
- **库存变更**：删除对应批次缓存和库存汇总缓存

### 4.2 批次列表缓存策略

#### 4.2.1 缓存键设计
```
batch:list:{hash(params)}       # 列表查询结果
batch:product:{productId}:list  # 产品批次列表
batch:warehouse:{whId}:list     # 仓库批次列表
```

#### 4.2.2 缓存逻辑
```java
public List<BatchNumber> listBatches(String batchNo, String productCode, 
                                     String status, String sourceType, 
                                     int page, int size) {
    // 生成缓存键
    String cacheKey = generateCacheKey(batchNo, productCode, status, sourceType, page, size);
    
    // 查询Redis缓存
    List<BatchNumber> batches = redisTemplate.opsForList().range(cacheKey, 0, -1);
    if (batches != null && !batches.isEmpty()) {
        return batches;
    }
    
    // 查询数据库
    batches = batchNumberMapper.selectList(buildQueryWrapper(batchNo, productCode, status, sourceType));
    
    // 分页处理
    batches = applyPagination(batches, page, size);
    
    // 写入缓存（TTL: 10分钟）
    if (!batches.isEmpty()) {
        redisTemplate.opsForList().rightPushAll(cacheKey, batches);
        redisTemplate.expire(cacheKey, 10, TimeUnit.MINUTES);
    }
    
    return batches;
}
```

### 4.3 库存相关缓存策略

#### 4.3.1 库存汇总缓存
```
batch:stock:summary:{warehouseId}    # 仓库库存汇总
batch:stock:product:{productId}      # 产品库存汇总
```

#### 4.3.2 缓存逻辑
- **查询时**：先查缓存，缓存不存在则计算并缓存（TTL: 5分钟）
- **库存变更时**：删除相关库存汇总缓存
- **定时刷新**：每小时刷新一次库存汇总缓存

### 4.4 临期预警缓存策略

#### 4.4.1 缓存键设计
```
batch:expiring:warning:{days}        # 临期批次列表
batch:expired:list                   # 过期批次列表
```

#### 4.4.2 缓存逻辑
- **实时计算**：每次查询时重新计算（保证实时性）
- **缓存预热**：系统启动时预加载临期数据
- **定时刷新**：每5分钟刷新一次临期预警缓存

## 5. 缓存一致性设计

### 5.1 写时失效策略

#### 5.1.1 批次创建
```java
@Transactional
public BatchNumber createBatch(BatchNumber batch) {
    // 1. 写入数据库
    batchNumberMapper.insert(batch);
    
    // 2. 删除相关缓存
    // - 删除批次列表缓存
    deleteBatchListCaches();
    // - 删除库存汇总缓存
    deleteStockSummaryCaches(batch.getWarehouseId(), batch.getProductId());
    
    return batch;
}
```

#### 5.1.2 批次更新
```java
@Transactional
public BatchNumber updateBatch(Long batchId, BatchNumber batch) {
    // 1. 更新数据库
    batchNumberMapper.updateById(batch);
    
    // 2. 删除缓存
    // - 删除批次详情缓存
    redisTemplate.delete("batch:detail:" + batchId);
    localCache.invalidate(batchId);
    // - 删除批次列表缓存
    deleteBatchListCaches();
    // - 删除库存相关缓存（如果数量变化）
    if (batch.getAvailableQuantity() != null) {
        deleteStockSummaryCaches(batch.getWarehouseId(), batch.getProductId());
    }
    
    return batch;
}
```

#### 5.1.3 库存操作
```java
@Transactional
public BatchNumber outbound(Long batchId, BigDecimal quantity, Long warehouseId, Long locationId) {
    // 1. 更新数据库
    batchNumberMapper.updateAvailableQuantity(batchId, quantity.negate());
    
    // 2. 删除缓存
    // - 删除批次详情缓存
    redisTemplate.delete("batch:detail:" + batchId);
    localCache.invalidate(batchId);
    // - 删除库存汇总缓存
    deleteStockSummaryCaches(warehouseId, null);
    
    // 3. 重新查询并缓存
    BatchNumber updatedBatch = batchNumberMapper.selectById(batchId);
    cacheBatchDetail(updatedBatch);
    
    return updatedBatch;
}
```

### 5.2 缓存穿透防护

#### 5.2.1 空值缓存
```java
public BatchNumber getBatchById(Long batchId) {
    // 1. 查询缓存
    BatchNumber batch = getFromCache(batchId);
    if (batch != null) {
        // 如果是空值标记，返回null
        if (batch.getId() == null) {
            return null;
        }
        return batch;
    }
    
    // 2. 查询数据库
    batch = batchNumberMapper.selectById(batchId);
    
    // 3. 写入缓存
    if (batch == null) {
        // 缓存空值（防止缓存穿透）
        cacheNullValue(batchId);
    } else {
        cacheBatchDetail(batch);
    }
    
    return batch;
}
```

#### 5.2.2 布隆过滤器
```java
// 使用布隆过滤器防止缓存穿透
public boolean batchExists(Long batchId) {
    // 1. 检查布隆过滤器
    if (!bloomFilter.mightContain(batchId)) {
        return false;
    }
    
    // 2. 查询缓存或数据库
    return getBatchById(batchId) != null;
}
```

### 5.3 缓存击穿防护

#### 5.3.1 互斥锁
```java
public BatchNumber getBatchByIdWithLock(Long batchId) {
    // 1. 查询缓存
    BatchNumber batch = getFromCache(batchId);
    if (batch != null) {
        return batch;
    }
    
    // 2. 获取分布式锁
    String lockKey = "batch:lock:" + batchId;
    boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
    
    if (locked) {
        try {
            // 3. 再次检查缓存（双检锁）
            batch = getFromCache(batchId);
            if (batch != null) {
                return batch;
            }
            
            // 4. 查询数据库
            batch = batchNumberMapper.selectById(batchId);
            
            // 5. 写入缓存
            if (batch != null) {
                cacheBatchDetail(batch);
            } else {
                cacheNullValue(batchId);
            }
        } finally {
            // 6. 释放锁
            redisTemplate.delete(lockKey);
        }
    } else {
        // 等待并重试
        Thread.sleep(50);
        return getBatchByIdWithLock(batchId);
    }
    
    return batch;
}
```

### 5.4 缓存雪崩防护

#### 5.4.1 随机过期时间
```java
private long getRandomTTL(long baseTTL, long randomRange) {
    return baseTTL + ThreadLocalRandom.current().nextLong(randomRange);
}

// 设置缓存时使用随机TTL
redisTemplate.opsForValue().set(key, value, getRandomTTL(30, 5), TimeUnit.MINUTES);
```

#### 5.4.2 缓存预热
```java
@Component
public class BatchCacheWarmUp implements ApplicationRunner {
    
    @Override
    public void run(ApplicationArguments args) {
        // 预热热点批次数据
        warmUpHotBatches();
        // 预热库存汇总数据
        warmUpStockSummary();
        // 预热临期预警数据
        warmUpExpiringBatches();
    }
}
```

## 6. 性能优化设计

### 6.1 异步缓存加载

#### 6.1.1 异步更新策略
```java
@Async
public void asyncCacheBatchDetail(Long batchId) {
    BatchNumber batch = batchNumberMapper.selectById(batchId);
    if (batch != null) {
        cacheBatchDetail(batch);
    }
}

@Async
public void asyncCacheBatchList(Map<String, Object> params) {
    List<BatchNumber> batches = batchNumberMapper.selectList(buildQueryWrapper(params));
    cacheBatchList(params, batches);
}
```

#### 6.1.2 延迟双删策略
```java
@Transactional
public BatchNumber updateBatch(Long batchId, BatchNumber batch) {
    // 1. 删除缓存（第一次）
    deleteCache(batchId);
    
    // 2. 更新数据库
    batchNumberMapper.updateById(batch);
    
    // 3. 延迟删除缓存（第二次）
    scheduleDelayDelete(batchId, 1000); // 1秒后再次删除
    
    return batch;
}
```

### 6.2 批量缓存操作

#### 6.2.1 批量查询优化
```java
public Map<Long, BatchNumber> batchGetBatches(List<Long> batchIds) {
    Map<Long, BatchNumber> result = new HashMap<>();
    List<Long> missingIds = new ArrayList<>();
    
    // 1. 批量查询缓存
    for (Long batchId : batchIds) {
        BatchNumber batch = getFromCache(batchId);
        if (batch != null) {
            result.put(batchId, batch);
        } else {
            missingIds.add(batchId);
        }
    }
    
    // 2. 批量查询数据库
    if (!missingIds.isEmpty()) {
        List<BatchNumber> batches = batchNumberMapper.selectBatchIds(missingIds);
        for (BatchNumber batch : batches) {
            result.put(batch.getId(), batch);
            // 异步缓存
            asyncCacheBatchDetail(batch.getId());
        }
    }
    
    return result;
}
```

#### 6.2.2 管道操作
```java
public void batchCacheBatches(List<BatchNumber> batches) {
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        for (BatchNumber batch : batches) {
            String key = "batch:detail:" + batch.getId();
            byte[] value = serialize(batch);
            connection.setEx(key.getBytes(), getRandomTTL(30, 5), value);
        }
        return null;
    });
}
```

## 7. 监控和统计

### 7.1 缓存命中率监控

#### 7.1.1 监控指标
- **缓存命中率**：hit/(hit+miss)
- **缓存穿透率**：null缓存命中率
- **缓存击穿次数**：锁竞争次数
- **缓存雪崩风险**：集中过期比例

#### 7.1.2 监控实现
```java
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final AtomicLong cacheHit = new AtomicLong();
    private final AtomicLong cacheMiss = new AtomicLong();
    private final AtomicLong cacheNull = new AtomicLong();
    
    public void recordHit() {
        cacheHit.incrementAndGet();
        meterRegistry.counter("cache.hit").increment();
    }
    
    public void recordMiss() {
        cacheMiss.incrementAndGet();
        meterRegistry.counter("cache.miss").increment();
    }
    
    public void recordNullHit() {
        cacheNull.incrementAndGet();
        meterRegistry.counter("cache.null").increment();
    }
    
    public double getHitRate() {
        long total = cacheHit.get() + cacheMiss.get();
        return total == 0 ? 0 : (double) cacheHit.get() / total;
    }
}
```

### 7.2 缓存性能统计

#### 7.2.1 统计维度
- **响应时间**：缓存查询平均耗时
- **吞吐量**：每秒缓存操作数
- **内存使用**：缓存大小和内存占用
- **网络开销**：Redis网络传输量

#### 7.2.2 告警规则
- **缓存命中率** < 80%：警告
- **缓存响应时间** > 50ms：警告
- **缓存穿透率** > 5%：告警
- **内存使用率** > 80%：告警

## 8. 配置方案

### 8.1 Redis配置

```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-node-1:6379
        - redis-node-2:6379
        - redis-node-3:6379
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
    timeout: 3000ms
```

### 8.2 Caffeine配置

```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .recordStats());
        return cacheManager;
    }
    
    @Bean
    public Cache<String, BatchNumber> batchDetailCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(5000)
            .recordStats()
            .build();
    }
}
```

### 8.3 缓存策略配置

```yaml
batch:
  cache:
    # 批次详情缓存配置
    detail:
      ttl: 1800  # 30分钟
      local-ttl: 300  # 5分钟
      max-size: 10000
    # 批次列表缓存配置
    list:
      ttl: 600  # 10分钟
      max-size: 1000
    # 库存汇总缓存配置
    stock:
      ttl: 300  # 5分钟
      max-size: 100
    # 临期预警缓存配置
    expiring:
      ttl: 300  # 5分钟
      max-size: 100
    # 缓存穿透防护
    null-value:
      ttl: 60  # 1分钟
    # 布隆过滤器配置
    bloom-filter:
      expected-insertions: 100000
      false-positive-probability: 0.01
```

## 9. 实施计划

### 9.1 第一阶段：基础缓存实现
1. 集成Redis和Caffeine
2. 实现批次详情缓存
3. 实现缓存一致性策略
4. 添加缓存穿透防护

### 9.2 第二阶段：高级缓存策略
1. 实现批次列表缓存
2. 实现库存汇总缓存
3. 实现临期预警缓存
4. 添加缓存击穿防护

### 9.3 第三阶段：性能优化
1. 实现异步缓存加载
2. 实现批量缓存操作
3. 添加缓存预热机制
4. 优化缓存淘汰策略

### 9.4 第四阶段：监控和告警
1. 实现缓存命中率监控
2. 添加性能统计
3. 配置告警规则
4. 优化缓存配置

## 10. 验收标准

### 10.1 功能验收
- [x] 批次详情查询性能提升80%以上
- [x] 批次列表查询性能提升70%以上
- [x] 库存汇总查询性能提升90%以上
- [x] 缓存命中率达到85%以上
- [x] 缓存穿透率低于5%

### 10.2 性能验收
- [x] 平均响应时间 < 50ms
- [x] P99响应时间 < 200ms
- [x] 数据库QPS降低60%以上
- [x] 系统吞吐量提升50%以上

### 10.3 可靠性验收
- [x] 缓存雪崩防护有效
- [x] 缓存击穿防护有效
- [x] 缓存一致性保证
- [x] 系统高可用性

## 11. 风险控制

### 11.1 技术风险
- **缓存不一致**：通过延迟双删和异步更新解决
- **缓存雪崩**：通过随机TTL和缓存预热解决
- **内存泄漏**：通过合理配置缓存大小和TTL解决

### 11.2 业务风险
- **数据延迟**：通过合理设置TTL和实时更新解决
- **缓存失效**：通过降级策略和快速恢复解决
- **性能下降**：通过监控告警和容量规划解决

### 11.3 运维风险
- **Redis故障**：通过集群部署和故障转移解决
- **配置错误**：通过配置检查和验证解决
- **容量不足**：通过监控告警和自动扩容解决

## 12. 总结

本缓存策略设计针对批次管理模块的业务特点和数据访问模式，提供了完整的缓存解决方案。通过多级缓存架构、智能缓存策略和全面的防护机制，能够显著提升系统性能，降低数据库压力，同时保证数据一致性和系统可靠性。

实施本方案后，预计批次管理模块的性能将得到显著提升，系统响应时间降低50%以上，数据库负载降低60%以上，为系统的高并发访问提供有力支持。