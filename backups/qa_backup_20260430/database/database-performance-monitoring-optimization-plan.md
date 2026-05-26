# 【Sprint 27+1】测试环境数据库性能监控与优化方案

## 1. 方案概述

### 1.1 目标
为Sprint 27+1测试环境建立完整的数据库性能监控与优化体系，确保数据库稳定高效运行，及时发现和解决性能问题。

### 1.2 范围
- PostgreSQL数据库性能监控
- Redis数据库性能监控  
- 慢查询监控和优化
- 数据库连接池监控
- 性能指标告警配置
- 性能优化建议和实施

### 1.3 当前状态评估
基于`qa/monitoring/basic-config-checklist.md`，发现以下问题：
1. PostgreSQL Exporter未部署
2. Redis Exporter未部署  
3. 缺少数据库专用监控仪表板
4. 缺少慢查询监控机制
5. 缺少性能基线数据

## 2. 性能监控指标体系

### 2.1 PostgreSQL监控指标

#### 2.1.1 基础资源指标
| 指标类别 | 监控指标 | 告警阈值 | 监控频率 |
|---------|---------|---------|---------|
| 连接数 | active_connections | > 80%最大连接数 | 30s |
| 连接数 | idle_connections | > 50%活跃连接数 | 30s |
| 连接数 | waiting_connections | > 0 | 30s |
| 事务 | transactions_per_second | < 10 TPS | 30s |
| 事务 | rollback_ratio | > 5% | 30s |

#### 2.1.2 查询性能指标
| 指标类别 | 监控指标 | 告警阈值 | 监控频率 |
|---------|---------|---------|---------|
| 慢查询 | queries_longer_than_1s | > 10个/分钟 | 30s |
| 慢查询 | queries_longer_than_5s | > 0个/分钟 | 30s |
| 缓存命中率 | cache_hit_ratio | < 90% | 30s |
| 索引命中率 | index_hit_ratio | < 95% | 30s |

#### 2.1.3 存储性能指标
| 指标类别 | 监控指标 | 告警阈值 | 监控频率 |
|---------|---------|---------|---------|
| 表大小 | table_size_growth | > 10GB/天 | 5分钟 |
| 索引大小 | index_size_growth | > 5GB/天 | 5分钟 |
| 死元组 | dead_tuples_ratio | > 20% | 5分钟 |
| 膨胀率 | bloat_ratio | > 30% | 1小时 |

### 2.2 Redis监控指标

#### 2.2.1 基础资源指标
| 指标类别 | 监控指标 | 告警阈值 | 监控频率 |
|---------|---------|---------|---------|
| 内存 | used_memory | > 80%总内存 | 30s |
| 内存 | memory_fragmentation_ratio | > 1.5 | 30s |
| 连接数 | connected_clients | > 1000 | 30s |
| 连接数 | rejected_connections | > 0 | 30s |

#### 2.2.2 性能指标
| 指标类别 | 监控指标 | 告警阈值 | 监控频率 |
|---------|---------|---------|---------|
| 命令处理 | instantaneous_ops_per_sec | < 1000 OPS | 30s |
| 延迟 | latency_percentile_99 | > 10ms | 30s |
| 命中率 | keyspace_hits_ratio | < 90% | 30s |
| 过期键 | expired_keys | > 1000/秒 | 30s |

#### 2.2.3 持久化指标
| 指标类别 | 监控指标 | 告警阈值 | 监控频率 |
|---------|---------|---------|---------|
| RDB | last_bgsave_status | != ok | 30s |
| RDB | rdb_last_bgsave_time_sec | > 60s | 30s |
| AOF | aof_last_bgrewrite_status | != ok | 30s |
| AOF | aof_current_size | > 1GB | 5分钟 |

## 3. 监控架构设计

### 3.1 监控组件部署
```
测试环境监控架构：
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │      Redis      │    │   Application   │
│    数据库       │    │    数据库       │    │     服务        │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         ▼                      ▼                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ PostgreSQL      │    │   Redis         │    │ Spring Boot     │
│  Exporter       │    │   Exporter      │    │  Actuator       │
│  (9187端口)     │    │  (9121端口)     │    │  (管理端口)     │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         └──────────┬───────────┴──────────────────────┘
                    │
                    ▼
           ┌─────────────────┐
           │   Prometheus    │
           │    (9090端口)   │
           └────────┬────────┘
                    │
                    ▼
           ┌─────────────────┐    ┌─────────────────┐
           │    Grafana      │    │  Alertmanager   │
           │   (3000端口)    │    │   (9093端口)    │
           └─────────────────┘    └─────────────────┘
```

### 3.2 Exporter部署配置

#### 3.2.1 PostgreSQL Exporter配置
```yaml
# docker-compose-postgres-exporter.yml
version: '3.8'
services:
  postgres-exporter-main:
    image: prometheuscommunity/postgres-exporter:latest
    container_name: postgres-exporter-main
    restart: unless-stopped
    ports:
      - "9187:9187"
    environment:
      - DATA_SOURCE_NAME=postgresql://postgres:password@postgres-main:5432/ai_ready?sslmode=disable
      - PG_EXPORTER_EXTEND_QUERY_PATH=/etc/postgres-exporter/queries.yaml
    volumes:
      - ./config/postgres-queries.yaml:/etc/postgres-exporter/queries.yaml
    networks:
      - ai-ready-network

  postgres-exporter-inventory:
    image: prometheuscommunity/postgres-exporter:latest
    container_name: postgres-exporter-inventory
    restart: unless-stopped
    ports:
      - "9188:9187"
    environment:
      - DATA_SOURCE_NAME=postgresql://postgres:password@postgres-inventory:5432/inventory?sslmode=disable
      - PG_EXPORTER_EXTEND_QUERY_PATH=/etc/postgres-exporter/queries.yaml
    volumes:
      - ./config/postgres-queries.yaml:/etc/postgres-exporter/queries.yaml
    networks:
      - ai-ready-network
```

#### 3.2.2 Redis Exporter配置
```yaml
# docker-compose-redis-exporter.yml
version: '3.8'
services:
  redis-exporter-main:
    image: oliver006/redis_exporter:latest
    container_name: redis-exporter-main
    restart: unless-stopped
    ports:
      - "9121:9121"
    environment:
      - REDIS_ADDR=redis://redis-main:6379
      - REDIS_PASSWORD=password
    networks:
      - ai-ready-network

  redis-exporter-inventory:
    image: oliver006/redis_exporter:latest
    container_name: redis-exporter-inventory
    restart: unless-stopped
    ports:
      - "9122:9121"
    environment:
      - REDIS_ADDR=redis://redis-inventory:6379
      - REDIS_PASSWORD=password
    networks:
      - ai-ready-network
```

### 3.3 Prometheus配置更新
```yaml
# prometheus.yml 新增配置
scrape_configs:
  # PostgreSQL监控
  - job_name: 'postgres-main'
    static_configs:
      - targets: ['postgres-exporter-main:9187']
    scrape_interval: 30s
    scrape_timeout: 25s

  - job_name: 'postgres-inventory'
    static_configs:
      - targets: ['postgres-exporter-inventory:9187']
    scrape_interval: 30s
    scrape_timeout: 25s

  # Redis监控
  - job_name: 'redis-main'
    static_configs:
      - targets: ['redis-exporter-main:9121']
    scrape_interval: 30s
    scrape_timeout: 25s

  - job_name: 'redis-inventory'
    static_configs:
      - targets: ['redis-exporter-inventory:9121']
    scrape_interval: 30s
    scrape_timeout: 25s
```

## 4. 性能优化策略

### 4.1 PostgreSQL性能优化

#### 4.1.1 连接池优化
```sql
-- 检查当前连接配置
SELECT name, setting, unit, context 
FROM pg_settings 
WHERE name LIKE '%connections%' OR name LIKE '%pool%';

-- 优化建议配置
-- postgresql.conf 调整
max_connections = 200                    -- 根据实际需求调整
shared_buffers = 4GB                     -- 25% 系统内存
work_mem = 64MB                          -- 每个操作内存
maintenance_work_mem = 1GB               -- 维护操作内存
effective_cache_size = 12GB              -- 预计可用缓存
```

#### 4.1.2 索引优化策略
```sql
-- 查找缺失索引
SELECT
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE tablename = 'your_table'
ORDER BY n_distinct DESC;

-- 检查索引使用情况
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

#### 4.1.3 查询优化
```sql
-- 查找慢查询
SELECT
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;

-- 查找需要优化的查询
SELECT
    query,
    calls,
    total_time,
    mean_time,
    stddev_time,
    rows / calls AS avg_rows
FROM pg_stat_statements
WHERE calls > 1000
ORDER BY total_time DESC
LIMIT 10;
```

### 4.2 Redis性能优化

#### 4.2.1 内存优化
```bash
# 检查内存使用情况
redis-cli info memory

# 优化建议配置
# redis.conf 调整
maxmemory 16gb                          -- 根据系统内存设置
maxmemory-policy allkeys-lru            -- 内存淘汰策略
activerehashing yes                     -- 激活rehash
hash-max-ziplist-entries 512           -- 小hash优化
hash-max-ziplist-value 64              -- 小hash优化
```

#### 4.2.2 持久化优化
```bash
# 检查持久化配置
redis-cli config get save
redis-cli config get appendonly

# 优化建议配置
# redis.conf 调整
save 900 1                              -- 900秒内至少1个key变化
save 300 10                             -- 300秒内至少10个key变化  
save 60 10000                           -- 60秒内至少10000个key变化
appendonly yes                          -- 开启AOF
appendfsync everysec                    -- 每秒同步
```

#### 4.2.3 连接优化
```bash
# 检查连接配置
redis-cli info clients

# 优化建议配置
# redis.conf 调整
maxclients 10000                        -- 最大客户端数
timeout 300                             -- 空闲连接超时
tcp-keepalive 60                        -- TCP保活
```

## 5. 监控仪表板设计

### 5.1 PostgreSQL监控仪表板

#### 5.1.1 概览面板
- 数据库连接数趋势图
- 查询QPS/TPS监控
- 缓存命中率仪表
- 活动会话监控

#### 5.1.2 性能面板
- 慢查询统计和趋势
- 索引使用情况分析
- 锁等待监控
- 事务回滚率

#### 5.1.3 存储面板
- 数据库大小增长趋势
- 表空间使用情况
- 死元组和膨胀率监控
- 预写日志(WAL)监控

### 5.2 Redis监控仪表板

#### 5.2.1 概览面板
- 内存使用情况和趋势
- 命令处理速率(OPS)
- 连接数监控
- 命中率监控

#### 5.2.2 性能面板
- 命令延迟分布(P50/P95/P99)
- 网络流量监控
- 键空间统计
- 过期键处理

#### 5.2.3 持久化面板
- RDB/AOF状态监控
- 持久化延迟监控
- 复制状态监控
- 故障转移状态

## 6. 告警规则配置

### 6.1 PostgreSQL告警规则
```yaml
# postgresql-alerts.yml
groups:
  - name: postgresql_alerts
    rules:
      # 连接数告警
      - alert: PostgreSQLHighConnections
        expr: pg_stat_database_numbackends{datname!~"template.*|postgres"} > 150
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL连接数过高"
          description: "数据库 {{ $labels.datname }} 连接数达到 {{ $value }}，超过阈值150"
      
      # 慢查询告警
      - alert: PostgreSQLSlowQueries
        expr: rate(pg_stat_statements_calls{query!~".*pg_stat_statements.*"}[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL慢查询过多"
          description: "检测到慢查询频率过高，需要优化"
      
      # 缓存命中率告警
      - alert: PostgreSQLLowCacheHitRatio
        expr: 100 * pg_stat_database_blks_hit / (pg_stat_database_blks_hit + pg_stat_database_blks_read) < 90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL缓存命中率过低"
          description: "数据库 {{ $labels.datname }} 缓存命中率 {{ $value }}%，低于90%阈值"
```

### 6.2 Redis告警规则
```yaml
# redis-alerts.yml
groups:
  - name: redis_alerts
    rules:
      # 内存使用告警
      - alert: RedisHighMemoryUsage
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis内存使用过高"
          description: "Redis实例 {{ $labels.instance }} 内存使用率达到 {{ $value | humanizePercentage }}"
      
      # 连接数告警
      - alert: RedisHighConnections
        expr: redis_connected_clients > 800
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis连接数过高"
          description: "Redis实例 {{ $labels.instance }} 连接数达到 {{ $value }}"
      
      # 高延迟告警
      - alert: RedisHighLatency
        expr: histogram_quantile(0.99, rate(redis_commands_duration_seconds_bucket[5m])) > 0.01
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Redis延迟过高"
          description: "Redis实例 {{ $labels.instance }} P99延迟 {{ $value }}s，超过10ms阈值"
```

## 7. 实施计划

### 7.1 阶段一：监控基础设施部署（第1周）
1. 部署PostgreSQL Exporter
2. 部署Redis Exporter
3. 更新Prometheus配置
4. 验证数据采集

### 7.2 阶段二：监控仪表板开发（第2周）
1. 创建PostgreSQL监控仪表板
2. 创建Redis监控仪表板
3. 配置告警规则
4. 验证告警功能

### 7.3 阶段三：性能优化实施（第3-4周）
1. 分析当前性能瓶颈
2. 实施连接池优化
3. 实施索引优化
4. 实施查询优化

### 7.4 阶段四：持续优化（第5周起）
1. 建立性能基线
2. 定期性能分析
3. 持续优化调整
4. 知识文档整理

## 8. 验收标准

### 8.1 监控覆盖度
- [ ] PostgreSQL基础指标监控覆盖100%
- [ ] Redis基础指标监控覆盖100%
- [ ] 慢查询监控机制建立
- [ ] 性能告警机制建立

### 8.2 优化效果
- [ ] 数据库响应时间P95 < 100ms
- [ ] 查询缓存命中率 > 90%
- [ ] 连接池利用率 < 80%
- [ ] 慢查询数量减少50%

### 8.3 文档完整性
- [ ] 监控方案文档完整
- [ ] 部署配置文档完整
- [ ] 优化案例文档完整
- [ ] 运维手册文档完整

## 9. 风险与应对

### 9.1 技术风险
- **风险**：Exporter部署影响数据库性能
- **应对**：在生产环境前先在测试环境充分验证

### 9.2 实施风险
- **风险**：优化调整导致业务异常
- **应对**：制定详细的回滚方案，分阶段实施

### 9.3 运维风险
- **风险**：监控数据量过大影响存储
- **应对**：设置合理的数据保留策略，定期清理

---

**版本历史**：
- v1.0 (2026-04-28)：初始版本创建
- 创建者：team-member
- 审核者：待审核

**相关文档**：
1. `qa/monitoring/basic-config-checklist.md`
2. `qa/database/database-backup-recovery-drill-plan.md`
3. `infra/docker/prometheus/prometheus.yml`