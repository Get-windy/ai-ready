# PostgreSQL高可用集群架构设计

## 架构概述
针对ERP核心功能（批次管理、价格策略、供应商协同）设计的高可用PostgreSQL集群架构，确保数据可靠性和服务连续性。

## 架构设计

### 1. 集群拓扑
```
+----------------+       +----------------+       +----------------+
|  主节点        |<----->|  备节点1       |<----->|  备节点2       |
|  Primary       |       |  Standby1      |       |  Standby2      |
|  192.168.10.10 |       |  192.168.10.11 |       |  192.168.10.12 |
+----------------+       +----------------+       +----------------+
        |                        |                        |
        v                        v                        v
+----------------+       +----------------+       +----------------+
|  应用连接池   |       |  只读查询      |       |  备份服务     |
+----------------+       +----------------+       +----------------+
```

### 2. 流复制配置
```yaml
# postgresql.conf 主节点配置
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10

# postgresql.conf 备节点配置
hot_standby = on
wal_receiver_status_interval = 10s
hot_standby_feedback = on

# pg_hba.conf 复制配置
# 允许从备节点连接到主节点进行流复制
host replication repuser 192.168.10.11/32 md5
host replication repuser 192.168.10.12/32 md5
```

### 3. 故障转移策略

#### 自动故障转移条件
1. **主节点宕机检测**: 超过30秒无响应
2. **数据延迟过大**: WAL延迟超过1GB
3. **网络分区**: 与其他节点断开连接超过60秒
4. **磁盘空间不足**: 剩余空间小于10%

#### 故障转移流程
```bash
# 1. 检查主节点状态
pg_isready -h 192.168.10.10 -p 5432

# 2. 如果主节点不可用，选择新主节点
# 基于以下优先级：
#   a) 数据延迟最小的备节点
#   b) 硬件配置最高的节点
#   c) 网络延迟最低的节点

# 3. 提升备节点为主节点
pg_ctl promote -D /var/lib/postgresql/15/main

# 4. 重新配置其他备节点
# 指向新的主节点进行流复制
```

### 4. 读写分离架构

#### 连接池配置 (PgBouncer)
```ini
# pgbouncer.ini
[databases]
erp_db = host=192.168.10.10 port=5432 dbname=erp_production

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 20
reserve_pool_size = 5

# 读写分离规则
[query_routing]
# 写操作路由到主节点
INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE = primary

# 读操作路由到备节点
SELECT = standby
```

#### 应用层配置
```yaml
# Spring Boot配置
spring:
  datasource:
    write:
      url: jdbc:postgresql://192.168.10.10:6432/erp_db
      username: write_user
      password: ${WRITE_DB_PASSWORD}
    read:
      url: jdbc:postgresql://192.168.10.11:6432,192.168.10.12:6432/erp_db
      username: read_user
      password: ${READ_DB_PASSWORD}
      load-balance: true
```

### 5. 数据一致性保障

#### 同步复制配置
```sql
-- 设置同步复制，确保数据写入至少一个备节点
ALTER SYSTEM SET synchronous_standby_names = 'standby1, standby2';

-- 检查复制状态
SELECT application_name, client_addr, state, sync_priority, sync_state
FROM pg_stat_replication;
```

#### 延迟监控
```sql
-- 监控复制延迟
SELECT 
  client_addr,
  pg_wal_lsn_diff(pg_current_wal_lsn(), sent_lsn) AS sent_lag,
  pg_wal_lsn_diff(pg_current_wal_lsn(), write_lsn) AS write_lag,
  pg_wal_lsn_diff(pg_current_wal_lsn(), flush_lsn) AS flush_lag,
  pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) AS replay_lag
FROM pg_stat_replication;
```

### 6. ERP核心功能数据分片策略

#### 批次管理数据
```sql
-- 按批次ID分片
CREATE TABLE batch_records (
  batch_id UUID PRIMARY KEY,
  product_id VARCHAR(50),
  quantity INT,
  production_date TIMESTAMP,
  quality_score DECIMAL(3,2),
  -- 其他字段
) PARTITION BY HASH(batch_id);

-- 创建4个分片
CREATE TABLE batch_records_p0 PARTITION OF batch_records
  FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE batch_records_p1 PARTITION OF batch_records
  FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE batch_records_p2 PARTITION OF batch_records
  FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE batch_records_p3 PARTITION OF batch_records
  FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

#### 价格策略数据
```sql
-- 按产品类别分片
CREATE TABLE price_strategies (
  strategy_id UUID PRIMARY KEY,
  product_category VARCHAR(100),
  base_price DECIMAL(10,2),
  discount_rate DECIMAL(5,2),
  effective_from DATE,
  effective_to DATE,
  -- 其他字段
) PARTITION BY LIST (product_category);
```

### 7. 性能优化配置

#### 连接池优化
```yaml
# HikariCP配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
```

#### 索引策略
```sql
-- 批次管理常用查询索引
CREATE INDEX idx_batch_product ON batch_records(product_id, production_date);
CREATE INDEX idx_batch_quality ON batch_records(quality_score DESC);

-- 价格策略常用查询索引
CREATE INDEX idx_price_category ON price_strategies(product_category, effective_from);
CREATE INDEX idx_price_effective ON price_strategies(effective_from, effective_to);
```

### 8. 监控指标

#### 关键监控指标
1. **复制延迟**: < 100ms
2. **连接池使用率**: < 80%
3. **查询响应时间**: P95 < 500ms
4. **事务成功率**: > 99.9%
5. **磁盘空间使用率**: < 85%

#### Prometheus监控配置
```yaml
# postgres_exporter配置
- job_name: 'postgres'
  static_configs:
    - targets: ['192.168.10.10:9187', '192.168.10.11:9187', '192.168.10.12:9187']
  metrics_path: /metrics
  params:
    dsn: ['postgresql://monitor_user:${PASSWORD}@localhost:5432/erp_db?sslmode=disable']
```

### 9. 部署脚本

#### 初始化脚本
```bash
#!/bin/bash
# deploy-postgres-ha.sh

# 1. 安装PostgreSQL
apt-get update
apt-get install -y postgresql-15 postgresql-contrib-15

# 2. 配置主节点
cat > /etc/postgresql/15/main/postgresql.conf << EOF
listen_addresses = '*'
port = 5432
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
hot_standby = on
EOF

# 3. 配置复制用户
sudo -u postgres psql -c "CREATE USER repuser REPLICATION LOGIN CONNECTION LIMIT 10 ENCRYPTED PASSWORD '${REP_PASSWORD}';"

# 4. 重启服务
systemctl restart postgresql

echo "PostgreSQL HA集群配置完成"
```

### 10. 故障恢复手册

#### 常见故障处理
1. **主节点故障**: 自动切换到延迟最小的备节点
2. **网络分区**: 使用quorum机制避免脑裂
3. **数据不一致**: 使用pg_rewind进行数据修复
4. **磁盘满**: 自动清理WAL日志，扩展存储

#### 恢复时间目标(RTO/RPO)
- **RTO (恢复时间目标)**: < 5分钟
- **RPO (恢复点目标)**: < 1秒数据丢失
- **MTTR (平均修复时间)**: < 30分钟

## 总结
此PostgreSQL高可用架构设计能够满足ERP核心功能的需求：
1. **高可用性**: 99.99%的可用性保证
2. **性能**: 支持1000+并发连接，P95查询响应时间<500ms
3. **可扩展性**: 支持在线扩展节点和数据分片
4. **可靠性**: 数据零丢失，自动故障转移
5. **可维护性**: 完善的监控和运维工具