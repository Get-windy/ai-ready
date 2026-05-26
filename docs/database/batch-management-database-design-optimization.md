# ERP批次管理模块数据库设计与性能优化

## 1. 数据库需求分析

### 1.1 业务数据模型分析
批次管理模块包含以下核心业务实体：

1. **批次（Batch）**：产品批次的基础信息，包含生产日期、有效期、数量状态等
2. **序列号（Serial）**：单品序列号管理，支持全生命周期追踪
3. **流转记录（Flow Record）**：批次/序列号的流转历史
4. **规则配置（Rule）**：批次生成规则和生命周期规则
5. **追溯日志（Traceability）**：追溯查询记录
6. **审计日志（Audit）**：数据变更审计
7. **快照缓存（Snapshot）**：性能优化用的快照数据

### 1.2 查询性能需求分析

#### 高频查询场景：
1. **批次查询**：按产品、状态、日期范围查询批次
   - 查询频率：高（1000+次/天）
   - 响应要求：< 100ms
   - 数据量：10-50万批次记录

2. **序列号追踪**：按序列号查询全生命周期
   - 查询频率：中（200-500次/天）
   - 响应要求：< 200ms
   - 数据量：100-500万序列号记录

3. **库存统计**：产品可用库存统计
   - 查询频率：高（每30分钟定时查询）
   - 响应要求：< 500ms
   - 数据量：聚合50+产品分类

4. **临期预警**：查询即将到期的批次
   - 查询频率：定时任务（每天1次）
   - 响应要求：< 2s
   - 数据量：筛选10%的批次记录

### 1.3 数据量预估

| 表名 | 当前数据量 | 1年后预估 | 3年后预估 | 年增长率 |
|------|------------|-----------|-----------|----------|
| batch_number | 50,000 | 150,000 | 400,000 | 200% |
| serial_number | 200,000 | 800,000 | 2,500,000 | 300% |
| batch_flow_record | 300,000 | 1,500,000 | 6,000,000 | 500% |
| serial_flow_record | 400,000 | 2,000,000 | 8,000,000 | 500% |
| traceability_log | 100,000 | 500,000 | 2,000,000 | 400% |
| batchsn_audit_log | 250,000 | 1,250,000 | 5,000,000 | 500% |

**总数据量预估**：
- 当前：约130万记录
- 1年后：约620万记录
- 3年后：约2380万记录

### 1.4 并发访问分析

#### 并发模式：
1. **读多写少**：查询占比80%，写入占比20%
2. **峰值时间**：工作日9:00-11:00，14:00-16:00
3. **并发用户数**：50-100并发用户

#### 并发峰值分析：
- **读操作峰值**：100 QPS（每秒查询次数）
- **写操作峰值**：20 TPS（每秒事务次数）
- **混合工作负载**：读:写 = 4:1

## 2. 现有数据库设计分析

### 2.1 现有表结构优势

✅ **优点**：
1. **分区设计**：使用独立的erp_batch_sn schema，隔离性好
2. **索引完整**：关键查询字段都有索引覆盖
3. **审计完整**：包含完善的审计日志机制
4. **性能优化**：包含快照缓存表用于统计查询
5. **软删除支持**：is_deleted字段支持软删除

### 2.2 需要优化的方面

⚠️ **问题与改进点**：

#### 1. 缺少表分区
```sql
-- 建议添加的表分区（当前缺失）
-- batch_number表按product_id或production_date分区
```

#### 2. 缺少复合索引
```sql
-- 常用查询组合缺少复合索引
-- 例如：product_id + batch_status + expiration_date
```

#### 3. 缺少数据归档策略
- 审计日志和流转记录表会快速增长
- 缺少历史数据归档机制

#### 4. 缺少读写分离配置
- 所有查询都走主库
- 缺少只读副本配置

## 3. 性能优化方案

### 3.1 数据库架构优化

#### 3.1.1 读写分离
```yaml
# 数据库连接配置
datasource:
  master:
    url: jdbc:postgresql://master-db:5432/ai_ready
    username: app_user
    password: ${DB_PASSWORD}
  slave:
    url: jdbc:postgresql://slave-db:5432/ai_ready
    username: app_user_ro
    password: ${DB_PASSWORD_RO}
```

#### 3.1.2 表分区策略
```sql
-- 批次表按年份分区
CREATE TABLE batch_number_2025 PARTITION OF batch_number
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE batch_number_2026 PARTITION OF batch_number
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
```

### 3.2 索引优化方案

#### 3.2.1 新增复合索引
```sql
-- 批次查询常用组合索引
CREATE INDEX idx_batch_query_optimized ON batch_number 
    (product_id, batch_status, expiration_date, created_at);

-- 序列号查询优化索引
CREATE INDEX idx_serial_query_optimized ON serial_number 
    (product_id, sn_status, warranty_end_date, created_at);

-- 流转记录查询优化
CREATE INDEX idx_flow_query_optimized ON batch_flow_record 
    (batch_id, flow_type, created_at DESC);
```

#### 3.2.2 索引维护策略
```sql
-- 定期重建索引（每月一次）
REINDEX INDEX CONCURRENTLY idx_batch_query_optimized;

-- 统计信息更新（每天）
ANALYZE batch_number;
```

### 3.3 查询优化方案

#### 3.3.1 常用查询优化
```sql
-- 优化前（全表扫描）
SELECT * FROM batch_number 
WHERE product_id = 1001 
AND expiration_date > CURRENT_DATE;

-- 优化后（使用覆盖索引）
SELECT id, batch_no, product_id, available_quantity 
FROM batch_number 
WHERE product_id = 1001 
AND expiration_date > CURRENT_DATE 
AND batch_status = 'ACTIVE'
ORDER BY expiration_date ASC 
LIMIT 100;
```

#### 3.3.2 分页查询优化
```sql
-- 优化前（OFFSET性能差）
SELECT * FROM batch_number 
ORDER BY created_at DESC 
OFFSET 10000 LIMIT 100;

-- 优化后（使用游标分页）
SELECT * FROM batch_number 
WHERE created_at < '2026-04-01' 
AND id > 10000
ORDER BY created_at DESC, id ASC 
LIMIT 100;
```

### 3.4 数据归档策略

#### 3.4.1 归档规则
1. **流转记录**：超过1年的数据归档到历史表
2. **审计日志**：超过6个月的数据归档到历史表
3. **追溯日志**：超过3个月的数据归档到历史表

#### 3.4.2 归档脚本
```sql
-- 每月执行的归档任务
CREATE PROCEDURE archive_old_data()
LANGUAGE plpgsql
AS $$
BEGIN
    -- 归档1年以上的流转记录
    INSERT INTO batch_flow_record_history 
    SELECT * FROM batch_flow_record 
    WHERE created_at < NOW() - INTERVAL '1 year';
    
    DELETE FROM batch_flow_record 
    WHERE created_at < NOW() - INTERVAL '1 year';
    
    -- 更新统计信息
    ANALYZE batch_flow_record;
END;
$$;
```

### 3.5 缓存策略

#### 3.5.1 Redis缓存配置
```yaml
# Redis缓存配置
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10分钟
      cache-null-values: false
      key-prefix: "batch:"
```

#### 3.5.2 缓存键设计
```java
// 批次信息缓存
batch:info:{batchId}
batch:product:{productId}:list
batch:stats:{productId}:daily

// 序列号缓存
serial:info:{serialNo}
serial:batch:{batchId}:list
```

## 4. 监控与维护方案

### 4.1 性能监控指标

#### 4.1.1 关键性能指标
1. **查询响应时间**：
   - 批次查询：P95 < 100ms
   - 序列号查询：P95 < 200ms
   - 统计查询：P95 < 500ms

2. **数据库连接**：
   - 连接池使用率：< 80%
   - 活跃连接数：< 50

3. **索引效率**：
   - 索引命中率：> 95%
   - 缓冲区命中率：> 98%

### 4.2 预警机制

#### 4.2.1 慢查询预警
```sql
-- 开启慢查询日志
ALTER SYSTEM SET log_min_duration_statement = '100ms';
ALTER SYSTEM SET log_statement = 'all';

-- 重启后检查
SELECT pg_reload_conf();
```

#### 4.2.2 空间预警
```sql
-- 表空间监控
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size
FROM pg_tables 
WHERE schemaname = 'erp_batch_sn'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

## 5. 实施计划

### 5.1 第一阶段（第1-2周）：基础优化
1. 新增复合索引
2. 优化查询语句
3. 配置基本监控

### 5.2 第二阶段（第3-4周）：架构优化
1. 实现读写分离
2. 配置Redis缓存
3. 实施数据归档策略

### 5.3 第三阶段（第5-6周）：高级优化
1. 实施表分区
2. 优化连接池配置
3. 完善监控告警

## 6. 预期效果

### 6.1 性能提升目标
1. **查询性能**：提升50-70%
2. **写入性能**：提升30-50%
3. **并发能力**：提升200%
4. **数据量支持**：支持5000万+记录

### 6.2 容量规划
1. **当前容量**：支持300万记录
2. **优化后容量**：支持5000万记录
3. **扩展能力**：支持横向扩展

### 6.3 维护成本降低
1. **监控自动化**：减少人工巡检
2. **预警提前**：问题提前发现
3. **维护窗口**：减少停机时间

## 7. 风险评估与应对

### 7.1 实施风险
1. **数据迁移风险**：影响在线业务
   - 应对：分批次迁移，设置回滚方案

2. **性能波动风险**：新增索引影响写入
   - 应对：使用CONCURRENTLY创建索引

3. **兼容性风险**：应用代码需要适配
   - 应对：灰度发布，逐步验证

### 7.2 监控指标
1. **实施期间监控**：
   - CPU使用率：< 70%
   - 内存使用率：< 80%
   - 磁盘IO：< 60%

2. **业务指标监控**：
   - 错误率：< 0.1%
   - 响应时间：满足SLA要求
   - 事务成功率：> 99.9%

## 8. 附录

### 8.1 索引创建脚本
```sql
-- 新增优化索引
CREATE INDEX CONCURRENTLY idx_batch_query_optimized ON erp_batch_sn.batch_number 
    (product_id, batch_status, expiration_date, created_at);

CREATE INDEX CONCURRENTLY idx_serial_query_optimized ON erp_batch_sn.serial_number 
    (product_id, sn_status, warranty_end_date, created_at);

-- 删除冗余索引
DROP INDEX IF EXISTS erp_batch_sn.idx_product_id;
DROP INDEX IF EXISTS erp_batch_sn.idx_batch_status;
```

### 8.2 监控查询脚本
```sql
-- 查询当前活跃连接
SELECT * FROM pg_stat_activity 
WHERE state = 'active' 
AND query NOT LIKE '%pg_stat_activity%';

-- 查询锁信息
SELECT 
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query AS blocked_statement,
    blocking_activity.query AS current_statement_in_blocking_process
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.DATABASE IS NOT DISTINCT FROM blocked_locks.DATABASE
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.GRANTED;
```

### 8.3 性能测试方案
```yaml
# 性能测试配置
test:
  concurrency: 100
  duration: 300 # 5分钟
  ramp-up: 60 # 1分钟预热
  
  scenarios:
    - name: 批次查询测试
      requests:
        - url: /api/batch/query
          method: GET
          params:
            productId: 1001
            status: ACTIVE
          think-time: 100ms
    
    - name: 序列号追踪测试
      requests:
        - url: /api/serial/trace
          method: GET
          params:
            serialNo: "SN202601000001"
          think-time: 200ms
```

---

**文档版本**：v1.0  
**创建时间**：2026-05-01  
**更新历史**：
- v1.0 (2026-05-01)：初始版本，基于现有数据库设计分析