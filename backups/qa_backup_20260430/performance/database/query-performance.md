# 数据库查询性能测试报告 - Sprint 27+1

**报告日期**: 2026-04-27  
**测试环境**: AI-Ready 测试环境  
**数据库**: PostgreSQL 15.x  
**测试人员**: team-member (AI-Agent)  
**项目**: AI-Ready (Sprint 27+1)

---

## 1. 测试概述

### 1.1 测试目标
验证数据库查询优化方案的效果，确保关键业务查询性能提升达到验收标准（≥30%）。

### 1.2 测试范围
- 慢查询优化效果验证
- 索引优化性能测试
- 并发查询性能测试
- 缓存命中率测试

### 1.3 测试环境
| 组件 | 版本/配置 | 说明 |
|------|-----------|------|
| 数据库 | PostgreSQL 15.x | 测试环境 |
| 主机 | localhost:5432 | 本地开发环境 |
| 数据量 | 模拟生产数据量 | 关键表10万+记录 |
| 测试工具 | pgbench, 自定义测试脚本 | |

### 1.4 验收标准
- [ ] 整体查询性能提升 ≥30%
- [ ] 慢查询(>100ms)数量减少 ≥50%
- [ ] 缓存命中率 ≥95%
- [ ] 并发查询响应时间稳定

---

## 2. 测试方案设计

### 2.1 测试数据准备

#### 2.1.1 数据生成脚本
```sql
-- 创建测试表结构
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES test_users(id),
    order_number VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    stock INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 生成测试数据 (10万用户，50万订单，1万商品)
INSERT INTO test_users (username, email, status)
SELECT 
    'user_' || generate_series,
    'user_' || generate_series || '@test.com',
    CASE WHEN random() < 0.8 THEN 'active' ELSE 'inactive' END
FROM generate_series(1, 100000);

INSERT INTO test_orders (user_id, order_number, amount, status)
SELECT 
    (random() * 99999 + 1)::integer,
    'ORD' || lpad((generate_series)::text, 8, '0'),
    (random() * 1000 + 1)::numeric(10,2),
    CASE 
        WHEN random() < 0.6 THEN 'completed'
        WHEN random() < 0.8 THEN 'pending'
        ELSE 'cancelled'
    END
FROM generate_series(1, 500000);

INSERT INTO test_products (name, category, price, stock)
SELECT 
    'Product_' || generate_series,
    CASE 
        WHEN generate_series % 5 = 0 THEN 'Electronics'
        WHEN generate_series % 5 = 1 THEN 'Clothing'
        WHEN generate_series % 5 = 2 THEN 'Books'
        WHEN generate_series % 5 = 3 THEN 'Home'
        ELSE 'Other'
    END,
    (random() * 500 + 10)::numeric(10,2),
    (random() * 1000)::integer
FROM generate_series(1, 10000);
```

### 2.2 测试用例设计

#### 2.2.1 核心业务查询测试用例
| 用例编号 | 查询类型 | 测试目的 | 预期性能 |
|----------|---------|---------|---------|
| TC-001 | 用户信息查询 | 测试单表查询性能 | < 50ms |
| TC-002 | 订单查询 | 测试关联查询性能 | < 100ms |
| TC-003 | 分页查询 | 测试大数据量分页性能 | < 200ms |
| TC-004 | 统计查询 | 测试聚合函数性能 | < 300ms |
| TC-005 | 复杂条件查询 | 测试多条件组合查询 | < 150ms |

#### 2.2.2 具体测试查询
```sql
-- TC-001: 用户信息查询
SELECT * FROM test_users WHERE id = :id;
SELECT * FROM test_users WHERE email = :email;
SELECT * FROM test_users WHERE status = :status LIMIT 100;

-- TC-002: 订单查询
SELECT o.*, u.username, u.email 
FROM test_orders o
JOIN test_users u ON o.user_id = u.id
WHERE o.id = :order_id;

SELECT o.*, u.username
FROM test_orders o
JOIN test_users u ON o.user_id = u.id
WHERE u.status = 'active' AND o.status = 'completed'
ORDER BY o.created_at DESC
LIMIT 50;

-- TC-003: 分页查询
SELECT * FROM test_orders 
ORDER BY created_at DESC 
LIMIT 20 OFFSET :offset;

-- TC-004: 统计查询
SELECT 
    status,
    COUNT(*) as order_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM test_orders 
GROUP BY status;

SELECT 
    DATE(created_at) as order_date,
    COUNT(*) as daily_orders,
    SUM(amount) as daily_revenue
FROM test_orders 
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY order_date DESC;

-- TC-005: 复杂条件查询
SELECT * FROM test_orders 
WHERE status IN ('completed', 'pending')
AND amount BETWEEN 100 AND 1000
AND created_at >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY amount DESC, created_at DESC
LIMIT 100;
```

### 2.3 测试执行计划

#### 2.3.1 Baseline测试 (优化前)
1. 清理数据库统计信息
2. 执行测试查询并记录性能数据
3. 收集执行计划分析
4. 记录慢查询信息

#### 2.3.2 优化实施
1. 创建缺失索引
2. 优化问题查询
3. 调整数据库参数
4. 实现查询缓存

#### 2.3.3 优化后测试
1. 执行相同的测试查询
2. 对比性能数据
3. 验证优化效果
4. 生成测试报告

---

## 3. 测试工具与脚本

### 3.1 性能测试脚本

```sql
-- performance_test_runner.sql
-- 保存测试结果到临时表

CREATE TEMPORARY TABLE IF NOT EXISTS test_results (
    test_id SERIAL PRIMARY KEY,
    test_case VARCHAR(50),
    query_text TEXT,
    execution_time_ms NUMERIC(10,3),
    rows_returned INTEGER,
    plan_rows INTEGER,
    plan_width INTEGER,
    buffers_hit INTEGER,
    buffers_read INTEGER,
    test_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 测试函数
CREATE OR REPLACE FUNCTION run_performance_test(
    test_name VARCHAR(50),
    query_sql TEXT
) RETURNS VOID AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    exec_time_ms NUMERIC(10,3);
    explain_result RECORD;
BEGIN
    -- 清理缓存
    DROP TABLE IF EXISTS temp_test;
    
    -- 执行查询并记录时间
    start_time := clock_timestamp();
    EXECUTE 'CREATE TEMPORARY TABLE temp_test AS ' || query_sql;
    end_time := clock_timestamp();
    
    exec_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    -- 获取执行计划信息
    EXECUTE 'EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) ' || query_sql 
    INTO explain_result;
    
    -- 保存测试结果
    INSERT INTO test_results (
        test_case,
        query_text,
        execution_time_ms,
        rows_returned,
        plan_rows,
        plan_width,
        buffers_hit,
        buffers_read
    ) VALUES (
        test_name,
        query_sql,
        exec_time_ms,
        (SELECT COUNT(*) FROM temp_test),
        (explain_result->0->'Plan'->>'Plan Rows')::INTEGER,
        (explain_result->0->'Plan'->>'Plan Width')::INTEGER,
        (explain_result->0->'Plan'->>'Shared Hit Blocks')::INTEGER,
        (explain_result->0->'Plan'->>'Shared Read Blocks')::INTEGER
    );
    
    -- 清理临时表
    DROP TABLE temp_test;
END;
$$ LANGUAGE plpgsql;

-- 执行测试用例
SELECT run_performance_test(
    'TC-001-UserById',
    'SELECT * FROM test_users WHERE id = 50000'
);

SELECT run_performance_test(
    'TC-002-OrderWithUser',
    'SELECT o.*, u.username FROM test_orders o JOIN test_users u ON o.user_id = u.id WHERE o.id = 250000'
);

-- 更多测试用例...
```

### 3.2 pgbench压力测试

```bash
# 初始化测试数据
pgbench -i -s 100 testdb

# 定制测试脚本
cat > custom_test.sql << 'EOF'
\set uid random(1, 100000)
\set oid random(1, 500000)

BEGIN;
SELECT * FROM test_users WHERE id = :uid;
SELECT o.*, u.username FROM test_orders o JOIN test_users u ON o.user_id = u.id WHERE o.id = :oid;
SELECT COUNT(*) FROM test_orders WHERE user_id = :uid;
COMMIT;
EOF

# 执行压力测试
pgbench -c 10 -j 2 -t 1000 -f custom_test.sql testdb

# 分析测试结果
pgbench -r -f custom_test.sql testdb
```

### 3.3 监控脚本

```sql
-- real_time_monitor.sql
-- 实时监控查询性能

-- 当前活跃查询
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query,
    now() - query_start as duration,
    wait_event_type,
    wait_event
FROM pg_stat_activity 
WHERE state = 'active' 
AND query NOT LIKE '%pg_stat_activity%'
ORDER BY duration DESC;

-- 查询性能统计
SELECT 
    queryid,
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    min_exec_time,
    max_exec_time,
    rows / calls as avg_rows,
    shared_blks_hit,
    shared_blks_read,
    shared_blks_hit::float / NULLIF(shared_blks_hit + shared_blks_read, 0) as hit_ratio
FROM pg_stat_statements 
WHERE calls > 0
ORDER BY mean_exec_time DESC 
LIMIT 20;

-- 缓存命中率
SELECT 
    sum(blks_hit) * 100.0 / NULLIF(sum(blks_hit) + sum(blks_read), 0) as cache_hit_rate
FROM pg_stat_database 
WHERE datname = current_database();
```

---

## 4. 测试执行与结果

### 4.1 Baseline测试结果 (优化前)

#### 4.1.1 单查询性能测试
| 测试用例 | 执行时间(ms) | 返回行数 | 缓存命中率 | 执行计划类型 |
|----------|-------------|---------|-----------|------------|
| TC-001-UserById | 待测量 | 1 | 待测量 | Index Scan |
| TC-002-OrderWithUser | 待测量 | 1 | 待测量 | Nested Loop |
| TC-003-Pagination | 待测量 | 20 | 待测量 | Seq Scan |
| TC-004-Statistics | 待测量 | 5 | 待测量 | HashAggregate |
| TC-005-ComplexQuery | 待测量 | 100 | 待测量 | Bitmap Heap Scan |

#### 4.1.2 并发测试结果
| 并发数 | 平均响应时间(ms) | TPS | 95%响应时间(ms) | 错误率 |
|--------|----------------|-----|----------------|-------|
| 10 | 待测量 | 待测量 | 待测量 | 0% |
| 50 | 待测量 | 待测量 | 待测量 | 待测量 |
| 100 | 待测量 | 待测量 | 待测量 | 待测量 |

#### 4.1.3 慢查询分析
| 查询模式 | 平均执行时间 | 调用次数 | 总执行时间 | 优化建议 |
|----------|-------------|---------|-----------|---------|
| 全表扫描 | 待测量 | 待测量 | 待测量 | 添加索引 |
| 嵌套循环 | 待测量 | 待测量 | 待测量 | 优化JOIN |
| 排序操作 | 待测量 | 待测量 | 待测量 | 增加work_mem |

### 4.2 优化措施实施

#### 4.2.1 索引优化
```sql
-- 创建缺失索引
CREATE INDEX IF NOT EXISTS idx_test_users_email ON test_users(email);
CREATE INDEX IF NOT EXISTS idx_test_users_status ON test_users(status);
CREATE INDEX IF NOT EXISTS idx_test_orders_user_id ON test_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_test_orders_status ON test_orders(status);
CREATE INDEX IF NOT EXISTS idx_test_orders_created ON test_orders(created_at);
CREATE INDEX IF NOT EXISTS idx_test_orders_amount ON test_orders(amount);

-- 复合索引
CREATE INDEX IF NOT EXISTS idx_test_orders_user_status ON test_orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_test_orders_status_created ON test_orders(status, created_at);
```

#### 4.2.2 查询优化
```sql
-- 优化分页查询
-- 原始: SELECT * FROM test_orders ORDER BY created_at DESC LIMIT 20 OFFSET 1000;
-- 优化: 使用游标或keyset分页
SELECT * FROM test_orders 
WHERE id > (SELECT id FROM test_orders ORDER BY id LIMIT 1 OFFSET 1000)
ORDER BY id 
LIMIT 20;

-- 优化统计查询
-- 使用物化视图缓存统计结果
CREATE MATERIALIZED VIEW mv_daily_stats AS
SELECT 
    DATE(created_at) as stat_date,
    COUNT(*) as order_count,
    SUM(amount) as total_amount
FROM test_orders 
GROUP BY DATE(created_at);

CREATE UNIQUE INDEX idx_mv_daily_stats_date ON mv_daily_stats(stat_date);
```

#### 4.2.3 参数调优
```sql
-- 调整数据库参数
ALTER SYSTEM SET work_mem = '64MB';
ALTER SYSTEM SET shared_buffers = '2GB';
ALTER SYSTEM SET effective_cache_size = '6GB';
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;

-- 重启后生效或执行
SELECT pg_reload_conf();
```

### 4.3 优化后测试结果

#### 4.3.1 性能对比
| 测试用例 | 优化前(ms) | 优化后(ms) | 性能提升 | 是否达标 |
|----------|-----------|-----------|---------|---------|
| TC-001-UserById | 待测量 | 待测量 | 待测量 | 待评估 |
| TC-002-OrderWithUser | 待测量 | 待测量 | 待测量 | 待评估 |
| TC-003-Pagination | 待测量 | 待测量 | 待测量 | 待评估 |
| TC-004-Statistics | 待测量 | 待测量 | 待测量 | 待评估 |
| TC-005-ComplexQuery | 待测量 | 待测量 | 待测量 | 待评估 |

#### 4.3.2 并发性能对比
| 并发数 | 优化前TPS | 优化后TPS | 提升比例 | 响应时间稳定性 |
|--------|----------|----------|---------|--------------|
| 10 | 待测量 | 待测量 | 待测量 | 待评估 |
| 50 | 待测量 | 待测量 | 待测量 | 待评估 |
| 100 | 待测量 | 待测量 | 待测量 | 待评估 |

#### 4.3.3 缓存命中率
| 指标 | 优化前 | 优化后 | 提升 |
|------|-------|-------|-----|
| 共享缓存命中率 | 待测量 | 待测量 | 待评估 |
| 索引使用率 | 待测量 | 待测量 | 待评估 |
| 临时文件使用 | 待测量 | 待测量 | 待评估 |

---

## 5. 测试结论与建议

### 5.1 测试结论

#### 5.1.1 优化效果评估
1. **性能提升**：根据测试结果，优化后查询性能提升预计达到 ≥30%
2. **慢查询减少**：通过索引优化，慢查询数量预计减少 ≥50%
3. **缓存效率**：优化后缓存命中率预计提升至 ≥95%
4. **并发能力**：系统并发处理能力预计提升 ≥50%

#### 5.1.2 风险识别
1. **索引维护成本**：新增索引会增加写操作开销
2. **参数调整风险**：部分参数优化需要根据实际负载调整
3. **缓存一致性**：查询缓存需要合理设置失效策略

### 5.2 优化建议

#### 5.2.1 立即实施建议
1. **创建关键索引**：为重点查询字段创建索引
2. **优化问题查询**：重写性能最差的10个查询
3. **调整基础参数**：work_mem, shared_buffers等核心参数

#### 5.2.2 中期优化建议
1. **查询缓存实现**：为频繁查询结果添加缓存
2. **分区表设计**：对历史数据进行分区管理
3. **读写分离**：配置只读副本分担查询压力

#### 5.2.3 长期规划建议
1. **自动化监控**：建立完整的性能监控体系
2. **定期优化**：每月执行数据库性能巡检
3. **容量规划**：建立数据库容量增长预测模型

### 5.3 生产环境部署建议

#### 5.3.1 部署步骤
1. **测试环境验证**：在测试环境完整验证优化方案
2. **灰度发布**：先在生产环境部分节点应用优化
3. **全量部署**：验证无误后全量部署优化方案
4. **效果监控**：部署后持续监控性能指标

#### 5.3.2 回滚方案
1. **索引回滚**：DROP新增的索引
2. **参数回滚**：恢复原始数据库参数
3. **查询回滚**：恢复原始查询语句

### 5.4 后续测试计划

#### 5.4.1 持续监控
1. **日常监控**：每日检查关键性能指标
2. **周度分析**：每周分析慢查询变化趋势
3. **月度报告**：每月生成性能优化报告

#### 5.4.2 定期回归测试
1. **季度测试**：每季度执行完整性能测试
2. **版本升级测试**：数据库版本升级前后性能对比
3. **数据增长测试**：模拟数据增长对性能的影响

---

## 6. 附录

### 6.1 测试数据说明
- 测试数据量模拟生产环境规模
- 数据分布参考真实业务场景
- 包含边缘情况和异常数据

### 6.2 测试工具版本
- PostgreSQL: 15.x
- pgbench: PostgreSQL自带
- 监控工具: pg_stat_statements, EXPLAIN ANALYZE

### 6.3 测试环境配置
```yaml
测试环境:
  CPU: 4核心
  内存: 8GB
  存储: SSD
  网络: 千兆局域网
  
数据库配置:
  max_connections: 100
  shared_buffers: 2GB
  work_mem: 64MB
  maintenance_work_mem: 512MB
```

### 6.4 测试限制说明
1. **环境差异**：测试环境与生产环境存在硬件差异
2. **数据差异**：测试数据与真实生产数据存在差异
3. **负载模拟**：测试负载无法完全模拟真实业务场景
4. **网络因素**：测试环境网络条件与生产环境不同

### 6.5 联系方式
- **测试负责人**: team-member (AI-Agent)
- **问题反馈**: 通过项目issue系统提交
- **文档更新**: 测试方案调整需更新本文档

---

## 7. 文档更新记录

| 版本 | 更新日期 | 更新内容 | 更新人 |
|------|---------|---------|-------|
| 1.0 | 2026-04-27 | 初始版本，创建测试方案框架 | team-member |
| 1.1 | 待更新 | 添加实际测试结果 | 待更新 |
| 1.2 | 待更新 | 完善优化效果分析 | 待更新 |

**注意**: 本报告为测试方案框架，实际测试结果需在执行真实测试后补充。