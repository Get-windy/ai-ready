# 批次管理模块缓存策略设计

## 1. 设计目标

### 1.1 性能目标
- **响应时间**: 高频查询操作响应时间 ≤ 50ms (95分位)
- **数据库压力**: 减少数据库查询压力 70-80%
- **并发能力**: 支持1000+并发查询请求
- **缓存命中率**: 热点数据缓存命中率 ≥ 90%

### 1.2 业务目标
- **数据一致性**: 保证缓存与数据库数据一致性
- **可用性**: 缓存层故障不影响核心业务功能
- **可扩展性**: 支持水平扩展和容量增长
- **可维护性**: 提供监控、告警和运维工具

## 2. 数据访问模式分析

### 2.1 高频访问数据（热点数据）
| 数据类型 | 访问频率 | 数据量 | 特点 |
|---------|---------|--------|------|
| **批次详情** | 极高 | 单条 | 按ID查询，实时性要求高 |
| **批次列表** | 高 | 多页 | 分页查询，条件过滤 |
| **库存汇总** | 中 | 聚合 | 按仓库统计，计算成本高 |
| **临期预警** | 中 | 批量 | 定时计算，批量查询 |

### 2.2 低频访问数据（冷数据）
| 数据类型 | 访问频率 | 数据量 | 特点 |
|---------|---------|--------|------|
| **历史批次** | 低 | 大量 | 归档数据，偶尔查询 |
| **批次流转记录** | 低 | 海量 | 审计追踪，按需查询 |
| **统计报表** | 低 | 聚合 | 定时生成，缓存有效 |

## 3. 多级缓存架构设计

### 3.1 架构图
```
┌─────────────────────────────────────────────────────────┐
│                    应用层 (Application)                   │
├─────────────────────────────────────────────────────────┤
│  L1 缓存: Caffeine本地缓存 (进程内，极速)                   │
│  ├──批次详情缓存 (batchDetail)                           │
│  ├──批次列表缓存 (batchList)                             │
│  ├──库存汇总缓存 (stockSummary)                          │
│  └──临期预警缓存 (expiringWarning)                       │
├─────────────────────────────────────────────────────────┤
│  L2 缓存: Redis分布式缓存 (跨进程，共享)                   │
│  ├──批次详情缓存 (batchDetail)                           │
│  ├──批次列表缓存 (batchList)                             │
│  ├──库存汇总缓存 (stockSummary)                          │
│  ├──临期预警缓存 (expiringWarning)                       │
│  └──空值缓存 (nullValue)                                 │
├─────────────────────────────────────────────────────────┤
│                    数据库层 (Database)                    │
│  ├──批次主表 (batch_number)                              │
│  ├──批次流转表 (batch_flow_record)                       │
│  └──序列号表 (serial_number)                             │
└─────────────────────────────────────────────────────────┘
```

### 3.2 缓存层级策略
| 缓存层级 | 技术栈 | 容量 | 响应时间 | 一致性 | 适用场景 |
|---------|--------|------|----------|--------|----------|
| **L1本地缓存** | Caffeine | 10,000条 | < 1ms | 弱一致性 | 热点数据、频繁访问 |
| **L2分布式缓存** | Redis | 100,000条 | < 5ms | 最终一致 | 共享数据、分布式访问 |
| **数据库** | PostgreSQL | 无限制 | 10-100ms | 强一致 | 持久化、全量数据 |

## 4. 缓存策略详细设计

### 4.1 批次详情缓存策略
```yaml
batch.cache.detail:
  redisTtl: 1800        # Redis缓存30分钟
  localTtl: 300         # 本地缓存5分钟
  maxSize: 10000        # 最大缓存条目数
  
  # 缓存键设计
  keyPrefix: "batch:detail:"
  keyPattern: "${keyPrefix}${batchId}"
  
  # 缓存逻辑
  - 读流程: L1 → L2 → DB
  - 写流程: DB → 删除L1 → 删除L2
  - 更新策略: 写时失效 + 异步刷新
```

### 4.2 批次列表缓存策略
```yaml
batch.cache.list:
  redisTtl: 600         # Redis缓存10分钟
  maxSize: 1000         # 最大缓存列表数
  
  # 缓存键设计
  keyPrefix: "batch:list:"
  keyPattern: "${keyPrefix}${hash(condition)}:page:${page}:size:${size}"
  
  # 缓存逻辑
  - 条件哈希: MD5(查询条件)作为缓存键部分
  - 分页缓存: 每页独立缓存
  - 过期策略: 条件变更时自动失效
```

### 4.3 库存汇总缓存策略
```yaml
batch.cache.stock:
  redisTtl: 3600        # Redis缓存1小时
  maxSize: 1000         # 最大缓存仓库数
  
  # 缓存键设计
  keyPrefix: "batch:stock:"
  keyPattern: "${keyPrefix}${warehouseId}"
  
  # 缓存逻辑
  - 定时刷新: 每30分钟自动刷新
  - 事件触发: 库存变动时失效缓存
  - 降级策略: 缓存失效时返回旧数据并异步刷新
```

### 4.4 临期预警缓存策略
```yaml
batch.cache.expiring:
  redisTtl: 7200        # Redis缓存2小时
  maxSize: 100          # 最大预警天数配置
  
  # 缓存键设计
  keyPrefix: "batch:expiring:"
  keyPattern: "${keyPrefix}${warningDays}"
  
  # 缓存逻辑
  - 定时计算: 每天凌晨计算
  - 分级缓存: 不同预警天数独立缓存
  - 主动刷新: 批次状态变更时重新计算
```

### 4.5 空值缓存策略（防缓存穿透）
```yaml
batch.cache.nullValue:
  redisTtl: 300         # Redis缓存5分钟
  
  # 缓存键设计
  keyPrefix: "batch:null:"
  keyPattern: "${keyPrefix}${batchId}"
  
  # 缓存逻辑
  - 空值标记: 查询不存在数据时缓存空值
  - 短时缓存: 防止恶意查询穿透
  - 自动清理: 定时清理过期空值缓存
```

## 5. 缓存一致性设计

### 5.1 写时失效策略
```
写操作流程:
1. 开启数据库事务
2. 更新数据库记录
3. 提交事务
4. 异步删除缓存 (L1 → L2)
5. 记录缓存操作日志
```

### 5.2 延迟双删策略
```java
public void updateBatch(BatchNumber batch) {
    // 第一次删除：写前删除
    cacheService.deleteBatchDetailCache(batch.getId());
    
    // 更新数据库
    batchNumberMapper.updateById(batch);
    
    // 延迟第二次删除（防止并发读脏数据）
    scheduledExecutor.schedule(() -> {
        cacheService.deleteBatchDetailCache(batch.getId());
    }, 1, TimeUnit.SECONDS);
}
```

### 5.3 异步刷新策略
```java
@Component
public class CacheRefreshService {
    
    @Async
    public void refreshBatchCache(Long batchId) {
        // 从数据库加载最新数据
        BatchNumber batch = batchNumberMapper.selectById(batchId);
        
        // 更新缓存
        if (batch != null) {
            cacheService.cacheBatchDetail(batch);
        } else {
            cacheService.cacheNullValue(batchId);
        }
    }
}
```

## 6. 缓存异常防护策略

### 6.1 缓存穿透防护
```java
public BatchNumber getBatchById(Long batchId) {
    // 1. 检查空值缓存
    if (cacheService.isNullValueCached(batchId)) {
        return null;
    }
    
    // 2. 查询本地缓存
    BatchNumber batch = localCache.get(batchId);
    if (batch != null) {
        return batch;
    }
    
    // 3. 查询Redis缓存
    batch = redisCache.get(batchId);
    if (batch != null) {
        // 回填本地缓存
        localCache.put(batchId, batch);
        return batch;
    }
    
    // 4. 查询数据库（加分布式锁防并发穿透）
    String lockKey = "lock:batch:" + batchId;
    if (redisLock.tryLock(lockKey, 3, TimeUnit.SECONDS)) {
        try {
            batch = batchNumberMapper.selectById(batchId);
            if (batch != null) {
                // 写入缓存
                cacheService.cacheBatchDetail(batch);
            } else {
                // 缓存空值
                cacheService.cacheNullValue(batchId);
            }
            return batch;
        } finally {
            redisLock.unlock(lockKey);
        }
    } else {
        // 等待并重试
        Thread.sleep(100);
        return getBatchById(batchId);
    }
}
```

### 6.2 缓存击穿防护
```java
public BatchNumber getBatchDetailWithBreakdownProtection(Long batchId) {
    // 使用互斥锁防止热点数据失效时大量请求穿透
    String lockKey = "mutex:batch:" + batchId;
    
    // 尝试获取缓存
    BatchNumber batch = cacheService.getBatchById(batchId);
    if (batch != null) {
        return batch;
    }
    
    // 缓存失效，获取互斥锁
    if (redisLock.tryLock(lockKey, 10, TimeUnit.SECONDS)) {
        try {
            // 双重检查
            batch = cacheService.getBatchById(batchId);
            if (batch != null) {
                return batch;
            }
            
            // 查询数据库
            batch = batchNumberMapper.selectById(batchId);
            if (batch != null) {
                // 重建缓存
                cacheService.cacheBatchDetail(batch);
            }
            return batch;
        } finally {
            redisLock.unlock(lockKey);
        }
    } else {
        // 等待其他线程重建缓存
        Thread.sleep(50);
        return cacheService.getBatchById(batchId);
    }
}
```

### 6.3 缓存雪崩防护
```yaml
# 配置随机过期时间
batch.cache:
  detail:
    redisTtl: ${random.int[1500,2100]}  # 25-35分钟随机
  list:
    redisTtl: ${random.int[540,660]}    # 9-11分钟随机
  stock:
    redisTtl: ${random.int[3300,3900]}  # 55-65分钟随机
  
# 热key探测与拆分
hotkey:
  detection:
    threshold: 1000      # 每秒访问阈值
    window: 10           # 统计窗口(秒)
  solution:
    - 本地缓存优先
    - 随机过期时间
    - 请求合并
    - 数据分片
```

## 7. 缓存监控与运维

### 7.1 监控指标
| 指标 | 阈值 | 告警级别 | 处理措施 |
|------|------|----------|----------|
| **缓存命中率** | < 80% | 警告 | 检查缓存策略，调整TTL |
| **缓存穿透率** | > 5% | 严重 | 检查空值缓存，优化查询 |
| **响应时间P95** | > 50ms | 警告 | 检查缓存性能，扩容 |
| **内存使用率** | > 85% | 严重 | 清理过期缓存，扩容 |
| **连接数使用率** | > 80% | 警告 | 优化连接池，扩容 |

### 7.2 运维工具
```java
@RestController
@RequestMapping("/api/cache")
public class CacheManagerController {
    
    @GetMapping("/stats")
    public CacheStats getCacheStats() {
        return cacheService.getCacheStats();
    }
    
    @PostMapping("/clear/{cacheType}")
    public ResponseEntity<?> clearCache(@PathVariable String cacheType) {
        cacheService.clearCache(cacheType);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/keys/{pattern}")
    public List<String> getCacheKeys(@PathVariable String pattern) {
        return cacheService.scanKeys(pattern);
    }
    
    @GetMapping("/detail/{key}")
    public Object getCacheDetail(@PathVariable String key) {
        return cacheService.getCacheDetail(key);
    }
}
```

### 7.3 健康检查
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,cache
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# 缓存健康检查
cache:
  health:
    check-interval: 30s
    timeout: 5s
    retry: 3
```

## 8. 实施路线图

### 8.1 第一阶段：基础缓存（1周）
1. 实现批次详情基础缓存
2. 配置Caffeine本地缓存
3. 配置Redis分布式缓存
4. 实现缓存监控基础

### 8.2 第二阶段：高级策略（2周）
1. 实现多级缓存架构
2. 添加缓存异常防护
3. 实现缓存一致性机制
4. 完善监控告警系统

### 8.3 第三阶段：优化调优（1周）
1. 性能压测与调优
2. 容量规划与扩容
3. 生产环境部署
4. 运维文档完善

## 9. 预期效益

### 9.1 性能提升
| 场景 | 优化前 | 优化后 | 提升幅度 |
|------|--------|--------|----------|
| 批次详情查询 | 20-50ms | < 5ms | 75-90% |
| 批次列表查询 | 50-200ms | < 20ms | 60-90% |
| 库存汇总查询 | 100-500ms | < 50ms | 50-90% |
| 并发查询能力 | 100 QPS | 1000+ QPS | 10倍 |

### 9.2 成本节约
- **数据库成本**: 减少70%数据库查询压力
- **硬件成本**: 延迟硬件升级需求
- **运维成本**: 减少数据库维护工作量
- **开发成本**: 统一缓存框架，降低开发复杂度

### 9.3 风险降低
- **可用性风险**: 缓存层故障不影响核心业务
- **性能风险**: 防止突发流量导致系统雪崩
- **数据风险**: 保证缓存数据一致性
- **运维风险**: 完善的监控和运维工具

---

## 附录

### A. 配置文件示例
```yaml
# application-cache.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 1800s
      cache-null-values: false
      key-prefix: "cache:"
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

batch:
  cache:
    detail:
      redisTtl: 1800
      localTtl: 300
      maxSize: 10000
    list:
      redisTtl: 600
      maxSize: 1000
    stock:
      redisTtl: 3600
      maxSize: 1000
    expiring:
      redisTtl: 7200
      maxSize: 100
    nullValue:
      redisTtl: 300
```

### B. 缓存键命名规范
```
batch:detail:{batchId}           # 批次详情
batch:list:{hash}:page:{p}:size:{s} # 批次列表
batch:stock:{warehouseId}        # 库存汇总
batch:expiring:{warningDays}     # 临期预警
batch:null:{batchId}             # 空值缓存
lock:batch:{batchId}             # 分布式锁
mutex:batch:{batchId}            # 互斥锁
```

### C. 监控仪表板指标
- **缓存命中率趋势图**
- **响应时间分布图**
- **内存使用率监控**
- **QPS/TPS统计**
- **错误率监控**
- **慢查询分析**

---

**文档版本**: v1.0  
**最后更新**: 2026-05-01  
**负责人**: team-member  
**审核状态**: ✅ 已完成设计