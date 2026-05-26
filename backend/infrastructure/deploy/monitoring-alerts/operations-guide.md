# 监控告警模块运维手册

## 概述

本文档提供AI-Ready项目监控告警模块的运维管理指南。

## 运维职责

1. 服务监控和维护
2. 日志管理
3. 性能优化
4. 故障诊断和处理
5. 安全管理
6. 备份和恢复

## 服务管理

### 日常巡检

#### 1. 容器状态检查

```bash
# 检查所有容器状态
docker-compose --env-file .env.production ps

# 检查特定服务状态
docker ps -a --filter "name=ai-ready-*"
```

#### 2. 健康检查

```bash
# Prometheus健康检查
curl -s http://localhost:9090/-/healthy | jq

# AlertManager健康检查
curl -s http://localhost:9093/-/healthy | jq

# Grafana健康检查
curl -s http://localhost:3000/api/health | jq

# 监控API健康检查
curl -s http://localhost:8081/actuator/health | jq

# 自定义导出器健康检查
curl -s http://localhost:9101/health | jq

# Node Exporter健康检查
curl -s http://localhost:9100/metrics | head -10
```

#### 3. 数据库检查

```bash
# PostgreSQL状态
docker exec -it ai-ready-postgres-monitoring pg_isready

# 数据库大小
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -c "SELECT pg_database_size('monitoring_db');"

# 活动连接数
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -c "SELECT count(*) FROM pg_stat_activity;"
```

#### 4. Redis检查

```bash
# Redis状态
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> ping

# 内存使用
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> INFO memory

# 连接数
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> INFO clients
```

#### 5. RabbitMQ检查

```bash
# RabbitMQ状态
docker exec -it ai-ready-rabbitmq-monitoring rabbitmq-diagnostics ping

# 队列状态
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl list_queues

# 连接数
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl list_connections
```

### 服务重启

#### 1. 重启单个服务

```bash
# 重启Prometheus
docker-compose restart prometheus

# 重启AlertManager
docker-compose restart alertmanager

# 重启Grafana
docker-compose restart grafana

# 重启自定义导出器
docker-compose restart custom-exporter

# 重启监控API
docker-compose restart monitoring-api

# 重启Node Exporter
docker-compose restart node-exporter

# 重启Nginx
docker-compose restart nginx
```

#### 2. 重启所有服务

```bash
docker-compose restart
```

#### 3. 完全重启（包括数据库和缓存）

```bash
docker-compose down
docker-compose --env-file .env.production up -d
```

## 日志管理

### 日志查看

#### 1. 容器日志

```bash
# 实时查看日志
docker logs -f <container_name>

# 查看最近100行日志
docker logs --tail 100 <container_name>

# 查看特定时间段的日志
docker logs --since 2026-04-29T00:00:00 <container_name>
```

#### 2. 应用日志

```bash
# 监控API日志
tail -f logs/monitoring-api/app.log

# 自定义导出器日志
docker logs -f ai-ready-custom-exporter
```

### 日志分析

#### 1. 错误日志分析

```bash
# 查找ERROR级别日志
grep "ERROR" logs/monitoring-api/app.log

# 查找特定错误
grep "NullPointerException" logs/monitoring-api/app.log

# 统计错误数量
grep -c "ERROR" logs/monitoring-api/app.log
```

#### 2. 性能日志分析

```bash
# 查找慢请求
grep "响应时间>" logs/monitoring-api/app.log

# 查找内存告警
grep "内存使用>" logs/monitoring-api/app.log
```

### 日志清理

```bash
# 清理30天前的日志
find logs/monitoring-api -name "*.log" -mtime +30 -delete

# 压缩日志
gzip logs/monitoring-api/app.log.2026-04-28
```

## 性能优化

### JVM参数调优

修改 `.env.production`:

```bash
JAVA_OPTS=-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Prometheus优化

#### 1. 调整保留时间

修改 `.env.production`:

```bash
RETENTION_TIME=15d
RETENTION_SIZE=5GB
```

#### 2. 优化采集间隔

修改 `prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 30s  # 默认15s，可调整为30s减少负载
  evaluation_interval: 30s
```

### Grafana优化

#### 1. 调整数据源缓存

修改 Grafana配置:

```ini
[dataproxy]
timeout = 30
keep_alive = 30
```

#### 2. 优化仪表板刷新

在Grafana仪表板设置中调整刷新间隔为30s或60s。

### 数据库优化

#### 1. 调整PostgreSQL参数

修改 PostgreSQL配置:

```sql
-- 增加共享缓冲区
ALTER SYSTEM SET shared_buffers = '512MB';

-- 增加工作内存
ALTER SYSTEM SET work_mem = '64MB';

-- 重启PostgreSQL生效
SELECT pg_reload_conf();
```

#### 2. 创建索引

```sql
-- 监控指标索引
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp);
CREATE INDEX idx_metrics_name ON metrics(metric_name);

-- 告警规则索引
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_created_at ON alerts(created_at);
```

## 故障诊断

### 容器故障

#### 1. 容器无法启动

```bash
# 检查日志
docker logs <container_name>

# 检查配置
docker inspect <container_name>

# 检查资源限制
docker stats <container_name>
```

#### 2. 容器频繁重启

```bash
# 检查重启次数
docker inspect --format='{{.RestartCount}}' <container_name>

# 检查OOM事件
dmesg | grep -i "Out of memory"
```

### 数据库故障

#### 1. 连接失败

```bash
# 检查PostgreSQL状态
docker exec -it ai-ready-postgres-monitoring pg_isready

# 检查连接数
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -c "SELECT count(*) FROM pg_stat_activity;"

# 检查最大连接数
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -c "SHOW max_connections;"
```

#### 2. 性能下降

```bash
# 检查慢查询
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"

# 检查锁
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -c "SELECT * FROM pg_locks;"
```

### Redis故障

#### 1. 连接失败

```bash
# 检查Redis状态
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> ping

# 检查内存
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> INFO memory
```

#### 2. 性能下降

```bash
# 检查慢日志
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> SLOWLOG GET 10

# 检查大键
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> --bigkeys
```

### RabbitMQ故障

#### 1. 连接失败

```bash
# 检查RabbitMQ状态
docker exec -it ai-ready-rabbitmq-monitoring rabbitmq-diagnostics ping

# 检查队列状态
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl list_queues
```

#### 2. 消息堆积

```bash
# 检查消息队列长度
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl list_queues name messages_ready messages_unacknowledged

# 检查消费者
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl list_consumers
```

## 安全管理

### 访问控制

#### 1. 网络隔离

```bash
# 检查网络配置
docker network inspect monitoring-network

# 限制外部访问
# 仅允许内部网络访问数据库、Redis和RabbitMQ
```

#### 2. 密码管理

```bash
# 定期更换密码
# 修改 .env.production 文件

# 使用强密码
# 密码长度至少16位，包含字母、数字、特殊字符
```

### 安全审计

#### 1. 日志审计

```bash
# 检查登录日志
grep "登录" logs/monitoring-api/app.log

# 检查权限变更
grep "权限" logs/monitoring-api/app.log
```

#### 2. 安全扫描

```bash
# 容器安全扫描
docker scan ai-ready/custom-exporter:latest

# 依赖安全扫描
mvn dependency-check:check
```

## 备份和恢复

### 数据备份

#### 1. PostgreSQL备份

```bash
# 全量备份
docker exec -it ai-ready-postgres-monitoring pg_dump -U monitoring_user monitoring_db > backup_$(date +%Y%m%d).sql

# 增量备份
docker exec -it ai-ready-postgres-monitoring pg_dump -U monitoring_user --format=custom monitoring_db > backup_$(date +%Y%m%d).dump
```

#### 2. Redis备份

```bash
# RDB备份
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码> BGSAVE

# AOF备份
docker cp ai-ready-redis-monitoring:/data/appendonly.aof backup_$(date +%Y%m%d).aof
```

#### 3. RabbitMQ备份

```bash
# 定义备份
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl export_definitions /tmp/rabbitmq-definitions.json
docker cp ai-ready-rabbitmq-monitoring:/tmp/rabbitmq-definitions.json backup_$(date +%Y%m%d).json
```

#### 4. 应用配置备份

```bash
# 备份配置文件
tar -czf config_backup_$(date +%Y%m%d).tar.gz I:\AI-Ready\deploy\monitoring-alerts\*.yml I:\AI-Ready\deploy\monitoring-alerts\.env*
```

### 数据恢复

#### 1. PostgreSQL恢复

```bash
# 恢复SQL备份
docker exec -i ai-ready-postgres-monitoring psql -U monitoring_user monitoring_db < backup_20260429.sql

# 恢复自定义格式备份
docker exec -i ai-ready-postgres-monitoring pg_restore -U monitoring_user -d monitoring_db backup_20260429.dump
```

#### 2. Redis恢复

```bash
# 恢复RDB备份
docker cp backup_20260429.rdb ai-ready-redis-monitoring:/data/dump.rdb
docker restart ai-ready-redis-monitoring

# 恢复AOF备份
docker cp backup_20260429.aof ai-ready-redis-monitoring:/data/appendonly.aof
docker restart ai-ready-redis-monitoring
```

#### 3. RabbitMQ恢复

```bash
# 恢复定义
docker cp backup_20260429.json ai-ready-rabbitmq-monitoring:/tmp/rabbitmq-definitions.json
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl import_definitions /tmp/rabbitmq-definitions.json
```

## 监控告警

### Prometheus监控

访问: http://localhost:9090

关键指标:
- CPU使用率
- 内存使用率
- JVM堆内存
- 响应时间
- 错误率
- 数据库连接池
- Redis连接状态
- RabbitMQ队列长度
- 磁盘使用率

### Grafana监控

访问: http://localhost:3000

关键仪表板:
- 监控系统总览
- 系统资源监控
- 数据库性能
- Redis性能
- RabbitMQ性能
- JVM性能
- 业务指标监控

### 告警处理

#### 1. 查看告警

```bash
# Prometheus告警
curl http://localhost:9090/api/v1/alerts

# Alertmanager告警
curl http://localhost:9093/api/v1/alerts
```

#### 2. 告警响应流程

1. 接收告警通知
2. 分析告警原因
3. 检查服务状态
4. 采取修复措施
5. 验证告警解除
6. 记录处理过程

## 运维检查清单

### 每日检查

- [ ] 所有容器状态正常
- [ ] 健康检查通过
- [ ] 数据库连接正常
- [ ] Redis连接正常
- [ ] RabbitMQ连接正常
- [ ] 无ERROR级别日志
- [ ] 监控指标正常
- [ ] 无活跃告警

### 每周检查

- [ ] 日志清理完成
- [ ] 备份文件生成
- [ ] 性能指标分析
- [ ] 安全扫描完成
- [ ] 配置文件审查

### 每月检查

- [ ] 数据备份验证
- [ ] 密码更换
- [ ] 性能优化评估
- [ ] 安全审计报告
- [ ] 运维文档更新

## 附录

### 运维工具

- Docker: 容器管理
- Docker Compose: 服务编排
- Prometheus: 监控系统
- Grafana: 监控面板
- AlertManager: 告警管理
- psql: PostgreSQL客户端
- redis-cli: Redis客户端
- rabbitmqctl: RabbitMQ管理工具

### 常用命令速查

```bash
# 服务管理
docker-compose ps                # 查看服务状态
docker-compose restart <service> # 重启服务
docker-compose down              # 停止所有服务
docker-compose up -d             # 启动所有服务

# 日志查看
docker logs <container>          # 查看容器日志
tail -f logs/monitoring-api/app.log  # 实时查看应用日志

# 监控访问
curl http://localhost:9090       # Prometheus
curl http://localhost:9093       # AlertManager
curl http://localhost:3000       # Grafana

# 数据库操作
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user  # PostgreSQL客户端
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码>  # Redis客户端
docker exec -it ai-ready-rabbitmq-monitoring rabbitmqctl  # RabbitMQ管理

# 备份恢复
pg_dump -U monitoring_user monitoring_db > backup.sql  # PostgreSQL备份
psql -U monitoring_user monitoring_db < backup.sql     # PostgreSQL恢复
```

### 紧急联系方式

- 运维负责人: devops-engineer
- 技术支持: coordinator
- 系统管理员: main