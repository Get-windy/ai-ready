# AI-Ready 数据库性能优化方案

**版本**: v1.0  
**日期**: 2026-04-12  
**作者**: devops-engineer  

---

## 一、性能瓶颈分析

### 1.1 常见性能问题

| 问题类型 | 症状 | 影响 | 优先级 |
|----------|------|------|--------|
| 慢查询 | 查询时间 > 1s | 用户体验差 | P0 |
| 缺失索引 | 全表扫描 | CPU飙升 | P0 |
| 连接池耗尽 | 连接超时 | 服务不可用 | P0 |
| 锁竞争 | 死锁/等待 | 并发下降 | P1 |
| 大数据表 | 查询缓慢 | 磁盘IO高 | P1 |
| 内存不足 | 频繁换页 | 性能抖动 | P2 |

### 1.2 性能诊断方法

```sql
-- 查看慢查询日志
SELECT * FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 20;

-- 查看表扫描情况
SELECT schemaname, tablename, seq_scan, seq_tup_read,
       idx_scan, n_tup_ins, n_tup_upd, n_tup_del
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_scan DESC;

-- 查看索引使用情况
SELECT schemaname, tablename, indexrelname, idx_scan,
       pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查看锁等待
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

---

## 二、索引优化策略

### 2.1 索引设计原则

```
1. WHERE条件列 → 创建索引
2. JOIN条件列 → 创建索引
3. ORDER BY列 → 创建索引
4. 高选择性列 → 优先创建
5. 避免过多索引 → 影响写入性能
6. 定期分析 → 删除无用索引
```

### 2.2 核心表索引设计

#### 2.2.1 用户表 (sys_user)

```sql
-- 主键索引 (自动创建)
-- PRIMARY KEY (id)

-- 唯一索引
CREATE UNIQUE INDEX idx_user_username ON sys_user(username);
CREATE UNIQUE INDEX idx_user_email ON sys_user(email);
CREATE UNIQUE INDEX idx_user_phone ON sys_user(phone) WHERE phone IS NOT NULL;

-- 复合索引 (登录查询)
CREATE INDEX idx_user_status_type ON sys_user(status, user_type);

-- 覆盖索引 (列表查询)
CREATE INDEX idx_user_org_status ON sys_user(org_id, status) 
INCLUDE (username, real_name, email, phone, create_time);

-- 时间索引 (按时间范围查询)
CREATE INDEX idx_user_create_time ON sys_user(create_time DESC);

-- 部分索引 (仅查询活跃用户)
CREATE INDEX idx_user_active ON sys_user(last_login_time) 
WHERE status = 1;
```

#### 2.2.2 业务数据表 (biz_data)

```sql
-- 主键索引
-- PRIMARY KEY (id)

-- 外键索引
CREATE INDEX idx_data_user_id ON biz_data(user_id);
CREATE INDEX idx_data_org_id ON biz_data(org_id);
CREATE INDEX idx_data_project_id ON biz_data(project_id);

-- 复合索引 (常用查询组合)
CREATE INDEX idx_data_org_status ON biz_data(org_id, status, create_time DESC);
CREATE INDEX idx_data_user_status ON biz_data(user_id, status, update_time DESC);

-- 覆盖索引 (避免回表)
CREATE INDEX idx_data_query_cover ON biz_data(org_id, status) 
INCLUDE (title, content_summary, create_time, update_time);

-- 全文搜索索引
CREATE INDEX idx_data_title_fulltext ON biz_data 
USING gin(to_tsvector('chinese', title));

-- JSON字段索引
CREATE INDEX idx_data_attrs_gin ON biz_data 
USING gin(attrs jsonb_path_ops);
```

#### 2.2.3 日志表 (sys_log)

```sql
-- 主键索引
-- PRIMARY KEY (id)

-- 时间分区索引
CREATE INDEX idx_log_create_time ON sys_log(create_time DESC);

-- 复合索引 (日志查询)
CREATE INDEX idx_log_user_time ON sys_log(user_id, create_time DESC);
CREATE INDEX idx_log_type_time ON sys_log(log_type, create_time DESC);

-- 部分索引 (仅保留错误日志索引)
CREATE INDEX idx_log_error ON sys_log(create_time DESC) 
WHERE level = 'ERROR';
```

### 2.3 索引维护脚本

```sql
-- 查看索引大小和碎片
SELECT schemaname, tablename, indexrelname, 
       pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
       idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;

-- 重建索引 (消除碎片)
REINDEX INDEX CONCURRENTLY idx_user_username;

-- 分析表和索引
ANALYZE sys_user;
ANALYZE biz_data;

-- 查看未使用的索引
SELECT schemaname, tablename, indexrelname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0 AND indexrelname NOT LIKE 'pg_toast%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- 删除未使用的索引 (谨慎操作)
-- DROP INDEX IF EXISTS idx_unused;
```

---

## 三、SQL查询优化

### 3.1 优化原则

```
1. SELECT只取需要的列，避免 SELECT *
2. 使用 LIMIT 分页，避免大数据量查询
3. 避免在WHERE中使用函数，会导致索引失效
4. 使用EXPLAIN ANALYZE分析查询计划
5. 避免隐式类型转换
6. 使用JOIN代替子查询
7. 使用UNION ALL代替UNION (去重)
```

### 3.2 常见优化案例

#### 3.2.1 分页查询优化

```sql
-- 低效：大数据量偏移
SELECT * FROM biz_data 
WHERE status = 1 
ORDER BY create_time DESC 
LIMIT 10 OFFSET 100000;

-- 高效：游标分页 (Keyset Pagination)
SELECT * FROM biz_data 
WHERE status = 1 AND create_time < '2026-04-01'
ORDER BY create_time DESC 
LIMIT 10;

-- 高效：覆盖索引分页
SELECT id, title, create_time 
FROM biz_data 
WHERE status = 1 
ORDER BY create_time DESC 
LIMIT 10 OFFSET 100000;
```

#### 3.2.2 IN查询优化

```sql
-- 低效：大量IN值
SELECT * FROM biz_data WHERE user_id IN (1,2,3,...10000);

-- 高效：使用JOIN
SELECT d.* FROM biz_data d
JOIN (VALUES (1), (2), (3)) AS t(user_id) ON d.user_id = t.user_id;

-- 高效：使用临时表
CREATE TEMP TABLE tmp_users (user_id int PRIMARY KEY);
INSERT INTO tmp_users VALUES (1), (2), (3);
SELECT d.* FROM biz_data d
JOIN tmp_users t ON d.user_id = t.user_id;
```

#### 3.2.3 OR条件优化

```sql
-- 低效：OR导致索引失效
SELECT * FROM biz_data 
WHERE user_id = 1 OR org_id = 2;

-- 高效：使用UNION ALL
SELECT * FROM biz_data WHERE user_id = 1
UNION ALL
SELECT * FROM biz_data WHERE org_id = 2;

-- 高效：使用复合索引
CREATE INDEX idx_data_user_org ON biz_data(user_id, org_id);
```

#### 3.2.4 模糊查询优化

```sql
-- 低效：前模糊无法使用索引
SELECT * FROM sys_user WHERE username LIKE '%admin%';

-- 高效：后模糊可以使用索引
SELECT * FROM sys_user WHERE username LIKE 'admin%';

-- 高效：使用全文搜索
SELECT * FROM sys_user 
WHERE to_tsvector('chinese', username) @@ to_tsquery('admin');

-- 高效：使用pg_trgm模糊搜索
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_user_username_trgm ON sys_user USING gin(username gin_trgm_ops);
SELECT * FROM sys_user WHERE username LIKE '%admin%';
```

### 3.3 批量操作优化

```sql
-- 低效：逐条插入
INSERT INTO biz_data (title) VALUES ('A');
INSERT INTO biz_data (title) VALUES ('B');

-- 高效：批量插入
INSERT INTO biz_data (title) VALUES ('A'), ('B'), ('C');

-- 高效：使用COPY
COPY biz_data (title, content) FROM '/data/import.csv' WITH CSV;

-- 高效：批量更新
UPDATE biz_data 
SET status = 1 
WHERE id IN (SELECT id FROM tmp_ids);

-- 高效：批量删除 (分批删除避免大事务)
DELETE FROM sys_log 
WHERE create_time < '2025-01-01'
AND ctid = ANY(ARRAY(SELECT ctid FROM sys_log 
                     WHERE create_time < '2025-01-01' 
                     LIMIT 10000));
```

---

## 四、连接池优化 (HikariCP)

### 4.1 配置参数

```yaml
# application.yml
spring:
  datasource:
    hikari:
      # 基础配置
      jdbc-url: jdbc:postgresql://localhost:5432/ai_ready
      username: ${DB_USER:ai_ready}
      password: ${DB_PASS:ai_ready2026}
      driver-class-name: org.postgresql.Driver
      
      # 连接池大小配置
      minimum-idle: 10              # 最小空闲连接
      maximum-pool-size: 50         # 最大连接数
      connection-timeout: 30000     # 获取连接超时(30s)
      idle-timeout: 600000          # 空闲连接超时(10min)
      max-lifetime: 1800000         # 连接最大生命周期(30min)
      keepalive-time: 300000        # 连接保活时间(5min)
      
      # 性能优化配置
      prep-statement-cache-size: 500      # 预编译语句缓存大小
      prep-statement-cache-sql-limit: 2048 # SQL缓存长度限制
      use-server-prep-statements: true    # 使用服务端预编译
      cache-prep-statements: true         # 缓存预编译语句
      cache-result-set-metadata: true     # 缓存结果集元数据
      cache-server-configuration: true    # 缓存服务端配置
      
      # 连接测试配置
      connection-test-query: SELECT 1
      validation-timeout: 5000      # 连接验证超时(5s)
      leak-detection-threshold: 60000  # 连接泄漏检测阈值(60s)
      
      # 其他配置
      auto-commit: false            # 关闭自动提交
      transaction-isolation: TRANSACTION_READ_COMMITTED
```

### 4.2 连接池大小