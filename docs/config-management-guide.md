# AI-Ready 服务配置管理设计文档

**项目**: 智企连·AI-Ready  
**版本**: v1.0  
**日期**: 2026-04-03  
**负责人**: devops-engineer

---

## 一、配置中心架构

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      配置中心架构                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐     ┌──────────────────┐                 │
│  │  Git Repository  │────▶│  Config Server   │                 │
│  │  (配置版本控制)   │     │  (Spring Cloud   │                 │
│  │                  │◀────│   Config Server) │                 │
│  └──────────────────┘     └────────┬─────────┘                 │
│                                    │                            │
│                           ┌────────┴────────┐                   │
│                           ▼                 ▼                   │
│                    ┌──────────┐      ┌──────────┐              │
│                    │ 服务实例 │      │ 服务实例 │              │
│                    │ (pull)   │      │ (pull)   │              │
│                    └──────────┘      └──────────┘              │
│                                                                 │
│  可选增强：                                                      │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐               │
│  │ Nacos    │ or │ Apollo   │ or │ Consul   │               │
│  │(配置+注册)│    │(配置中心) │    │(服务网格) │               │
│  └──────────┘     └──────────┘     └──────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 技术选型

| 方案 | 优点 | 缺点 | 推荐场景 |
|------|------|------|----------|
| **Spring Cloud Config** | Spring原生、Git版本控制 | 需要Git webhook刷新 | Spring Boot项目 |
| **Nacos** | 配置+注册中心、动态刷新 | 运维成本中等 | 微服务架构 |
| **Apollo** | 功能丰富、权限管理 | 部署复杂 | 大型企业 |
| **Kubernetes ConfigMap** | 云原生、无需额外组件 | 功能简单 | K8s环境 |

**推荐方案**: Spring Cloud Config + Git (当前阶段) / Nacos (未来演进)

---

## 二、配置项分类

### 2.1 配置层级

```
配置层级：
├── 全局配置 (application.yml)        # 所有环境共享
├── 环境配置 (application-{env}.yml)  # 环境特定
│   ├── application-dev.yml
│   ├── application-staging.yml
│   └── application-prod.yml
├── 服务配置 (application-{service}.yml)  # 服务特定
│   ├── application-core-api.yml
│   └── application-web.yml
└── 敏感配置 (Vault/K8s Secret)       # 加密存储
```

### 2.2 配置项清单

| 分类 | 配置项 | 环境隔离 | 敏感度 |
|------|--------|----------|--------|
| **数据库** | datasource.url | 是 | 中 |
| | datasource.username | 是 | 高 |
| | datasource.password | 是 | 高 |
| **Redis** | redis.host | 是 | 中 |
| | redis.password | 是 | 高 |
| **AI引擎** | ai.engine.endpoint | 是 | 中 |
| | ai.api.key | 是 | 高 |
| **消息队列** | kafka.bootstrap-servers | 是 | 中 |
| | kafka.sasl.password | 是 | 高 |
| **监控** | prometheus.endpoint | 是 | 低 |
| | grafana.api-key | 是 | 高 |
| **外部服务** | dingtalk.webhook | 是 | 高 |
| | sms.api-key | 是 | 高 |

---

## 三、环境隔离配置

### 3.1 环境定义

| 环境 | Profile | 配置文件 | 数据库 | 域名 |
|------|---------|----------|--------|------|
| 开发 | dev | application-dev.yml | ai_ready_dev | dev.ai-ready.cn |
| 测试 | staging | application-staging.yml | ai_ready_staging | staging.ai-ready.cn |
| 生产 | prod | application-prod.yml | ai_ready_prod | api.ai-ready.cn |

### 3.2 环境配置文件

**application-dev.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://dev-db:5432/ai_ready_dev
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: dev-redis
    password: ${REDIS_PASSWORD}
  ai:
    engine:
      endpoint: http://dev-ai-engine:8080

logging:
  level:
    root: DEBUG
    com.ai.ready: TRACE
```

**application-prod.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://prod-db-master:5432/ai_ready_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
  redis:
    host: prod-redis-cluster
    password: ${REDIS_PASSWORD}
    cluster:
      nodes: redis-node1:6379,redis-node2:6379,redis-node3:6379

logging:
  level:
    root: WARN
    com.ai.ready: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 四、配置版本管理

### 4.1 Git仓库结构

```
ai-ready-config/
├── application.yml                    # 全局配置
├── application-dev.yml                # 开发环境
├── application-staging.yml            # 测试环境
├── application-prod.yml               # 生产环境
├── services/
│   ├── core-api/
│   │   ├── application.yml
│   │   └── application-prod.yml
│   └── web/
│       └── application.yml
└── scripts/
    ├── encrypt.sh                     # 加密脚本
    └── decrypt.sh                     # 解密脚本
```

### 4.2 版本控制策略

```
分支策略：
├── main           # 生产环境配置
├── staging        # 测试环境配置
├── develop        # 开发环境配置
└── feature/*      # 功能配置变更

标签策略：
├── v1.0.0-prod    # 生产版本
├── v1.0.0-staging # 测试版本
└── v1.0.0-dev     # 开发版本
```

### 4.3 配置变更流程

```
配置变更流程：

1. 开发环境变更
   └─▶ 提交到 develop 分支
       └─▶ 自动同步到 Config Server
           └─▶ 服务实例自动刷新 (可选)

2. 生产环境变更
   └─▶ 提交 PR 到 main 分支
       └─▶ 代码审查 + 审批
           └─▶ 合并后触发配置同步
               └─▶ 通知相关服务刷新

3. 敏感配置变更
   └─▶ 加密后存储到 Git/Vault
       └─▶ 通过 K8s Secret 分发
           └─▶ 服务重启加载新配置
```

---

## 五、敏感配置管理

### 5.1 加密方案

**方案1: Jasypt加密**

```yaml
# application.yml
spring:
  datasource:
    password: ENC(加密后的密码)

# 启动参数
java -jar app.jar -Djasypt.encryptor.password=${JASYPT_PASSWORD}
```

**方案2: Kubernetes Secret**

```yaml
# secret.yml
apiVersion: v1
kind: Secret
metadata:
  name: ai-ready-secrets
type: Opaque
stringData:
  DB_PASSWORD: "your_password"
  REDIS_PASSWORD: "your_password"
  AI_API_KEY: "your_api_key"
```

**方案3: HashiCorp Vault**

```yaml
# application.yml
spring:
  cloud:
    vault:
      uri: https://vault.ai-ready.cn
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        application-name: ai-ready
```

### 5.2 敏感配置清单

| 配置项 | 存储方式 | 轮换周期 |
|--------|----------|----------|
| 数据库密码 | K8s Secret / Vault | 90天 |
| Redis密码 | K8s Secret / Vault | 90天 |
| AI API Key | Vault | 30天 |
| JWT密钥 | Vault | 365天 |
| 加密密钥 | Vault | 永不 (需特殊变更) |

---

## 六、配置刷新机制

### 6.1 刷新方式

| 方式 | 适用场景 | 实现复杂度 |
|------|----------|------------|
| **重启服务** | 所有配置 | 低 |
| **Actuator Refresh** | @RefreshScope配置 | 低 |
| **Config Server Webhook** | 自动刷新 | 中 |
| **Spring Cloud Bus** | 批量刷新 | 中 |

### 6.2 Actuator刷新配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info
        
# 刷新配置
curl -X POST http://localhost:8080/actuator/refresh
```

### 6.3 Webhook自动刷新

```yaml
# Config Server 配置
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/ai-ready/ai-ready-config
          search-paths: services
          refresh-rate: 60  # 60秒检查一次
```

---

## 七、Config Server部署

### 7.1 Docker部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  config-server:
    image: hyness/spring-cloud-config-server:latest
    ports:
      - "8888:8888"
    environment:
      - SPRING_CLOUD_CONFIG_SERVER_GIT_URI=https://github.com/ai-ready/ai-ready-config
      - SPRING_CLOUD_CONFIG_SERVER_GIT_USERNAME=${GIT_USERNAME}
      - SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD=${GIT_PASSWORD}
    volumes:
      - ./config-repo:/config
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 7.2 Kubernetes部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: config-server
  template:
    metadata:
      labels:
        app: config-server
    spec:
      containers:
      - name: config-server
        image: hyness/spring-cloud-config-server:latest
        ports:
        - containerPort: 8888
        env:
        - name: SPRING_CLOUD_CONFIG_SERVER_GIT_URI
          value: https://github.com/ai-ready/ai-ready-config
        - name: SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: git-credentials
              key: password
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8888
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## 八、运维手册

### 8.1 配置查询

```bash
# 查询配置
curl http://config-server:8888/ai-ready/dev
curl http://config-server:8888/ai-ready/prod

# 查询特定配置项
curl http://config-server:8888/ai-ready/prod/main | jq '.propertySources[0].source'
```

### 8.2 配置变更

```bash
# 1. 修改Git配置
git checkout develop
vim application-dev.yml
git commit -am "Update database connection pool"
git push

# 2. 刷新服务配置
curl -X POST http://ai-ready-api:8080/actuator/refresh

# 3. 验证配置生效
curl http://ai-ready-api:8080/actuator/env | jq '.activeProfiles'
```

### 8.3 回滚配置

```bash
# 回滚到上一个版本
git revert HEAD
git push

# 或回滚到指定标签
git checkout v1.0.0-prod
```

---

## 九、文件清单

| 文件路径 | 说明 |
|----------|------|
| `config/application.yml` | 全局配置 |
| `config/application-dev.yml` | 开发环境配置 |
| `config/application-prod.yml` | 生产环境配置 |
| `k8s/secrets/ai-ready-secrets.yml` | 敏感配置Secret |
| `docs/config-management-guide.md` | 本文档 |

---

**文档生成**: devops-engineer  
**版本**: v1.0
