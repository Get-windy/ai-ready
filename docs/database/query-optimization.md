# 查询优化方案 - Sprint 27+1 测试环境

**文档日期**: 2026-04-27  
**数据库**: PostgreSQL 15.x  
**优化人员**: team-member (AI-Agent)  
**项目**: AI-Ready (Sprint 27+1)

---

## 1. 优化目标与验收标准

### 1.1 优化目标
- **整体性能提升**: ≥30% (与baseline对比)
- **慢查询减少**: 将执行时间 > 100ms的查询减少50%
- **缓存命中率**: shared_blks_hit率提升至 > 95%
- **索引覆盖率**: 关键查询字段索引覆盖率达到100%

### 1.2 验收标准
- [ ] 慢查询分析完成并生成报告
- [ ] 查询优化实施完成
- [ ] 性能提升≥30% (通过性能测试验证)
- [ ] 优化报告完整准确

---

## 2. 索引优化策略

### 2.1 索引设计原则

#### 2.1.1 B-Tree索引最佳实践
```sql
-- 单列索引
CREATE INDEX idx_table_column ON table_name(column_name);

-- 多列复合索引
CREATE INDEX idx_table_columns ON table_name(col1, col2, col3);

-- 包含列的索引 (PostgreSQL 11+)
CREATE INDEX idx_table_include ON table_name(col1) INCLUDE (col2, col3);
```

#### 2.1.2 索引选择策略
| 查询类型 | 推荐索引类型 | 示例 |
|---------|-------------|------|
| 等值查询 | B-Tree索引 | `WHERE id = 100` |
| 范围查询 | B-Tree索引 | `WHERE date > '2024-01-01'` |
| 全文搜索 | GIN/GiST索引 | `WHERE content @@ 'search'` |
| 地理位置 | GiST索引 | `WHERE location <-> point < 1000` |
| JSON查询 | GIN索引 | `WHERE json_column @> '{"key":"value"}'` |

### 2.2 索引创建脚本模板

```sql
-- 1. 分析表查询模式
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats 
WHERE schemaname = 'public' 
AND tablename = 'your_table'
ORDER BY attname;

-- 2. 创建常用索引
-- 主键索引 (通常已存在)
CREATE INDEX IF NOT EXISTS idx_pk_table ON table_name(id);

-- 外键索引 (建议为所有外键创建)
CREATE INDEX idx_fk_table_parent ON table_name(parent_id);

-- 频繁查询字段索引
CREATE INDEX idx_table_status ON table_name(status);
CREATE INDEX idx_table_created ON table_name(created_at);

-- 联合查询索引
CREATE INDEX idx_table_query ON table_name(column1, column2, column3);

-- 3. 删除未使用的索引
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes 
WHERE idx_scan = 0  -- 从未被使用的索引
ORDER BY pg_relation_size(schemaname||'.'||indexname) DESC;
```

---

## 3. SQL查询优化技巧

### 3.1 查询重写优化

#### 3.1.1 避免SELECT *
**原始查询**:
```sql
SELECT * FROM large_table WHERE condition;
```

**优化后**:
```sql
SELECT id, name, email FROM large_table WHERE condition;
```

**优化效果**:
- 减少数据传输量
- 提高缓存效率
- 降低I/O压力

#### 3.1.2 使用EXISTS替代IN
**原始查询**:
```sql
SELECT * FROM orders 
WHERE customer_id IN (
    SELECT id FROM customers WHERE status = 'active'
);
```

**优化后**:
```sql
SELECT o.* FROM orders o
WHERE EXISTS (
    SELECT 1 FROM customers c 
    WHERE c.id = o.customer_id AND c.status = 'active'
);
```

#### 3.1.3 分页查询优化
**原始查询**:
```sql
SELECT * FROM large_table 
ORDER BY created_at DESC 
LIMIT 20 OFFSET 1000;
```

**优化后**:
```sql
SELECT * FROM large_table 
WHERE id > (SELECT id FROM large_table ORDER BY id LIMIT 1 OFFSET 1000)
ORDER BY id 
LIMIT 20;
```

### 3.2 JOIN优化策略

#### 3.2.1 JOIN顺序优化
```sql
-- 原始: 大表在前
SELECT * FROM large_table l
JOIN small_table s ON l.id = s.large_id;

-- 优化: 小表在前
SELECT * FROM small_table s
JOIN large_table l ON s.large_id = l.id;
```

#### 3.2.2 避免笛卡尔积
```sql
-- 错误: 缺少JOIN条件
SELECT * FROM table1, table2;

-- 正确: 明确的JOIN条件
SELECT * FROM table1 
JOIN table2 ON table1.id = table2.table1_id;
```

### 3.3 子查询优化

#### 3.3.1 使用CTE优化复杂查询
```sql
-- 原始: 多层嵌套子查询
SELECT * FROM (
    SELECT * FROM (
        SELECT * FROM table1 WHERE condition1
    ) t1 WHERE condition2
) t2 WHERE condition3;

-- 优化: 使用CTE
WITH t1 AS (
    SELECT * FROM table1 WHERE condition1
),
t2 AS (
    SELECT * FROM t1 WHERE condition2
)
SELECT * FROM t2 WHERE condition3;
```

#### 3.3.2 使用LATERAL JOIN
```sql
-- 优化相关子查询
SELECT u.*, latest_order.*
FROM users u
CROSS JOIN LATERAL (
    SELECT * FROM orders o
    WHERE o.user_id = u.id
    ORDER BY o.created_at DESC
    LIMIT 1
) latest_order;
```

---

## 4. 数据库参数调优

### 4.1 内存相关参数

```sql
-- 查看当前配置
SHOW shared_buffers;
SHOW work_mem;
SHOW maintenance_work_mem;
SHOW effective_cache_size;

-- 推荐配置 (基于8GB内存的测试环境)
ALTER SYSTEM SET shared_buffers = '2GB';           -- 内存的25%
ALTER SYSTEM SET work_mem = '64MB';               -- 每个操作的排序内存
ALTER SYSTEM SET maintenance_work_mem = '512MB';  -- 维护操作内存
ALTER SYSTEM SET effective_cache_size = '6GB';    -- 操作系统缓存估计
```

### 4.2 I/O相关参数

```sql
-- 针对SSD优化
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;
ALTER SYSTEM SET seq_page_cost = 1.0;

-- WAL配置优化
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET max_wal_size = '4GB';
ALTER SYSTEM SET min_wal_size = '1GB';
```

### 4.3 连接和并发参数

```sql
-- 连接池配置
ALTER SYSTEM SET max_connections = 100;
ALTER SYSTEM SET superuser_reserved_connections = 3;

-- 并行查询配置
ALTER SYSTEM SET max_parallel_workers_per_gather = 4;
ALTER SYSTEM SET max_parallel_workers = 8;
ALTER SYSTEM SET max_parallel_maintenance_workers = 4;
```

---

## 5. 查询缓存实现方案

### 5.1 应用层缓存

#### 5.1.1 Redis缓存实现
```python
# Python示例
import redis
import json
import hashlib

class QueryCache:
    def __init__(self):
        self.redis = redis.Redis(host='localhost', port=6379, db=0)
    
    def get_cached_query(self, query, params, ttl=300):
        # 生成缓存键
        cache_key = self._generate_key(query, params)
        
        # 尝试从缓存获取
        cached_result = self.redis.get(cache_key)
        if cached_result:
            return json.loads(cached_result)
        
        # 执行查询并缓存结果
        result = self._execute_query(query, params)
        self.redis.setex(cache_key, ttl, json.dumps(result))
        return result
    
    def _generate_key(self, query, params):
        key_str = f"{query}:{json.dumps(params, sort_keys=True)}"
        return hashlib.md5(key_str.encode()).hexdigest()
```

#### 5.1.2 缓存策略
| 数据类型 | 缓存时间 | 缓存策略 |
|---------|---------|---------|
| 配置数据 | 24小时 | 全量缓存 |
| 用户数据 | 5分钟 | 按用户缓存 |
| 报表数据 | 1小时 | 按参数缓存 |
| 实时数据 | 30秒 | 短时缓存 |

### 5.2 数据库内部缓存

#### 5.2.1 物化视图
```sql
-- 创建物化视图
CREATE MATERIALIZED VIEW mv_user_orders AS
SELECT 
    u.id as user_id,
    u.name,
    COUNT(o.id) as order_count,
    SUM(o.amount) as total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name;

-- 定期刷新
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_user_orders;

-- 创建索引提升查询性能
CREATE INDEX idx_mv_user_id ON mv_user_orders(user_id);
```

#### 5.2.2 结果集缓存
```sql
-- 使用pgpool-II或pgBouncer的查询缓存功能
-- 配置pgpool.conf
memory_cache_enabled = on
memqcache_method = 'shmem'
memqcache_total_size = 64MB
memqcache_max_num_cache = 1000000
memqcache_expire = 0
```

---

## 6. 性能测试与验证

### 6.1 测试方案设计

#### 6.1.1 Baseline测试
```sql
-- 测试脚本模板
-- 1. 清理缓存
SELECT pg_stat_reset();
SELECT pg_stat_statements_reset();

-- 2. 执行测试查询
EXPLAIN (ANALYZE, BUFFERS, TIMING) 
SELECT * FROM target_table WHERE condition;

-- 3. 收集性能指标
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    rows / calls as avg_rows
FROM pg_stat_statements 
WHERE query LIKE '%target_table%';
```

#### 6.1.2 并发测试
```sql
-- 使用pgbench进行压力测试
pgbench -i -s 100 testdb  # 初始化100倍数据
pgbench -c 10 -j 2 -t 1000 testdb  # 10个客户端，2个线程，1000事务
```

### 6.2 性能指标监控

#### 6.2.1 关键性能指标
```sql
-- 查询性能指标
SELECT 
    datname,
    numbackends,
    xact_commit,
    xact_rollback,
    blks_read,
    blks_hit,
    tup_returned,
    tup_fetched,
    tup_inserted,
    tup_updated,
    tup_deleted
FROM pg_stat_database 
WHERE datname = 'devdb';

-- 索引使用统计
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(schemaname||'.'||indexname)) as index_size
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

#### 6.2.2 慢查询监控
```sql
-- 查看当前慢查询
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query,
    now() - query_start as duration
FROM pg_stat_activity 
WHERE state = 'active' 
AND now() - query_start > interval '100 milliseconds'
ORDER BY duration DESC;
```

---

## 7. 实施计划与风险控制

### 7.1 实施阶段

#### 阶段1: 分析与设计 (第1周)
- [ ] 连接测试数据库并收集性能数据
- [ ] 识别TOP 10慢查询
- [ ] 设计索引优化方案
- [ ] 制定SQL重写计划

#### 阶段2: 优化实施 (第2周)
- [ ] 创建缺失索引
- [ ] 重写问题查询
- [ ] 调整数据库参数
- [ ] 实现查询缓存

#### 阶段3: 测试验证 (第3周)
- [ ] 执行性能对比测试
- [ ] 验证优化效果
- [ ] 监控系统稳定性
- [ ] 生成优化报告

### 7.2 风险控制

#### 技术风险
1. **索引创建影响**：大表创建索引可能导致锁表
   - **缓解措施**：使用CONCURRENTLY创建索引
   - **回滚方案**：DROP INDEX恢复

2. **参数调整风险**：不当参数可能导致性能下降
   - **缓解措施**：先在测试环境验证
   - **监控指标**：实时监控数据库性能

3. **缓存一致性问题**：缓存数据与数据库不一致
   - **缓解措施**：设置合理的TTL
   - **失效策略**：实现缓存失效机制

#### 业务风险
1. **业务高峰期影响**：优化操作影响在线业务
   - **缓解措施**：在业务低峰期执行
   - **时间窗口**：选择凌晨2:00-4:00执行

2. **数据一致性风险**：优化操作导致数据不一致
   - **缓解措施**：完整备份后执行
   - **验证步骤**：优化后数据一致性检查

---

## 8. 优化效果评估

### 8.1 性能提升指标

| 指标 | 优化前 | 优化目标 | 优化后 | 提升比例 |
|------|--------|---------|--------|---------|
| 平均查询时间 | 待测量 | < 50ms | 待测量 | ≥30% |
| 缓存命中率 | 待测量 | > 95% | 待测量 | 提升 |
| 慢查询数量 | 待测量 | 减少50% | 待测量 | ≥50% |
| 并发处理能力 | 待测量 | 提升50% | 待测量 | ≥50% |

### 8.2 业务影响评估

1. **用户体验提升**：页面加载时间减少
2. **系统稳定性**：减少数据库连接超时
3. **运维成本**：降低数据库硬件需求
4. **开发效率**：简化复杂查询编写

---

## 9. 维护与监控

### 9.1 日常监控脚本

```sql
-- 每日性能检查脚本
-- 保存为 daily_performance_check.sql

-- 1. 数据库连接状态
SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active';

-- 2. 慢查询检查
SELECT count(*) as slow_queries 
FROM pg_stat_activity 
WHERE state = 'active' 
AND now() - query_start > interval '100 milliseconds';

-- 3. 缓存命中率
SELECT 
    sum(blks_hit) * 100.0 / (sum(blks_hit) + sum(blks_read)) as cache_hit_rate
FROM pg_stat_database 
WHERE datname = 'devdb';

-- 4. 索引使用情况
SELECT count(*) as unused_indexes
FROM pg_stat_user_indexes 
WHERE idx_scan = 0;

-- 5. 表空间使用
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;
```

### 9.2 告警配置

```yaml
# Prometheus告警规则
groups:
  - name: database_performance
    rules:
      - alert: HighSlowQueryRate
        expr: rate(pg_stat_statements_calls{mean_exec_time>0.1}[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "慢查询率过高"
          description: "过去5分钟慢查询(>100ms)数量超过10个/秒"
      
      - alert: LowCacheHitRate
        expr: pg_stat_database_blks_hit_rate < 0.9
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "缓存命中率过低"
          description: "数据库缓存命中率低于90%持续10分钟"
      
      - alert: TooManyConnections
        expr: pg_stat_activity_count > 80
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "数据库连接数过高"
          description: "活跃数据库连接数超过80个"
```

---

## 10. 总结与建议

### 10.1 优化总结
1. **已完成工作**：
   - 建立了完整的查询优化方案框架
   - 提供了索引设计、SQL优化、参数调优的具体方案
   - 设计了性能测试和监控方案

2. **待执行工作**：
   - 实际连接数据库执行分析
   - 根据真实数据调整优化策略
   - 执行性能对比测试

### 10.2 长期建议
1. **建立查询审查流程**：所有新查询上线前需经过性能审查
2. **定期性能巡检**：每周执行数据库性能检查
3. **自动化优化工具**：引入SQL审核和自动优化工具
4. **容量规划**：建立数据库容量增长预测模型

### 10.3 后续步骤
1. 使用提供的数据库配置连接测试环境
2. 执行本章中的分析脚本获取真实性能数据
3. 根据实际结果调整并实施优化方案
4. 完成性能测试并生成最终优化报告