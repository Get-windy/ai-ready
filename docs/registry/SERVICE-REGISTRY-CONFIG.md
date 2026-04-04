# AI-Ready 服务注册中心配置文档

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-30  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [Nacos 配置](#nacos-配置)
4. [Consul 配置](#consul-配置)
5. [健康检查配置](#健康检查配置)
6. [部署指南](#部署指南)
7. [运维手册](#运维手册)

---

## 概述

### 服务注册中心选型

| 方案 | 状态 | 适用场景 | 特点 |
|------|------|----------|------|
| Nacos | 主选 | Spring Cloud Alibaba 生态 | 配置中心+注册中心一体化 |
| Consul | 备选 | 多语言微服务 | 服务网格支持、KV存储 |

### 功能特性

- **服务注册**: 服务启动自动注册到注册中心
- **服务发现**: 客户端通过注册中心获取服务实例
- **健康检查**: 自动检测服务健康状态
- **负载均衡**: 多实例自动负载分配
- **配置管理**: 动态配置更新（Nacos）
- **元数据管理**: 服务标签和元信息管理

---

## 架构设计

### 服务注册流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  服务实例   │────▶│  注册中心   │────▶│  服务消费者 │
│  (Provider) │     │  (Nacos)    │     │  (Consumer) │
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │
      │                   │                   │
      ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 心跳上报    │     │ 服务列表    │     │ 服务调用    │
│ 元数据注册  │     │ 健康检查    │     │ 负载均衡    │
│ 健康检查    │     │ 配置推送    │     │ 故障转移    │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 服务实例列表

| 服务名 | 端口 | 健康检查路径 | 元数据 |
|--------|------|--------------|--------|
| ai-ready-api | 8080 | /actuator/health | module:api |
| ai-ready-agent | 8081 | /actuator/health | module:agent |
| ai-ready-crm | 8082 | /actuator/health | module:crm |
| ai-ready-erp | 8083 | /actuator/health | module:erp |

---

## Nacos 配置

### 配置文件位置

```
I:\AI-Ready\configs\registry\nacos\
├── application.yml          # 主配置文件
├── nacos-config-dev.yaml    # 开发环境配置
├── nacos-config-staging.yaml # 预发布配置
├── nacos-config-prod.yaml   # 生产配置
```

### 核心配置项

#### application.yml

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:ai-ready}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        heart-beat-interval: 5000    # 心跳间隔
        heart-beat-timeout: 15000    # 心跳超时
        healthy-threshold: 2         # 健康阈值
        unhealthy-threshold: 3       # 不健康阈值
```

### 服务元数据配置

```yaml
metadata:
  version: ${APP_VERSION:1.0.0-SNAPSHOT}
  region: ${REGION:cn-east}
  zone: ${ZONE:zone-a}
  env: ${SPRING_PROFILES_ACTIVE:dev}
  
  # 业务元数据
  business.module: api
  business.owner: ai-ready-team
  business.monitor: enabled
```

### 命名空间配置

| 环境 | 命名空间 ID | Group | 说明 |
|------|-------------|-------|------|
| dev | ai-ready-dev | DEFAULT_GROUP | 开发环境 |
| staging | ai-ready-staging | DEFAULT_GROUP | 预发布环境 |
| prod | ai-ready-prod | DEFAULT_GROUP | 生产环境 |

---

## Consul 配置

### 配置文件位置

```
I:\AI-Ready\configs\registry\consul\
└── application-consul.yml
```

### 核心配置项

```yaml
spring:
  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-critical-timeout: 30s
        tags:
          - version=1.0.0
          - env=dev
```

### Consul 特有功能

1. **服务网格**: 支持 Consul Connect 服务网格
2. **KV 存储**: 用于配置存储
3. **多数据中心**: 跨数据中心服务发现
4. **ACL 管理**: 访问控制列表

---

## 健康检查配置

### 配置文件位置

```
I:\AI-Ready\configs\registry\health\health-check-config.yml
```

### 健康检查类型

| 类型 | 适用场景 | 配置示例 |
|------|----------|----------|
| HTTP | Web服务 | `/actuator/health` |
| TCP | 数据库/Redis | `port: 5432` |
| Script | 自定义检查 | 自定义脚本 |

### Kubernetes 健康检查

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,nacos-discovery,metrics
  endpoint:
    health:
      show-details: always
```

---

## 部署指南

### Docker 部署

#### Nacos Docker Compose

```bash
# 启动 Nacos
cd I:\AI-Ready\docker\registry
docker-compose -f nacos-docker-compose.yml up -d

# 验证启动
curl http://localhost:8848/nacos/v1/console/health/liveness
```

#### 配置文件

```
I:\AI-Ready\docker\registry\nacos-docker-compose.yml
```

包含服务：
- Nacos Server (8848)
- MySQL (3306)
- Prometheus (9090)
- Grafana (3000)

### Kubernetes 部署

#### Nacos K8s 部署

```yaml
# 应用部署配置
kubectl apply -f I:\AI-Ready\k8s\nacos\nacos-deployment.yaml
```

### 环境变量配置

| 变量 | 开发环境 | 预发布 | 生产 |
|------|----------|--------|------|
| NACOS_SERVER_ADDR | localhost:8848 | nacos-staging:8848 | nacos-prod:8848 |
| NACOS_NAMESPACE | ai-ready-dev | ai-ready-staging | ai-ready-prod |
| NACOS_GROUP | DEFAULT_GROUP | DEFAULT_GROUP | DEFAULT_GROUP |

---

## 运维手册

### 服务注册验证

```bash
# 查看已注册服务
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=ai-ready-api"

# 查看服务详情
curl "http://localhost:8848/nacos/v1/ns/instance?serviceName=ai-ready-api&ip=192.168.1.1&port=8080"
```

### 健康检查验证

```bash
# 检查服务健康状态
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=ai-ready-api&healthyOnly=true"

# Actuator 健康检查
curl "http://localhost:8080/actuator/health"
```

### 服务下线

```bash
# 手动下线服务实例
curl -X DELETE "http://localhost:8848/nacos/v1/ns/instance?serviceName=ai-ready-api&ip=192.168.1.1&port=8080"
```

### 配置管理

```bash
# 发布配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=ai-ready-api.yaml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=@nacos-config-prod.yaml"

# 获取配置
curl "http://localhost:8848/nacos/v1/cs/configs?dataId=ai-ready-api.yaml&group=DEFAULT_GROUP"
```

### 故障排查

#### 服务无法注册

1. 检查 Nacos 服务是否启动
2. 检查网络连接
3. 检查配置是否正确
4. 检查日志输出

```bash
# 查看服务日志
kubectl logs -n ai-ready deployment/ai-ready-api

# 查看 Nacos 日志
docker logs ai-ready-nacos
```

#### 服务频繁下线

1. 检查心跳配置
2. 检查健康检查路径
3. 检查服务资源（内存/CPU）
4. 检查网络稳定性

---

## 附录

### 配置文件清单

| 文件 | 位置 | 用途 |
|------|------|------|
| application.yml | configs/registry/nacos/ | Nacos 主配置 |
| nacos-config-dev.yaml | configs/registry/nacos/ | 开发环境配置 |
| nacos-config-staging.yaml | configs/registry/nacos/ | 预发布配置 |
| nacos-config-prod.yaml | configs/registry/nacos/ | 生产配置 |
| application-consul.yml | configs/registry/consul/ | Consul 配置 |
| health-check-config.yml | configs/registry/health/ | 健康检查配置 |
| nacos-docker-compose.yml | docker/registry/ | Docker 部署 |

### 常用命令

```bash
# Nacos 控制台
http://localhost:8848/nacos
用户名: nacos
密码: nacos

# Consul 控制台
http://localhost:8500/ui
```

### 监控指标

| 指标 | 描述 | 告警阈值 |
|------|------|----------|
| nacos_service_count | 注册服务数 | < 1 |
| nacos_healthy_instance | 健康实例数 | < 预期值 |
| nacos_heartbeat_loss | 心跳丢失率 | > 5% |

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-30*