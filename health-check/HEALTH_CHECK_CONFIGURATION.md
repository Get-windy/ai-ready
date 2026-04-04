# AI-Ready 健康检查配置文档

## 📋 文档概述

| 项目 | 内容 |
|------|------|
| 项目名称 | AI-Ready (智企连) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-04-01 |
| 创建者 | devops-engineer |
| 文档目的 | 配置系统健康检查，保障服务可用性 |

---

## 1️⃣ 应用健康检查端点配置

### 1.1 Spring Boot Actuator 配置

**配置文件位置**: `I:\AI-Ready\core-api\src\main\resources\application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  tracing:
    enabled: true
```

### 1.2 健康检查端点列表

| 端点 | URL | 功能 |
|------|-----|------|
| 总体健康状态 | `/actuator/health` | 综合健康检查结果 |
| 存活探针 | `/actuator/health/liveness` | Kubernetes 存活检查 |
| 就绪探针 | `/actuator/health/readiness` | Kubernetes 就绪检查 |
| 应用信息 | `/actuator/info` | 应用版本、启动时间等 |
| 性能指标 | `/actuator/metrics` | JVM、HTTP 等指标 |
| Prometheus | `/actuator/prometheus` | Prometheus 格式指标 |

### 1.3 自定义健康指示器

**代码位置**: `I:\AI-Ready\core-base\src\main\java\cn\aiedge\base\config\health\`

| 文件 | 检查内容 |
|------|----------|
| `DatabaseHealthIndicator.java` | PostgreSQL 连接状态、响应时间 |
| `RedisHealthIndicator.java` | Redis 连接、内存使用、客户端连接数 |
| `ApplicationHealthIndicator.java` | JVM 内存、CPU负载、线程数 |

---

## 2️⃣ 数据库连接健康检查

### 2.1 DatabaseHealthIndicator 配置

**检查项**:
- 数据库连接有效性 (Connection.isValid)
- 查询响应时间 (SELECT 1)
- 连接池状态 (HikariPool)

**健康状态判断**:

| 状态 | 条件 |
|------|------|
| UP | 连接有效，响应时间 < 100ms |
| UP (Warning) | 连接有效，响应时间 > 100ms |
| DOWN | 连接失败或无效 |

### 2.2 数据库连接池配置

**配置文件**: `application.yml`

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      pool-name: AiReadyHikariPool
      max-lifetime: 1800000
      connection-timeout: 30000
```

---

## 3️⃣ 缓存服务健康检查

### 3.1 RedisHealthIndicator 配置

**检查项**:
- PING 命令响应
- 内存使用量 (used_memory_human)
- 最大内存 (maxmemory_human)
- 内存使用比例
- 连接客户端数 (connected_clients)
- 阻塞客户端数 (blocked_clients)
- Redis 版本

**健康状态判断**:

| 状态 | 条件 |
|------|------|
| UP | PING 返回 PONG，响应 < 50ms |
| UP (Warning) | 内存使用 > 80% 或响应时间 > 50ms |
| DOWN | PING 失败或连接异常 |

### 3.2 Redis 配置

**配置文件**: `application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
```

---

## 4️⃣ 容器健康检查探针配置

### 4.1 Docker Compose 配置

**配置文件**: `I:\AI-Ready\docker-compose.yml`

```yaml
services:
  api:
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", 
             "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
  
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U devuser -d devdb"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
  
  redis:
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### 4.2 Kubernetes 探针配置

**开发环境**: `I:\AI-Ready\k8s\dev\api-deployment.yaml`

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

**生产环境**: `I:\AI-Ready\k8s\prod\api-deployment.yaml`

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 120
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 60
```

### 4.3 探针参数说明

| 参数 | 说明 | Dev | Prod |
|------|------|-----|------|
| initialDelaySeconds | 启动后等待时间 | 60s | 120s |
| periodSeconds | 检查间隔 | 10s | 10s |
| timeoutSeconds | 单次超时 | 5s | 5s |
| failureThreshold | 失败阈值 | 3 | 3 |

---

## 5️⃣ 健康检查使用指南

### 5.1 手动检查命令

**API 服务健康检查**:
```bash
curl http://localhost:8080/actuator/health
```

**数据库健康检查**:
```bash
curl http://localhost:8080/actuator/health | jq '.components.db'
```

**Redis 健康检查**:
```bash
curl http://localhost:8080/actuator/health | jq '.components.redis'
```

**应用健康检查**:
```bash
curl http://localhost:8080/actuator/health | jq '.components.application'
```

### 5.2 PowerShell 健康检查脚本

**脚本位置**: `I:\AI-Ready\health-check\health-check.ps1`

```powershell
# 执行完整健康检查
.\health-check.ps1

# 详细输出模式
.\health-check.ps1 -Verbose $true
```

### 5.3 健康检查响应示例

**正常状态响应**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "connection": "active",
        "queryTimeMs": 5,
        "fromPool": true
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "redis": "connected",
        "pingTimeMs": 2,
        "usedMemory": "50MB",
        "connectedClients": "5",
        "version": "7.0.0"
      }
    },
    "application": {
      "status": "UP",
      "details": {
        "application": "ai-ready",
        "profile": "dev",
        "heapUsagePercent": "45.00%",
        "activeThreadCount": 25
      }
    }
  }
}
```

**异常状态响应**:
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused",
        "database": "PostgreSQL"
      }
    }
  }
}
```

---

## 6️⃣ 监控集成

### 6.1 Prometheus 配置

**Prometheus 抓取配置**:
```yaml
scrape_configs:
  - job_name: 'ai-ready'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 6.2 健康检查告警规则

**Prometheus 告警规则示例**:
```yaml
groups:
  - name: ai-ready-health
    rules:
      - alert: ServiceDown
        expr: up{job="ai-ready"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "AI-Ready service is down"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap memory usage > 80%"
```

---

## 7️⃣ 文件清单

| 文件路径 | 类型 | 说明 |
|----------|------|------|
| `core-base/src/main/java/cn/aiedge/base/config/health/DatabaseHealthIndicator.java` | Java | 数据库健康指示器 |
| `core-base/src/main/java/cn/aiedge/base/config/health/RedisHealthIndicator.java` | Java | Redis健康指示器 |
| `core-base/src/main/java/cn/aiedge/base/config/health/ApplicationHealthIndicator.java` | Java | 应用健康指示器 |
| `core-base/src/main/java/cn/aiedge/base/config/health/HealthCheckConfig.java` | Java | 健康检查配置类 |
| `core-api/src/main/resources/application.yml` | YAML | Actuator配置 |
| `docker-compose.yml` | YAML | Docker健康检查探针 |
| `k8s/dev/api-deployment.yaml` | YAML | K8s开发环境探针 |
| `k8s/prod/api-deployment.yaml` | YAML | K8s生产环境探针 |
| `health-check/health-check.ps1` | PowerShell | 健康检查脚本 |
| `health-check/health-check-config.json` | JSON | 健康检查配置 |
| `health-check/HEALTH_CHECK_CONFIGURATION.md` | Markdown | 本配置文档 |

---

## 8️⃣ 验证清单

- [x] Spring Boot Actuator 依赖已配置
- [x] 健康检查端点已暴露 (health/info/metrics/prometheus)
- [x] 自定义 DatabaseHealthIndicator 已创建
- [x] 自定义 RedisHealthIndicator 已创建
- [x] 自定义 ApplicationHealthIndicator 已创建
- [x] Docker Compose 健康探针已配置
- [x] K8s 开发环境探针已配置
- [x] K8s 生产环境探针已配置
- [x] 健康检查 PowerShell 脚本已存在
- [x] 配置文档已输出

---

**文档完成日期**: 2026-04-01  
**作者**: devops-engineer (运维工程师)  
**审核状态**: 待审核