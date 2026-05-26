# Sprint 27+1 测试环境数据库集群配置文档

## 1. 概述

### 1.1 文档目的
本文档提供Sprint 27+1测试环境数据库集群的详细配置说明，包括PostgreSQL主从复制配置、高可用方案、备份恢复策略等。

### 1.2 数据库架构
```
┌─────────────────────────────────────────────────────────┐
│                    PostgreSQL集群架构                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐      ┌─────────────┐                  │
│  │ 主数据库     │─────▶│ 从数据库     │                  │
│  │ (Master)    │ 流复制│ (Replica)   │                  │
│  └─────────────┘      └─────────────┘                  │
│        │                       │                       │
│  ┌─────▼──────┐        ┌──────▼─────┐                  │
│  │ 监控代理     │        │ 监控代理     │                  │
│  │ (Patroni)  │        │ (Patroni)  │                  │
│  └─────────────┘        └─────────────┘                  │
│        │                       │                       │
│  ┌─────▼───────────────────────▼─────┐                  │
│  │           Consul集群               │                  │
│  │      (服务发现和故障转移)           │                  │
│  └───────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

## 2. 集群配置

### 2.1 主从复制配置

#### 2.1.1 主数据库配置
```conf
# postgresql-master.conf
# 主数据库配置文件

# 连接设置
listen_addresses = '*'
port = 5432
max_connections = 100

# 复制设置
wal_level = replica
max_wal_senders = 10
wal_keep_size = 1GB
hot_standby = on

# 归档设置
archive_mode = on
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'
archive_timeout = 300

# 性能优化
shared_buffers = 256MB
effective_cache_size = 768MB
work_mem = 4MB
maintenance_work_mem = 64MB

# 日志设置
logging_collector = on
log_directory = '/var/log/postgresql'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 100MB
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on
log_temp_files = 0

# 认证设置
password_encryption = scram-sha-256
```

#### 2.1.2 从数据库配置
```conf
# postgresql-replica.conf
# 从数据库配置文件

# 连接设置
listen_addresses = '*'
port = 5433
max_connections = 100

# 复制设置
hot_standby = on
hot_standby_feedback = on
max_standby_streaming_delay = 30s
wal_receiver_status_interval = 10s

# 性能优化
shared_buffers = 256MB
effective_cache_size = 768MB
work_mem = 4MB
maintenance_work_mem = 64MB

# 只读模式
default_transaction_read_only = on
```

### 2.2 Patroni高可用配置

#### 2.2.1 Patroni主节点配置
```yaml
# patroni-master.yml
scope: ai-ready-postgres
name: postgres-master

restapi:
  listen: 0.0.0.0:8008
  connect_address: 192.168.1.101:8008
  auth: 'username:password'
  https: false

etcd:
  hosts:
    - 192.168.1.100:2379
    - 192.168.1.101:2379
    - 192.168.1.102:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      use_slots: true
      parameters:
        wal_level: replica
        hot_standby: "on"
        wal_keep_segments: 8
        max_wal_senders: 5
        max_replication_slots: 5
        wal_log_hints: "on"
        track_commit_timestamp: "off"

  initdb:
    - encoding: UTF8
    - locale: en_US.UTF-8
    - data-checksums

  pg_hba:
    - host replication replicator 0.0.0.0/0 md5
    - host all all 0.0.0.0/0 md5

postgresql:
  listen: 0.0.0.0:5432
  connect_address: 192.168.1.101:5432
  data_dir: /var/lib/postgresql/data
  pgpass: /var/lib/postgresql/.pgpass
  authentication:
    replication:
      username: replicator
      password: replicator_password
    superuser:
      username: postgres
      password: postgres_password
  parameters:
    unix_socket_directories: '/var/run/postgresql'
```

#### 2.2.2 Patroni从节点配置
```yaml
# patroni-replica.yml
scope: ai-ready-postgres
name: postgres-replica

restapi:
  listen: 0.0.0.0:8009
  connect_address: 192.168.1.102:8009
  auth: 'username:password'
  https: false

etcd:
  hosts:
    - 192.168.1.100:2379
    - 192.168.1.101:2379
    - 192.168.1.102:2379

postgresql:
  listen: 0.0.0.0:5433
  connect_address: 192.168.1.102:5433
  data_dir: /var/lib/postgresql/data
  pgpass: /var/lib/postgresql/.pgpass
  authentication:
    replication:
      username: replicator
      password: replicator_password
    superuser:
      username: postgres
      password: postgres_password
  parameters:
    unix_socket_directories: '/var/run/postgresql'
```

## 3. 部署脚本

### 3.1 数据库集群部署脚本
```bash
#!/bin/bash
# deploy-database-cluster.sh

set -e

echo "开始部署PostgreSQL数据库集群..."

# 1. 创建数据目录
echo "创建数据目录..."
sudo mkdir -p /data/postgresql/{master,replica,wal_archive}
sudo chown -R postgres:postgres /data/postgresql
sudo chmod -R 750 /data/postgresql

# 2. 安装PostgreSQL
echo "安装PostgreSQL..."
sudo apt-get update
sudo apt-get install -y postgresql-14 postgresql-client-14 postgresql-contrib-14

# 3. 配置主数据库
echo "配置主数据库..."
sudo cp postgresql-master.conf /etc/postgresql/14/main/postgresql.conf
sudo cp pg_hba-master.conf /etc/postgresql/14/main/pg_hba.conf

# 创建复制用户
sudo -u postgres psql -c "CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_password';"
sudo -u postgres psql -c "ALTER USER replicator WITH LOGIN;"

# 4. 启动主数据库
echo "启动主数据库..."
sudo systemctl restart postgresql@14-main

# 5. 配置从数据库
echo "配置从数据库..."
sudo -u postgres pg_basebackup -h 192.168.1.101 -D /data/postgresql/replica -U replicator -v -P -X stream

# 6. 配置从数据库恢复配置
echo "配置恢复配置..."
cat > /data/postgresql/replica/recovery.conf << EOF
standby_mode = 'on'
primary_conninfo = 'host=192.168.1.101 port=5432 user=replicator password=replicator_password application_name=replica1'
primary_slot_name = 'replica1_slot'
trigger_file = '/tmp/postgresql.trigger'
recovery_target_timeline = 'latest'
EOF

# 7. 安装和配置Patroni
echo "安装Patroni..."
sudo apt-get install -y python3-pip
sudo pip3 install patroni[etcd]

# 创建Patroni配置目录
sudo mkdir -p /etc/patroni
sudo cp patroni-master.yml /etc/patroni/patroni.yml

# 8. 启动Patroni服务
echo "启动Patroni服务..."
cat > /etc/systemd/system/patroni.service << EOF
[Unit]
Description=Patroni PostgreSQL HA
After=syslog.target network.target

[Service]
Type=simple
User=postgres
Group=postgres
ExecStart=/usr/local/bin/patroni /etc/patroni/patroni.yml
KillMode=process
TimeoutSec=30
Restart=no

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable patroni
sudo systemctl start patroni

echo "✅ PostgreSQL数据库集群部署完成！"
```

### 3.2 数据库初始化脚本
```bash
#!/bin/bash
# init-database.sh

set -e

echo "初始化数据库..."

# 连接到主数据库
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -d postgres << EOF

-- 创建应用数据库
CREATE DATABASE ai_ready WITH 
  ENCODING 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8'
  TEMPLATE = template0;

\c ai_ready

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- 创建应用用户
CREATE USER ai_ready_user WITH PASSWORD 'ai_ready_password';
GRANT CONNECT ON DATABASE ai_ready TO ai_ready_user;

-- 创建监控用户
CREATE USER monitor_user WITH PASSWORD 'monitor_password';
GRANT pg_monitor TO monitor_user;

-- 创建只读用户（用于从库）
CREATE USER readonly_user WITH PASSWORD 'readonly_password';
GRANT CONNECT ON DATABASE ai_ready TO readonly_user;

-- 创建表空间
CREATE TABLESPACE ai_ready_ts LOCATION '/data/postgresql/tablespaces';

-- 设置默认表空间
ALTER DATABASE ai_ready SET default_tablespace = ai_ready_ts;

EOF

echo "✅ 数据库初始化完成！"
```

## 4. 监控配置

### 4.1 Prometheus监控配置
```yaml
# prometheus-postgresql.yml
scrape_configs:
  - job_name: 'postgresql'
    static_configs:
      - targets:
        - '192.168.1.101:9187'  # postgres_exporter on master
        - '192.168.1.102:9187'  # postgres_exporter on replica
    metrics_path: /metrics
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '(.*):.*'
        replacement: '${1}'

  - job_name: 'patroni'
    static_configs:
      - targets:
        - '192.168.1.101:8008'  # patroni master
        - '192.168.1.102:8009'  # patroni replica
```

### 4.2 PostgreSQL Exporter配置
```bash
#!/bin/bash
# install-postgres-exporter.sh

# 安装PostgreSQL Exporter
wget https://github.com/prometheus-community/postgres_exporter/releases/download/v0.10.1/postgres_exporter-0.10.1.linux-amd64.tar.gz
tar -xzf postgres_exporter-0.10.1.linux-amd64.tar.gz
sudo mv postgres_exporter-0.10.1.linux-amd64/postgres_exporter /usr/local/bin/

# 创建服务配置文件
cat > /etc/systemd/system/postgres_exporter.service << EOF
[Unit]
Description=PostgreSQL Exporter
After=network.target

[Service]
Type=simple
User=postgres
Environment="DATA_SOURCE_NAME=postgresql://monitor_user:monitor_password@localhost:5432/ai_ready?sslmode=disable"
ExecStart=/usr/local/bin/postgres_exporter --web.listen-address=:9187 --web.telemetry-path=/metrics
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable postgres_exporter
sudo systemctl start postgres_exporter
```

## 5. 备份恢复策略

### 5.1 备份脚本
```bash
#!/bin/bash
# backup-database.sh

set -e

BACKUP_DIR="/data/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/ai_ready_backup_${DATE}.sql.gz"

echo "开始备份数据库..."

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
PGPASSWORD=postgres_password pg_dump -h 192.168.1.101 -U postgres -d ai_ready \
  --format=custom \
  --no-owner \
  --no-privileges \
  --verbose \
  | gzip > $BACKUP_FILE

# 备份WAL日志
rsync -av --delete /data/postgresql/wal_archive/ ${BACKUP_DIR}/wal_archive/

# 保留最近7天的备份
find $BACKUP_DIR -name "ai_ready_backup_*.sql.gz" -mtime +7 -delete

# 记录备份信息
echo "备份完成: $BACKUP_FILE" >> ${BACKUP_DIR}/backup.log
ls -lh $BACKUP_FILE

echo "✅ 数据库备份完成！"
```

### 5.2 恢复脚本
```bash
#!/bin/bash
# restore-database.sh

set -e

BACKUP_DIR="/data/backups/postgresql"
BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
  # 使用最新的备份文件
  BACKUP_FILE=$(ls -t ${BACKUP_DIR}/ai_ready_backup_*.sql.gz | head -1)
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "❌ 备份文件不存在: $BACKUP_FILE"
  exit 1
fi

echo "开始恢复数据库..."
echo "使用备份文件: $BACKUP_FILE"

# 停止应用连接
echo "停止应用连接..."
# 这里可以添加停止应用的逻辑

# 恢复数据库
echo "恢复数据库..."
gunzip -c $BACKUP_FILE | PGPASSWORD=postgres_password pg_restore -h 192.168.1.101 -U postgres -d ai_ready \
  --clean \
  --if-exists \
  --verbose

# 恢复WAL日志（如果需要）
echo "恢复WAL日志..."
# 这里可以添加WAL日志恢复的逻辑

echo "✅ 数据库恢复完成！"
```

## 6. 性能优化

### 6.1 性能监控脚本
```sql
-- performance-monitoring.sql
-- 性能监控查询

-- 查看当前活动连接
SELECT 
  pid,
  usename,
  application_name,
  client_addr,
  client_port,
  backend_start,
  state,
  query_start,
  query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_start;

-- 查看锁等待
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
  AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
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
WHERE NOT blocked_locks.granted;

-- 查看索引使用情况
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查看表大小
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS total_size,
  pg_size_pretty(pg_relation_size(schemaname || '.' || tablename)) AS table_size,
  pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename) - pg_relation_size(schemaname || '.' || tablename)) AS index_size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;
```

### 6.2 自动优化脚本
```bash
#!/bin/bash
# optimize-database.sh

set -e

echo "开始优化数据库..."

# 1. 更新统计信息
echo "更新统计信息..."
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -d ai_ready -c "ANALYZE;"

# 2. 重建索引
echo "重建索引..."
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -d ai_ready << EOF
SELECT 'REINDEX INDEX ' || indexname || ';' 
FROM pg_indexes 
WHERE schemaname = 'public' 
  AND tablename IN ('users', 'orders', 'products')
ORDER BY tablename, indexname;
EOF

# 3. 清理过期数据
echo "清理过期数据..."
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -d ai_ready << EOF
-- 清理30天前的日志
DELETE FROM audit_logs WHERE created_at < NOW() - INTERVAL '30 days';

-- 清理90天前的会话
DELETE FROM user_sessions WHERE last_activity < NOW() - INTERVAL '90 days';

-- 清理临时数据
DELETE FROM temp_data WHERE created_at < NOW() - INTERVAL '1 day';
EOF

# 4. 执行VACUUM
echo "执行VACUUM..."
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -d ai_ready -c "VACUUM ANALYZE;"

echo "✅ 数据库优化完成！"
```

## 7. 故障处理

### 7.1 常见故障处理

#### 7.1.1 主从复制中断
```bash
# 检查复制状态
PGPASSWORD=replicator_password psql -h 192.168.1.102 -U replicator -c "SELECT * FROM pg_stat_replication;"

# 重新创建复制槽
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -c "SELECT pg_drop_replication_slot('replica1_slot');"
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -c "SELECT * FROM pg_create_physical_replication_slot('replica1_slot');"
```

#### 7.1.2 数据库连接满
```bash
# 查看当前连接数
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

# 终止空闲连接
PGPASSWORD=postgres_password psql -h 192.168.1.101 -U postgres << EOF
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'idle' 
  AND pid <> pg_backend_pid() 
  AND state_change < NOW() - INTERVAL '10 minutes';
EOF
```

### 7.2 紧急恢复流程
```bash
#!/bin/bash
# emergency-recovery.sh

set -e

echo "开始紧急恢复流程..."

# 1. 停止应用服务
echo "停止应用服务..."
docker-compose -f docker-compose-application.yml down

# 2. 停止从数据库
echo "停止从数据库..."
sudo systemctl stop postgresql@14-replica

# 3. 提升从数据库为主
echo "提升从数据库为主..."
sudo -u postgres touch /tmp/postgresql.trigger

# 4. 重新配置应用连接
echo "重新配置应用连接..."
sed -i 's/192.168.1.101/192.168.1.102/g' application.yml

# 5. 启动应用服务
echo "启动应用服务..."
docker-compose -f docker-compose-application.yml up -d

# 6. 验证服务
echo "验证服务..."
./scripts/validate-application.sh

echo "✅ 紧急恢复完成！"
```

## 8. 附录

### 8.1 配置检查清单
- [ ] 主数据库配置文件正确
- [ ] 从数据库配置文件正确
- [ ] 复制用户配置正确
- [ ] Patroni配置正确
- [ ] 监控代理配置正确
- [ ] 备份目录权限正确
- [ ] 防火墙规则配置正确
- [ ] 服务发现配置正确

### 8.2 性能基准
| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| 查询响应时间 | < 100ms | - | - |
| 连接数使用率 | < 80% | - | - |
| 磁盘I/O使用率 | < 70% | - | - |
| CPU使用率 | < 60% | - | - |
| 内存使用率 | < 80% | - | - |
| 复制延迟 | < 1秒 | - | - |

### 8.3 相关命令参考
```bash
# 查看复制状态
pg_ctl status
pg_controldata

# 管理复制槽
SELECT * FROM pg_replication_slots;
SELECT pg_drop_replication_slot('slot_name');

# 查看WAL日志
pg_waldump

# 性能分析
pg_stat_statements_reset()
```

---

**文档版本**: v1.0  
**创建时间**: 2026-04-27  
**更新记录**:
- v1.0 (2026-04-27): 初始版本，创建数据库集群配置文档

**负责人**: doc-writer  
**审核人**: devops-engineer  
**批准人**: main