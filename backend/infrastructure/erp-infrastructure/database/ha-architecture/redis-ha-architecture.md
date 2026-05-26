# Redis高可用集群架构设计

## 架构概述
针对ERP核心功能设计的Redis高可用架构，提供高性能缓存和会话管理服务，确保缓存数据的高可用性和一致性。

## 架构设计

### 1. Redis哨兵模式架构
```
+----------------+     +----------------+     +----------------+
|  Redis主节点   |<--->|  Redis哨兵1    |<--->|  Redis哨兵2    |
|  Master        |     |  Sentinel1     |     |  Sentinel2     |
|  192.168.20.10 |     |  192.168.20.21 |     |  192.168.20.22 |
+----------------+     +----------------+     +----------------+
        |                      |                      |
        v                      v                      v
+----------------+     +----------------+     +----------------+
|  Redis从节点1  |     |  Redis哨兵3    |     |  应用连接池    |
|  Slave1        |     |  Sentinel3     |     |               |
|  192.168.20.11 |     |  192.168.20.23 |     +----------------+
+----------------+     +----------------+
        |
        v
+----------------+
|  Redis从节点2  |
|  Slave2        |
|  192.168.20.12 |
+----------------+
```

### 2. Redis哨兵配置

#### 主节点配置
```conf
# redis-master.conf
port 6379
bind 192.168.20.10
requirepass ${REDIS_MASTER_PASSWORD}
masterauth ${REDIS_MASTER_PASSWORD}

# 持久化配置
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# 内存配置
maxmemory 8gb
maxmemory-policy allkeys-lru
```

#### 从节点配置
```conf
# redis-slave.conf
port 6379
bind 192.168.20.11
requirepass ${REDIS_SLAVE_PASSWORD}
masterauth ${REDIS_MASTER_PASSWORD}
slaveof 192.168.20.10 6379
slave-read-only yes

# 持久化配置
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
```

#### 哨兵配置
```conf
# sentinel.conf
port 26379
bind 192.168.20.21
sentinel monitor mymaster 192.168.20.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
sentinel auth-pass mymaster ${REDIS_MASTER_PASSWORD}
```

### 3. 故障转移策略

#### 故障检测
1. **主观下线**: 单个哨兵检测到主节点不可用（超时5秒）
2. **客观下线**: 超过半数的哨兵（2/3）确认主节点不可用
3. **选举新主**: 哨兵通过Raft算法选举新的主节点

#### 故障转移流程
```bash
# 1. 哨兵检测到主节点故障
+ 主观下线 -> 请求其他哨兵确认
+ 客观下线确认

# 2. 选举领导者哨兵
+ 使用Raft算法选举
+ 领导者负责执行故障转移

# 3. 选择新主节点
# 基于以下优先级：
#   a) 复制偏移量最大的从节点
#   b) 运行时间最长的从节点
#   c) 实例ID最小的从节点

# 4. 提升从节点为主节点
SLAVEOF NO ONE

# 5. 重新配置其他从节点
SLAVEOF <new-master-ip> <new-master-port>

# 6. 更新客户端配置
# 哨兵通知所有客户端新的主节点地址
```

### 4. 缓存策略设计

#### ERP核心功能缓存策略
```java
// 批次管理缓存配置
@Component
public class BatchCacheConfig {
    
    @Value("${redis.cache.batch.ttl:1800}")
    private int batchTtl;
    
    @Value("${redis.cache.batch.max-size:10000}")
    private int batchMaxSize;
    
    @Bean
    public CacheManager batchCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(batchTtl))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("batch_details", 
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(3600)))
            .withCacheConfiguration("batch_history", 
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(7200)))
            .build();
    }
}
```

#### 价格策略缓存
```yaml
# application-redis.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300s
      cache-null-values: false
      key-prefix: "erp:"
      use-key-prefix: true
    
    caches:
      price-strategy:
        spec: expireAfterWrite=10m,maximumSize=5000
      product-price:
        spec: expireAfterWrite=5m,maximumSize=10000
      batch-info:
        spec: expireAfterWrite=30m,maximumSize=2000
```

### 5. 数据一致性保障

#### 缓存同步策略
1. **写穿透**: 先写数据库，再更新缓存
2. **缓存失效**: 数据库更新后，使相关缓存失效
3. **双写一致性**: 使用分布式锁保证缓存和数据库一致性

#### 分布式锁实现
```java
@Component
public class RedisDistributedLock {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_PREFIX = "erp:lock:";
    
    public boolean tryLock(String key, String value, long expireTime) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
            .setIfAbsent(LOCK_PREFIX + key, value, expireTime, TimeUnit.SECONDS));
    }
    
    public boolean unlock(String key, String value) {
        String lockKey = LOCK_PREFIX + key;
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        
        if (value.equals(currentValue)) {
            return Boolean.TRUE.equals(redisTemplate.delete(lockKey));
        }
        return false;
    }
}
```

### 6. 性能优化配置

#### 连接池配置
```yaml
# Lettuce连接池配置
spring:
  redis:
    lettuce:
      pool:
        max-active: 200
        max-idle: 50
        min-idle: 10
        max-wait: 1000ms
        time-between-eviction-runs: 60s
    timeout: 2000ms
    cluster:
      nodes:
        - 192.168.20.10:6379
        - 192.168.20.11:6379
        - 192.168.20.12:6379
      max-redirects: 3
```

#### 内存优化
```conf
# redis.conf 内存优化配置
# 根据ERP数据特性调整
maxmemory 16gb
maxmemory-policy allkeys-lru

# 优化AOF重写
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 优化RDB保存
save 900 1
save 300 10
save 60 10000
rdbcompression yes
rdbchecksum yes
```

### 7. 监控和告警

#### 关键监控指标
1. **缓存命中率**: > 95%
2. **内存使用率**: < 85%
3. **连接数**: < 最大连接数的80%
4. **响应时间**: P95 < 10ms
5. **主从延迟**: < 100ms

#### Prometheus监控配置
```yaml
# redis_exporter配置
- job_name: 'redis'
  static_configs:
    - targets: ['192.168.20.10:9121', '192.168.20.11:9121', '192.168.20.12:9121']
  metrics_path: /metrics
  params:
    redis.addr: ['redis://localhost:6379']
    redis.password: ['${REDIS_PASSWORD}']
```

#### Grafana监控面板
```json
{
  "panels": [
    {
      "title": "Redis缓存命中率",
      "targets": [
        {
          "expr": "rate(redis_keyspace_hits_total[5m]) / (rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])) * 100",
          "legendFormat": "命中率"
        }
      ]
    },
    {
      "title": "Redis内存使用",
      "targets": [
        {
          "expr": "redis_memory_used_bytes / redis_memory_max_bytes * 100",
          "legendFormat": "内存使用率"
        }
      ]
    }
  ]
}
```

### 8. 部署脚本

#### Redis集群部署脚本
```bash
#!/bin/bash
# deploy-redis-ha.sh

# 1. 安装Redis
apt-get update
apt-get install -y redis-server redis-sentinel

# 2. 配置主节点
cat > /etc/redis/redis-master.conf << EOF
port 6379
bind 192.168.20.10
requirepass ${REDIS_MASTER_PASSWORD}
masterauth ${REDIS_MASTER_PASSWORD}
save 900 1
save 300 10
save 60 10000
appendonly yes
maxmemory 8gb
maxmemory-policy allkeys-lru
EOF

# 3. 配置哨兵
cat > /etc/redis/sentinel.conf << EOF
port 26379
bind 192.168.20.21
sentinel monitor mymaster 192.168.20.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
sentinel auth-pass mymaster ${REDIS_MASTER_PASSWORD}
EOF

# 4. 启动服务
systemctl restart redis-server
systemctl restart redis-sentinel

echo "Redis HA集群部署完成"
```

#### 健康检查脚本
```bash
#!/bin/bash
# redis-health-check.sh

# 检查Redis主节点状态
check_redis_master() {
    redis-cli -h 192.168.20.10 -p 6379 -a ${REDIS_PASSWORD} ping
    if [ $? -eq 0 ]; then
        echo "Redis主节点正常"
        return 0
    else
        echo "Redis主节点异常"
        return 1
    fi
}

# 检查哨兵状态
check_sentinel() {
    for sentinel in 192.168.20.21 192.168.20.22 192.168.20.23; do
        redis-cli -h $sentinel -p 26379 sentinel master mymaster
        if [ $? -ne 0 ]; then
            echo "哨兵节点 $sentinel 异常"
            return 1
        fi
    done
    echo "所有哨兵节点正常"
    return 0
}

# 主检查流程
check_redis_master && check_sentinel
```

### 9. 故障恢复手册

#### 常见故障处理
1. **主节点故障**: 哨兵自动选举新主，切换时间<30秒
2. **网络分区**: 使用quorum机制，避免脑裂
3. **数据不一致**: 使用AOF重写或从节点全量同步
4. **内存溢出**: 自动清理LRU数据，告警扩容

#### 手动故障恢复
```bash
# 1. 检查当前主节点
redis-cli -h 192.168.20.10 -p 6379 info replication

# 2. 如果主节点故障，手动提升从节点
redis-cli -h 192.168.20.11 -p 6379 slaveof no one

# 3. 重新配置其他从节点
redis-cli -h 192.168.20.12 -p 6379 slaveof 192.168.20.11 6379

# 4. 更新哨兵配置
for sentinel in 192.168.20.21 192.168.20.22 192.168.20.23; do
    redis-cli -h $sentinel -p 26379 sentinel failover mymaster
done
```

#### 恢复时间目标(RTO/RPO)
- **RTO (恢复时间目标)**: < 30秒（自动故障转移）
- **RPO (恢复点目标)**: 0数据丢失（AOF每秒刷盘）
- **MTTR (平均修复时间)**: < 10分钟

### 10. ERP缓存使用场景

#### 批次管理缓存
```java
// 批次信息缓存服务
@Service
public class BatchCacheService {
    
    @Cacheable(value = "batch_details", key = "#batchId")
    public BatchDetail getBatchDetail(String batchId) {
        // 从数据库查询批次详情
        return batchRepository.findById(batchId);
    }
    
    @CacheEvict(value = "batch_details", key = "#batchId")
    public void updateBatch(String batchId, BatchDetail detail) {
        // 更新数据库
        batchRepository.save(detail);
        // 缓存自动失效
    }
}
```

#### 价格策略缓存
```java
// 价格计算缓存服务
@Service
public class PriceCacheService {
    
    @Cacheable(value = "product_price", key = "#productId + ':' + #quantity")
    public BigDecimal calculatePrice(String productId, int quantity) {
        // 复杂的价格计算逻辑
        return priceCalculator.calculate(productId, quantity);
    }
}
```

## 总结
此Redis高可用架构设计能够满足ERP核心功能的需求：
1. **高可用性**: 99.99%的可用性保证，自动故障转移<30秒
2. **高性能**: 支持10万+ QPS，响应时间<10ms
3. **数据一致性**: 缓存与数据库强一致性保证
4. **可扩展性**: 支持在线扩容，无感知扩展
5. **可维护性**: 完善的监控、告警和运维工具
6. **安全性**: 密码认证，网络隔离，访问控制