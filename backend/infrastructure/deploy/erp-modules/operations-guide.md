# ERP Modules 运维手册

## 概述

本文档提供AI-Ready项目5个P0 ERP核心模块的运维管理指南。

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
docker-compose --env-file .env-production ps

# 检查特定模块状态
docker ps -a --filter "name=ai-ready-erp-*"
```

#### 2. 健康检查

```bash
# 批次/序列号管理
curl -s http://localhost:8081/actuator/health | jq

# 发票管理
curl -s http://localhost:8082/actuator/health | jq

# 采购询价/报价管理
curl -s http://localhost:8083/actuator/health | jq

# 销售价格策略管理
curl -s http://localhost:8084/actuator/health | jq

# 供应商协同门户
curl -s http://localhost:8085/actuator/health | jq
```

#### 3. 数据库检查

```bash
# PostgreSQL状态
docker exec -it ai-ready-erp-postgres pg_isready

# 数据库大小
docker exec -it ai-ready-erp-postgres psql -U erpadmin -c "SELECT pg_database_size('ai_ready_erp');"

# 活动连接数
docker exec -it ai-ready-erp-postgres psql -U erpadmin -c "SELECT count(*) FROM pg_stat_activity;"
```

#### 4. Redis检查

```bash
# Redis状态
docker exec -it ai-ready-erp-redis redis-cli -a <密码> ping

# 内存使用
docker exec -it ai-ready-erp-redis redis-cli -a <密码> INFO memory

# 连接数
docker exec -it ai-ready-erp-redis redis-cli -a <密码> INFO clients
```

### 服务重启

#### 1. 重启单个模块

```bash
# 重启批次/序列号管理模块
docker-compose restart erp-batch-sn

# 重启发票管理模块
docker-compose restart erp-invoice

# 重启采购询价/报价管理模块
docker-compose restart erp-purchase-exchange

# 重启销售价格策略管理模块
docker-compose restart erp-sales-exchange

# 重启供应商协同门户模块
docker-compose restart supplier-portal
```

#### 2. 重启所有模块

```bash
docker-compose restart
```

#### 3. 完全重启（包括数据库和Redis）

```bash
docker-compose down
docker-compose --env-file .env-production up -d
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
# 批次/序列号管理模块日志
tail -f /var/log/ai-ready/erp/batch-sn/app.log

# 发票管理模块日志
tail -f /var/log/ai-ready/erp/invoice/app.log

# 采购询价/报价管理模块日志
tail -f /var/log/ai-ready/erp/purchase-exchange/app.log

# 销售价格策略管理模块日志
tail -f /var/log/ai-ready/erp/sales-exchange/app.log

# 供应商协同门户模块日志
tail -f /var/log/ai-ready/erp/supplier-portal/app.log
```

### 日志分析

#### 1. 错误日志分析

```bash
# 查找ERROR级别日志
grep "ERROR" /var/log/ai-ready/erp/*/app.log

# 查找特定错误
grep "NullPointerException" /var/log/ai-ready/erp/*/app.log

# 统计错误数量
grep -c "ERROR" /var/log/ai-ready/erp/*/app.log
```

#### 2. 性能日志分析

```bash
# 查找慢请求
grep "响应时间>" /var/log/ai-ready/erp/*/app.log

# 查找内存告警
grep "内存使用>" /var/log/ai-ready/erp/*/app.log
```

### 日志清理

```bash
# 清理30天前的日志
find /var/log/ai-ready/erp -name "*.log" -mtime +30 -delete

# 压缩日志
gzip /var/log/ai-ready/erp/*/app.log.2026-04-28
```

## 性能优化

### JVM参数调优

#### 1. 调整内存配置

修改 `.env.production`:

```bash
JAVA_OPTS=-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

#### 2. 启用GC日志

```bash
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -Xlog:gc*:file=/app/logs/gc.log:time,level,tags
```

### 数据库优化

#### 1. 调整连接池

修改应用配置:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### 2. 创建索引

```sql
-- 批次管理索引
CREATE INDEX idx_batch_number ON erp_batch.batch_number;
CREATE INDEX idx_batch_creation_date ON erp_batch.created_at;

-- 发票管理索引
CREATE INDEX idx_invoice_number ON erp_invoice.invoice_number;
CREATE INDEX idx_invoice_status ON erp_invoice.status;

-- 询价/报价索引
CREATE INDEX idx_inquiry_supplier ON erp_purchase.inquiry_supplier_id;
CREATE INDEX idx_quote_status ON erp_purchase.quote_status;
```

### Redis优化

#### 1. 调整内存限制

```bash
# 修改Redis配置
maxmemory 2gb
maxmemory-policy allkeys-lru
```

#### 2. 持久化配置

```bash
# 启用AOF持久化
appendonly yes
appendfsync everysec
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
docker exec -it ai-ready-erp-postgres pg_isready

# 检查连接数
docker exec -it ai-ready-erp-postgres psql -U erpadmin -c "SELECT count(*) FROM pg_stat_activity;"

# 检查最大连接数
docker exec -it ai-ready-erp-postgres psql -U erpadmin -c "SHOW max_connections;"
```

#### 2. 性能下降

```bash
# 检查慢查询
docker exec -it ai-ready-erp-postgres psql -U erpadmin -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"

# 检查锁
docker exec -it ai-ready-erp-postgres psql -U erpadmin -c "SELECT * FROM pg_locks;"
```

### Redis故障

#### 1. 连接失败

```bash
# 检查Redis状态
docker exec -it ai-ready-erp-redis redis-cli -a <密码> ping

# 检查内存
docker exec -it ai-ready-erp-redis redis-cli -a <密码> INFO memory
```

#### 2. 性能下降

```bash
# 检查慢日志
docker exec -it ai-ready-erp-redis redis-cli -a <密码> SLOWLOG GET 10

# 检查大键
docker exec -it ai-ready-erp-redis redis-cli -a <密码> --bigkeys
```

## 安全管理

### 访问控制

#### 1. 网络隔离

```bash
# 检查网络配置
docker network inspect erp-network

# 限制外部访问
# 仅允许内部网络访问数据库和Redis
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
grep "登录" /var/log/ai-ready/erp/*/app.log

# 检查权限变更
grep "权限" /var/log/ai-ready/erp/*/app.log
```

#### 2. 安全扫描

```bash
# 容器安全扫描
docker scan ai-ready/erp-batch-sn:latest

# 依赖安全扫描
mvn dependency-check:check
```

## 备份和恢复

### 数据备份

#### 1. PostgreSQL备份

```bash
# 全量备份
docker exec -it ai-ready-erp-postgres pg_dump -U erpadmin ai_ready_erp > backup_$(date +%Y%m%d).sql

# 增量备份
docker exec -it ai-ready-erp-postgres pg_dump -U erpadmin --format=custom ai_ready_erp > backup_$(date +%Y%m%d).dump
```

#### 2. Redis备份

```bash
# RDB备份
docker exec -it ai-ready-erp-redis redis-cli -a <密码> BGSAVE

# AOF备份
docker cp ai-ready-erp-redis:/data/appendonly.aof backup_$(date +%Y%m%d).aof
```

#### 3. 应用配置备份

```bash
# 备份配置文件
tar -czf config_backup_$(date +%Y%m%d).tar.gz I:\AI-Ready\deploy\erp-modules\*.yml I:\AI-Ready\deploy\erp-modules\.env*
```

### 数据恢复

#### 1. PostgreSQL恢复

```bash
# 恢复SQL备份
docker exec -i ai-ready-erp-postgres psql -U erpadmin ai_ready_erp < backup_20260429.sql

# 恢复自定义格式备份
docker exec -i ai-ready-erp-postgres pg_restore -U erpadmin -d ai_ready_erp backup_20260429.dump
```

#### 2. Redis恢复

```bash
# 恢复RDB备份
docker cp backup_20260429.rdb ai-ready-erp-redis:/data/dump.rdb
docker restart ai-ready-erp-redis

# 恢复AOF备份
docker cp backup_20260429.aof ai-ready-erp-redis:/data/appendonly.aof
docker restart ai-ready-erp-redis
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

### Grafana监控

访问: http://localhost:3000

关键仪表板:
- ERP系统总览
- 业务指标监控
- 数据库性能
- Redis性能
- JVM性能

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
- psql: PostgreSQL客户端
- redis-cli: Redis客户端

### 常用命令速查

```bash
# 服务管理
docker-compose ps                # 查看服务状态
docker-compose restart <service> # 重启服务
docker-compose down              # 停止所有服务
docker-compose up -d             # 启动所有服务

# 日志查看
docker logs <container>          # 查看容器日志
tail -f /var/log/ai-ready/erp/*/app.log  # 实时查看应用日志

# 监控访问
curl http://localhost:9090       # Prometheus
curl http://localhost:3000       # Grafana

# 数据库操作
docker exec -it ai-ready-erp-postgres psql -U erpadmin  # PostgreSQL客户端
docker exec -it ai-ready-erp-redis redis-cli -a <密码>  # Redis客户端

# 备份恢复
pg_dump -U erpadmin ai_ready_erp > backup.sql  # PostgreSQL备份
psql -U erpadmin ai_ready_erp < backup.sql     # PostgreSQL恢复
```

### 紧急联系方式

- 运维负责人: devops-engineer
- 技术支持: coordinator
- 系统管理员: main