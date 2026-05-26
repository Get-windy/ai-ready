# 管理员配置手册

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: mnj0j12k  
**最后更新**: 2026-04-27  

---

## 目录

1. [高级配置](#高级配置)
2. [性能调优](#性能调优)
3. [安全配置](#安全配置)
4. [备份恢复](#备份恢复)
5. [故障恢复](#故障恢复)

---

## 高级配置

### PostgreSQL高级配置

#### 1. 连接池配置

编辑`monitoring-alerting-deployment.yml`中的PostgreSQL配置：

```yaml
command: >
  postgres
  -c max_connections=200
  -c shared_buffers=256MB
  -c effective_cache_size=1GB
  -c maintenance_work_mem=64MB
  -c checkpoint_completion_target=0.9
  -c wal_buffers=16MB
  -c default_statistics_target=100
```

#### 2. 性能调优参数

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| max_connections | 200 | 最大连接数 |
| shared_buffers | 256MB | 共享缓冲区大小 |
| effective_cache_size | 1GB | 有效缓存大小 |
| work_mem | 64MB | 排序和哈希操作的内存 |
| maintenance_work_mem | 64MB | 维护操作内存 |
| checkpoint_completion_target | 0.9 | 检查点完成目标 |
| wal_buffers | 16MB | WAL缓冲区大小 |

#### 3. 配置示例

```yaml
postgres-monitoring:
  image: postgres:15-alpine
  container_name: postgres-monitoring
  restart: unless-stopped
  environment:
    POSTGRES_DB: monitoring_db
    POSTGRES_USER: monitoring_user
    POSTGRES_PASSWORD: monitoring_pass_123
    POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"
  command: >
    postgres
    -c max_connections=200
    -c shared_buffers=256MB
    -c effective_cache_size=1GB
    -c work_mem=64MB
    -c maintenance_work_mem=64MB
    -c checkpoint_completion_target=0.9
    -c wal_buffers=16MB
    -c default_statistics_target=100
```

### Redis高级配置

#### 1. 内存配置

编辑`monitoring-alerting-deployment.yml`中的Redis配置：

```yaml
redis-monitoring:
  image: redis:7-alpine
  container_name: redis-monitoring
  restart: unless-stopped
  command: redis-server --requirepass redis_monitoring_pass_123 --maxmemory 256mb --maxmemory-policy allkeys-lru
```

#### 2. 持久化配置

```yaml
redis-monitoring:
  image: redis:7-alpine
  container_name: redis-monitoring
  restart: unless-stopped
  command: >
    redis-server
    --requirepass redis_monitoring_pass_123
    --maxmemory 256mb
    --maxmemory-policy allkeys-lru
    --appendonly yes
    --appendfsync everysec
```

### Prometheus高级配置

#### 1. 数据保留配置

编辑`prometheus.yml`配置文件：

```yaml
storage:
  tsdb:
    path: /prometheus
    retention: 30d
```

#### 2. 服务发现配置

```yaml
scrape_configs:
  - job_name: 'docker'
    file_sd_configs:
      - files:
        - /prometheus/file_sd/docker.json
        refresh_interval: 30s
```

### Grafana高级配置

#### 1. 数据源配置

编辑`grafana/provisioning/datasources/prometheus.yml`：

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

#### 2. 仪表盘配置

编辑`grafana/provisioning/dashboards/default.yml`：

```yaml
apiVersion: 1
providers:
  - name: 'default'
    orgId: 1
    folder: ''
    folderUid: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards/default
```

---

## 性能调优

### 数据库性能调优

#### 1. 索引优化

```sql
-- 查看慢查询
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;

-- 创建索引
CREATE INDEX idx_order_created ON orders(created_at);
CREATE INDEX idx_order_status ON orders(status);

-- 查看索引使用情况
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

#### 2. 查询优化

```sql
-- 分析查询计划
EXPLAIN ANALYZE SELECT * FROM orders WHERE created_at > '2026-01-01';

-- 优化查询
SELECT o.*, u.name
FROM orders o
INNER JOIN users u ON o.user_id = u.id
WHERE o.created_at > '2026-01-01'
ORDER BY o.created_at DESC
LIMIT 100;
```

### Redis性能调优

#### 1. 连接池配置

```java
// Jedis连接池配置
JedisPoolConfig poolConfig = new JedisPoolConfig();
poolConfig.setMaxTotal(100);
poolConfig.setMaxIdle(50);
poolConfig.setMinIdle(10);
poolConfig.setMaxWaitMillis(2000);
```

#### 2. 批量操作

```java
// 使用Pipeline批量操作
Pipeline pipeline = jedis.pipelined();
for (int i = 0; i < 1000; i++) {
    pipeline.set("key" + i, "value" + i);
}
pipeline.sync();
```

---

## 安全配置

### 数据库安全配置

#### 1. 访问控制

```sql
-- 创建用户
CREATE USER monitoring_user WITH PASSWORD 'strong_password';

-- 授予权限
GRANT CONNECT ON DATABASE monitoring_db TO monitoring_user;
GRANT USAGE ON SCHEMA public TO monitoring_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO monitoring_user;

-- 撤销权限
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
```

#### 2. 加密配置

```yaml
# 在docker-compose.yml中
environment:
  POSTGRES_PASSWORD: ${DB_PASSWORD}  # 从环境变量读取
```

### 网络安全配置

#### 1. Docker网络隔离

```yaml
networks:
  monitoring-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.0.0/24
```

#### 2. 防火墙配置

```bash
# 配置防火墙
ufw allow 80/tcp      # Nginx
ufw allow 443/tcp     # HTTPS
ufw deny 5433/tcp     # PostgreSQL外部访问
ufw deny 6380/tcp     # Redis外部访问
ufw deny 9090/tcp     # Prometheus外部访问
```

---

## 备份恢复

### 数据库备份

#### 1. PostgreSQL备份

```bash
# 完整备份
docker exec postgres-monitoring pg_dump -U monitoring_user monitoring_db > backup_$(date +%Y%m%d).sql

# 增量备份
docker exec postgres-monitoring pg_dump -U monitoring_user -d monitoring_db --format=custom > backup_$(date +%Y%m%d).dump
```

#### 2. Redis备份

```bash
# 快照备份
docker exec redis-monitoring redis-cli -a redis_monitoring_pass_123 BGSAVE

# 复制AOF文件
docker cp redis-monitoring:/data/appendonly.aof backup_appendonly_$(date +%Y%m%d).aof
```

### 数据恢复

#### 1. PostgreSQL恢复

```bash
# 恢复备份
docker exec -i postgres-monitoring psql -U monitoring_user -d monitoring_db < backup.sql

# 恢复自定义格式
docker exec -i postgres-monitoring pg_restore -U monitoring_user -d monitoring_db backup.dump
```

#### 2. Redis恢复

```bash
# 恢复AOF文件
docker cp backup_appendonly.aof redis-monitoring:/data/appendonly.aof

# 重启Redis
docker restart redis-monitoring
```

---

## 故障恢复

### 数据库故障恢复

#### 1. 连接失败

```bash
# 检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps postgres-monitoring

# 查看日志
docker-compose -f monitoring-alerting-deployment.yml logs postgres-monitoring

# 重启服务
docker-compose -f monitoring-alerting-deployment.yml restart postgres-monitoring
```

#### 2. 数据库损坏

```bash
# 尝试恢复
docker exec postgres-monitoring pg_restore -U monitoring_user -d monitoring_db --clean backup.dump

# 如果无法恢复，从备份恢复
docker exec -i postgres-monitoring psql -U monitoring_user -d monitoring_db < backup.sql
```

### 监控服务故障恢复

#### 1. Prometheus故障

```bash
# 检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps prometheus

# 查看日志
docker-compose -f monitoring-alerting-deployment.yml logs prometheus

# 检查存储
docker exec -it prometheus ls /prometheus

# 重启服务
docker-compose -f monitoring-alerting-deployment.yml restart prometheus
```

#### 2. Grafana故障

```bash
# 检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps grafana

# 查看日志
docker-compose -f monitoring-alerting-deployment.yml logs grafana

# 重启服务
docker-compose -f monitoring-alerting-deployment.yml restart grafana
```

---

## 监控告警规则参考

### 系统监控告警规则

```yaml
groups:
  - name: system-alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage on {{ $labels.instance }}"
          description: "CPU usage is above 80% (current value: {{ $value }}%)"

      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.instance }}"
          description: "Memory usage is above 80% (current value: {{ $value }}%)"

      - alert: HighDiskUsage
        expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"} * 100) < 15
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low disk space on {{ $labels.instance }}"
          description: "Disk usage is above 85% (current value: {{ $value }}%)"
```

### 数据库监控告警规则

```yaml
groups:
  - name: database-alerts
    rules:
      - alert: HighPostgreSQLConnections
        expr: pg_stat_activity_count > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High PostgreSQL connections on {{ $labels.instance }}"
          description: "Active connections: {{ $value }} (threshold: 100)"

      - alert: PostgreSQLSlowQueries
        expr: pg_stat_statements_avg_time > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow PostgreSQL queries on {{ $labels.instance }}"
          description: "Average query time: {{ $value }}ms (threshold: 1000ms)"
```

### 缓存监控告警规则

```yaml
groups:
  - name: cache-alerts
    rules:
      - alert: LowRedisHitRate
        expr: redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total) < 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low Redis hit rate on {{ $labels.instance }}"
          description: "Hit rate: {{ $value }} (threshold: 80%)"

      - alert: HighRedisMemory
        expr: redis_used_memory_bytes > 200000000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High Redis memory usage on {{ $labels.instance }}"
          description: "Memory usage: {{ $value }} bytes (threshold: 200MB)"
```

---

## 下一步

- 阅读[常见问题解答](user-manual-faq.md)获取更多帮助
- 观看[部署运维视频](videos/deployment-script.md)了解详细运维操作

---

## 联系我们

如有问题，请联系：

- **技术支援**: devops-engineer
- **文档反馈**: doc-writer
- **项目管理**: coordinator