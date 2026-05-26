# 测试环境环境变量配置说明

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: devops-engineer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [环境变量分类](#环境变量分类)
3. [数据库配置](#数据库配置)
4. [缓存配置](#缓存配置)
5. [消息队列配置](#消息队列配置)
6. [监控配置](#监控配置)
7. [应用配置](#应用配置)
8. [安全配置](#安全配置)
9. [配置示例](#配置示例)

---

## 概述

### 环境变量作用

测试环境的环境变量配置管理是确保服务正确运行的关键。所有敏感信息和配置参数都通过环境变量注入，避免硬编码在配置文件中。

### 配置原则

- **安全性**: 敏感信息使用环境变量，不写入配置文件
- **可配置性**: 所有 configurable 参数通过环境变量控制
- **一致性**: 生产/测试/开发环境变量命名保持一致
- **文档化**: 每个环境变量都有详细说明

---

## 环境变量分类

| 分类 | 变量前缀 | 说明 |
|------|---------|------|
| PostgreSQL | POSTGRES_* | 数据库连接和初始化 |
| Redis | REDIS_* | Redis连接和配置 |
| RabbitMQ | RABBITMQ_* | RabbitMQ连接和配置 |
| Prometheus | PROMETHEUS_* | Prometheus配置 |
| Grafana | GF_* | Grafana配置 |
| 自定义导出器 | EXPORTER_* | 业务指标导出 |
| API服务 | SPRING_* | Spring Boot配置 |
| Nginx | NGINX_* | Nginx配置 |

---

## 数据库配置

### PostgreSQL 环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| POSTGRES_DB | 是 | monitoring_db | 数据库名称 |
| POSTGRES_USER | 是 | monitoring_user | 数据库用户名 |
| POSTGRES_PASSWORD | 是 | monitoring_pass_123 | 数据库密码 |
| POSTGRES_INITDB_ARGS | 否 | --encoding=UTF8 --locale=C | 初始化参数 |

**配置位置**: `monitoring-alerting-deployment.yml` services.postgres-monitoring.environment

**使用示例**:
```bash
POSTGRES_DB=monitoring_db
POSTGRES_USER=monitoring_user
POSTGRES_PASSWORD=monitoring_pass_123
POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C"
```

### PostgreSQL 连接字符串

**格式**: `postgresql://[user]:[password]@[host]:[port]/[database]`

**示例**:
```
postgresql://monitoring_user:monitoring_pass_123@postgres-monitoring:5432/monitoring_db
```

---

## 缓存配置

### Redis 环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| REDIS_PASSWORD | 是 | redis_monitoring_pass_123 | Redis密码 |
| REDIS_MAXMEMORY | 否 | 256mb | 最大内存限制 |
| REDIS_MAXMEMORY_POLICY | 否 | allkeys-lru | 内存淘汰策略 |

**配置位置**: `monitoring-alerting-deployment.yml` services.redis-monitoring.command

**使用示例**:
```bash
# Redis连接字符串
redis://:redis_monitoring_pass_123@redis-monitoring:6379
```

---

## 消息队列配置

### RabbitMQ 环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| RABBITMQ_DEFAULT_USER | 是 | monitoring_rabbit | 默认用户名 |
| RABBITMQ_DEFAULT_PASS | 是 | rabbit_monitoring_pass_123 | 默认密码 |
| RABBITMQ_DEFAULT_VHOST | 是 | monitoring_vhost | 默认虚拟主机 |

**配置位置**: `monitoring-alerting-deployment.yml` services.rabbitmq-monitoring.environment

** 사용示例**:
```bash
RABBITMQ_DEFAULT_USER=monitoring_rabbit
RABBITMQ_DEFAULT_PASS=rabbit_monitoring_pass_123
RABBITMQ_DEFAULT_VHOST=monitoring_vhost
```

### RabbitMQ 连接字符串

**格式**: `amqp://[user]:[password]@[host]:[port]/[vhost]`

**示例**:
```
amqp://monitoring_rabbit:rabbit_monitoring_pass_123@rabbitmq-monitoring:5672/monitoring_vhost
```

---

## 监控配置

### Prometheus 环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| STORAGE_TSDB_RETENTION_TIME | 否 | 30d | 数据保留时间 |
| STORAGE_TSDB_RETENTION_SIZE | 否 | 10GB | 数据保留大小 |
| WEB_ENABLE_ADMIN_API | 否 | true | 启用管理API |

**配置位置**: `monitoring-alerting-deployment.yml` services.prometheus.command

### AlertManager 环境变量

AlertManager 主要通过配置文件管理，无需环境变量。

---

## 应用配置

### 自定义导出器环境变量

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| POSTGRES_CONNECTION_STRING | 是 | - | PostgreSQL连接字符串 |
| REDIS_CONNECTION_STRING | 是 | - | Redis连接字符串 |
| RABBITMQ_CONNECTION_STRING | 是 | - | RabbitMQ连接字符串 |
| EXPORTER_PORT | 否 | 9101 | 导出器监听端口 |
| METRICS_PATH | 否 | /metrics | 指标路径 |
| COLLECT_INTERVAL | 否 | 30 | 采集间隔（秒） |

**配置位置**: `monitoring-alerting-deployment.yml` services.custom-exporter.environment

**使用示例**:
```bash
POSTGRES_CONNECTION_STRING=postgresql://monitoring_user:monitoring_pass_123@postgres-monitoring:5432/monitoring_db
REDIS_CONNECTION_STRING=redis://:redis_monitoring_pass_123@redis-monitoring:6379
RABBITMQ_CONNECTION_STRING=amqp://monitoring_rabbit:rabbit_monitoring_pass_123@rabbitmq-monitoring:5672/monitoring_vhost
EXPORTER_PORT=9101
METRICS_PATH=/metrics
COLLECT_INTERVAL=30
```

### API服务环境变量 (Spring Boot)

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| SPRING_DATASOURCE_URL | 是 | - | 数据库连接URL |
| SPRING_DATASOURCE_USERNAME | 是 | - | 数据库用户名 |
| SPRING_DATASOURCE_PASSWORD | 是 | - | 数据库密码 |
| SPRING_REDIS_HOST | 是 | - | Redis主机 |
| SPRING_REDIS_PORT | 是 | - | Redis端口 |
| SPRING_REDIS_PASSWORD | 是 | - | Redis密码 |
| SPRING_RABBITMQ_HOST | 是 | - | RabbitMQ主机 |
| SPRING_RABBITMQ_PORT | 是 | - | RabbitMQ端口 |
| SPRING_RABBITMQ_USERNAME | 是 | - | RabbitMQ用户名 |
| SPRING_RABBITMQ_PASSWORD | 是 | - | RabbitMQ密码 |
| SPRING_RABBITMQ_VIRTUAL_HOST | 是 | - | RabbitMQ虚拟主机 |
| PROMETHEUS_METRICS_ENABLED | 否 | true | 启用Prometheus指标 |
| MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE | 否 | health,info,metrics,prometheus | 暴露的端点 |

**配置位置**: `monitoring-alerting-deployment.yml` services.monitoring-api.environment

**使用示例**:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-monitoring:5432/monitoring_db
SPRING_DATASOURCE_USERNAME=monitoring_user
SPRING_DATASOURCE_PASSWORD=monitoring_pass_123
SPRING_REDIS_HOST=redis-monitoring
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=redis_monitoring_pass_123
SPRING_RABBITMQ_HOST=rabbitmq-monitoring
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=monitoring_rabbit
SPRING_RABBITMQ_PASSWORD=rabbit_monitoring_pass_123
SPRING_RABBITMQ_VIRTUAL_HOST=monitoring_vhost
PROMETHEUS_METRICS_ENABLED=true
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

---

## 安全配置

### Docker Compose 安全配置

1. **非root用户运行**
   - Prometheus: `user: "65534:65534"`
   - Grafana: `user: "472:472"`
   - AlertManager: `user: "65534:65534"`

2. **环境变量加密**
   - 使用Docker secrets或外部密钥管理
   - 避免在配置文件中硬编码密码

3. **网络隔离**
   - 使用独立的Docker网络
   - 限制外部访问

###敏感信息管理

| 敏感信息 | 安全存储方式 |
|---------|-------------|
| 数据库密码 | Docker secrets |
| Redis密码 | Docker secrets |
| RabbitMQ密码 | Docker secrets |
| Grafana密码 | 启动时设置 |

---

## 配置示例

### 完整环境变量配置

```yaml
# monitoring-alerting-deployment.yml
services:
  postgres-monitoring:
    environment:
      POSTGRES_DB: monitoring_db
      POSTGRES_USER: monitoring_user
      POSTGRES_PASSWORD: monitoring_pass_123
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"
  
  redis-monitoring:
    command: redis-server --requirepass redis_monitoring_pass_123 --maxmemory 256mb --maxmemory-policy allkeys-lru
  
  rabbitmq-monitoring:
    environment:
      RABBITMQ_DEFAULT_USER: monitoring_rabbit
      RABBITMQ_DEFAULT_PASS: rabbit_monitoring_pass_123
      RABBITMQ_DEFAULT_VHOST: monitoring_vhost
  
  custom-exporter:
    environment:
      POSTGRES_CONNECTION_STRING: "postgresql://monitoring_user:monitoring_pass_123@postgres-monitoring:5432/monitoring_db"
      REDIS_CONNECTION_STRING: "redis://:redis_monitoring_pass_123@redis-monitoring:6379"
      RABBITMQ_CONNECTION_STRING: "amqp://monitoring_rabbit:rabbit_monitoring_pass_123@rabbitmq-monitoring:5672/monitoring_vhost"
      EXPORTER_PORT: 9101
      METRICS_PATH: /metrics
      COLLECT_INTERVAL: 30
  
  monitoring-api:
    environment:
      SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-monitoring:5432/monitoring_db"
      SPRING_DATASOURCE_USERNAME: "monitoring_user"
      SPRING_DATASOURCE_PASSWORD: "monitoring_pass_123"
      SPRING_REDIS_HOST: "redis-monitoring"
      SPRING_REDIS_PORT: "6379"
      SPRING_REDIS_PASSWORD: "redis_monitoring_pass_123"
      SPRING_RABBITMQ_HOST: "rabbitmq-monitoring"
      SPRING_RABBITMQ_PORT: "5672"
      SPRING_RABBITMQ_USERNAME: "monitoring_rabbit"
      SPRING_RABBITMQ_PASSWORD: "rabbit_monitoring_pass_123"
      SPRING_RABBITMQ_VIRTUAL_HOST: "monitoring_vhost"
      PROMETHEUS_METRICS_ENABLED: "true"
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,metrics,prometheus"
```

### 环境变量验证

```bash
# 检查环境变量是否正确设置
docker exec postgres-monitoring env | grep POSTGRES
docker exec redis-monitoring env | grep REDIS
docker exec rabbitmq-monitoring env | grep RABBITMQ
docker exec custom-exporter env | grep -E "POSTGRES|REDIS|RABBITMQ"
docker exec monitoring-api env | grep SPRING
```

---

## 故障排查

### 常见配置问题

1. **连接超时**
   - 检查环境变量是否正确设置
   - 验证服务名称和端口
   - 检查网络配置

2. **认证失败**
   - 验证用户名和密码
   - 检查虚拟主机配置
   - 确认防火墙设置

3. **配置不生效**
   - 重启容器应用新配置
   - 检查配置文件语法
   - 验证环境变量优先级

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|-------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |

---

## 参考资料

- [Docker Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [PostgreSQL Environment Variables](https://hub.docker.com/_/postgres)
- [Redis Configuration](https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/)
- [RabbitMQ Environment Variables](https://hub.docker.com/_/rabbitmq)