# AI-Ready 容器化部署文档

**版本**: v2.0.0  
**日期**: 2026-04-04  
**作者**: devops-engineer  
**项目**: AI-Ready

---

## 目录

- [一、概述](#一概述)
- [二、Docker配置](#二docker配置)
- [三、Docker Compose部署](#三docker-compose部署)
- [四、Kubernetes部署](#四kubernetes部署)
- [五、CI/CD流水线](#五cicd流水线)
- [六、运维手册](#六运维手册)

---

## 一、概述

### 1.1 容器化架构

```
┌─────────────────────────────────────────────────────────────┐
│                      AI-Ready 容器架构                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Nginx     │  │  Core API   │  │ Core Agent  │         │
│  │   (网关)    │  │  (Spring)   │  │  (Python)   │         │
│  │   :80/443   │  │   :8080     │  │   :8000     │         │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │
│         │                │                │                 │
│         └────────────────┼────────────────┘                 │
│                          │                                  │
│         ┌────────────────┼────────────────┐                 │
│         │                │                │                 │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐         │
│  │ PostgreSQL  │  │    Redis    │  │    Nacos    │         │
│  │   :5432     │  │   :6379     │  │   :8848     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 镜像清单

| 服务 | 镜像 | 大小 | 说明 |
|------|------|------|------|
| Core API | eclipse-temurin:21-jre-alpine | ~200MB | Spring Boot应用 |
| Core Agent | python:3.11-slim | ~150MB | Python服务 |
| PostgreSQL | postgres:14-alpine | ~80MB | 主数据库 |
| Redis | redis:7-alpine | ~30MB | 缓存服务 |
| Nacos | nacos/nacos-server:v2.2.3 | ~300MB | 配置中心 |
| Nginx | nginx:alpine | ~25MB | 反向代理 |

### 1.3 环境要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |
| Kubernetes | 1.25+ | 1.28+ |
| 内存 | 8GB | 16GB |
| CPU | 4核 | 8核 |
| 磁盘 | 50GB | 200GB |

---

## 二、Docker配置

### 2.1 Dockerfile说明

#### Core API Dockerfile

**文件**: `docker/Dockerfile.api`

```dockerfile
# 多阶段构建
# 构建阶段: eclipse-temurin:21-jdk-alpine
# 运行阶段: eclipse-temurin:21-jre-alpine

# 优化特性:
# - 分层JAR解压 (缓存优化)
# - 非root用户 (安全)
# - 健康检查 (可用性)
# - 最小镜像体积 (~200MB)
```

#### Core Agent Dockerfile

**文件**: `docker/Dockerfile.agent`

```dockerfile
# Python服务
# 构建阶段: python:3.11-slim
# 运行阶段: python:3.11-slim

# 优化特性:
# - 虚拟环境隔离
# - 非root用户
# - 健康检查
```

### 2.2 镜像构建

```bash
# 构建Core API镜像
docker build -t ai-ready/api:latest -f docker/Dockerfile.api .

# 构建Core Agent镜像
docker build -t ai-ready/agent:latest -f docker/Dockerfile.agent ./core-agent

# 构建参数
docker build \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VERSION=1.0.0 \
  -t ai-ready/api:1.0.0 \
  -f docker/Dockerfile.api .
```

### 2.3 镜像优化建议

| 优化项 | 方法 | 效果 |
|--------|------|------|
| 基础镜像 | 使用alpine/slim | 减少50%体积 |
| 多阶段构建 | 分离构建和运行环境 | 减少30%体积 |
| 分层缓存 | Spring Boot分层JAR | 加速构建 |
| .dockerignore | 排除不必要文件 | 加速构建 |

---

## 三、Docker Compose部署

### 3.1 快速启动

```bash
# 进入配置目录
cd I:\AI-Ready\docker

# 启动所有服务
docker-compose -f docker-compose.full.yml up -d

# 查看服务状态
docker-compose -f docker-compose.full.yml ps

# 查看日志
docker-compose -f docker-compose.full.yml logs -f api

# 停止服务
docker-compose -f docker-compose.full.yml down
```

### 3.2 环境变量配置

**文件**: `.env`

```env
# API配置
API_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# 数据库配置
POSTGRES_USER=ai_ready
POSTGRES_PASSWORD=ai_ready2026
POSTGRES_DB=ai_ready
POSTGRES_PORT=5432

# Redis配置
REDIS_PORT=6379
REDIS_PASSWORD=

# Nacos配置
NACOS_PORT=8848
NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789

# Nginx配置
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443

# 版本
VERSION=1.0.0
```

### 3.3 服务配置

| 服务 | 端口 | 资源限制 | 说明 |
|------|------|----------|------|
| api | 8080 | CPU:2核 MEM:2GB | 核心API |
| postgres | 5432 | CPU:2核 MEM:2GB | 主数据库 |
| redis | 6379 | CPU:1核 MEM:1GB | 缓存服务 |
| nacos | 8848 | CPU:1核 MEM:1GB | 配置中心 |
| nginx | 80/443 | CPU:0.5核 MEM:256MB | 反向代理 |

### 3.4 数据持久化

```bash
# 数据卷列表
docker volume ls | grep ai-ready

# 备份数据
docker run --rm -v ai-ready_postgres-data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz /data

# 恢复数据
docker run --rm -v ai-ready_postgres-data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /
```

---

## 四、Kubernetes部署

### 4.1 部署命令

```bash
# 创建命名空间
kubectl apply -f k8s/prod/full-deployment.yaml

# 查看部署状态
kubectl get all -n ai-ready-prod

# 查看Pod日志
kubectl logs -f deployment/ai-ready-api -n ai-ready-prod

# 扩容
kubectl scale deployment ai-ready-api --replicas=5 -n ai-ready-prod

# 滚动更新
kubectl set image deployment/ai-ready-api ai-ready-api=ghcr.io/ai-ready/ai-ready-api:v2.0.0 -n ai-ready-prod

# 回滚
kubectl rollout undo deployment/ai-ready-api -n ai-ready-prod
```

### 4.2 配置说明

#### Deployment配置

| 配置项 | 值 | 说明 |
|--------|------|------|
| replicas | 3 | 高可用副本数 |
| maxSurge | 1 | 滚动更新最大增量 |
| maxUnavailable | 0 | 滚动更新最大不可用 |
| terminationGracePeriodSeconds | 60 | 优雅关闭时间 |

#### 资源配置

| 资源 | Request | Limit |
|------|---------|-------|
| CPU | 500m | 1000m |
| Memory | 1Gi | 2Gi |

#### 健康检查

| 检查类型 | 端点 | 初始延迟 | 周期 |
|----------|------|----------|------|
| livenessProbe | /actuator/health/liveness | 120s | 10s |
| readinessProbe | /actuator/health/readiness | 60s | 10s |

#### HPA配置

| 配置项 | 值 |
|--------|------|
| minReplicas | 3 |
| maxReplicas | 10 |
| CPU目标利用率 | 70% |
| Memory目标利用率 | 80% |

### 4.3 服务暴露

```yaml
# Ingress配置
host: api.ai-ready.cn
tls: letsencrypt-prod
annotations:
  nginx.ingress.kubernetes.io/ssl-redirect: "true"
```

---

## 五、CI/CD流水线

### 5.1 GitHub Actions

**文件**: `.github/workflows/container-build.yml`

#### 流水线阶段

```
┌─────────┐    ┌─────────────┐    ┌───────────────┐    ┌──────────┐
│  Build  │ -> │ Docker Build │ -> │ Security Scan │ -> │  Deploy  │
│  Test   │    │   Push       │    │    Trivy      │    │  Dev/Prod│
└─────────┘    └─────────────┘    └───────────────┘    └──────────┘
```

#### 触发条件

| 事件 | 分支/标签 | 操作 |
|------|-----------|------|
| push | main | 构建并推送latest镜像 |
| push | develop | 构建并推送到dev环境 |
| push | v* | 构建并推送到prod环境 |
| pull_request | main | 仅构建测试 |

### 5.2 GitLab CI/CD

**文件**: `.gitlab-ci.yml`

#### 流水线阶段

```yaml
stages:
  - build      # Maven编译
  - test       # 单元测试
  - package    # Docker镜像构建
  - security   # 安全扫描
  - deploy     # 部署到K8s
```

### 5.3 镜像仓库

| 仓库 | 地址 | 用途 |
|------|------|------|
| GitHub Packages | ghcr.io/ai-ready/ai-ready-api | 生产镜像 |
| Docker Hub | docker.io/ai-ready/api | 公开镜像 |
| 阿里云ACR | registry.cn-hangzhou.aliyuncs.com/ai-ready/api | 国内镜像 |

---

## 六、运维手册

### 6.1 日常运维

#### 查看服务状态

```bash
# Docker Compose
docker-compose -f docker-compose.full.yml ps
docker-compose -f docker-compose.full.yml logs -f --tail=100 api

# Kubernetes
kubectl get pods -n ai-ready-prod
kubectl describe pod <pod-name> -n ai-ready-prod
kubectl logs -f <pod-name> -n ai-ready-prod
```

#### 扩缩容

```bash
# Docker Compose
docker-compose -f docker-compose.full.yml up -d --scale api=3

# Kubernetes
kubectl scale deployment ai-ready-api --replicas=5 -n ai-ready-prod
kubectl autoscale deployment ai-ready-api --min=3 --max=10 --cpu-percent=70 -n ai-ready-prod
```

#### 更新部署

```bash
# Docker Compose
docker-compose -f docker-compose.full.yml pull api
docker-compose -f docker-compose.full.yml up -d api

# Kubernetes
kubectl set image deployment/ai-ready-api ai-ready-api=ghcr.io/ai-ready/ai-ready-api:v2.0.0 -n ai-ready-prod
kubectl rollout status deployment/ai-ready-api -n ai-ready-prod
```

### 6.2 故障排查

#### 容器无法启动

```bash
# 1. 查看容器日志
docker logs <container-id>
kubectl logs <pod-name> -n ai-ready-prod

# 2. 查看事件
kubectl describe pod <pod-name> -n ai-ready-prod

# 3. 进入容器调试
docker exec -it <container-id> sh
kubectl exec -it <pod-name> -n ai-ready-prod -- sh

# 4. 检查资源使用
docker stats
kubectl top pods -n ai-ready-prod
```

#### 镜像拉取失败

```bash
# 1. 检查镜像是否存在
docker images | grep ai-ready

# 2. 手动拉取镜像
docker pull ghcr.io/ai-ready/ai-ready-api:latest

# 3. 检查镜像仓库凭证
kubectl get secrets -n ai-ready-prod
kubectl describe secret regcred -n ai-ready-prod
```

#### 服务无响应

```bash
# 1. 检查健康状态
curl http://localhost:8080/actuator/health

# 2. 检查端口监听
netstat -tlnp | grep 8080

# 3. 检查网络
kubectl get svc -n ai-ready-prod
kubectl get ingress -n ai-ready-prod
```

### 6.3 性能调优

#### JVM参数调优

```bash
# 开发环境
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 生产环境
JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

#### 资源限制调优

| 环境 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|-------------|-----------|----------------|--------------|
| 开发 | 250m | 500m | 512Mi | 1Gi |
| 测试 | 500m | 1000m | 1Gi | 2Gi |
| 生产 | 500m | 2000m | 1Gi | 4Gi |

### 6.4 备份与恢复

#### 数据库备份

```bash
# PostgreSQL备份
docker exec ai-ready-postgres pg_dump -U ai_ready ai_ready > backup.sql

# 恢复
docker exec -i ai-ready-postgres psql -U ai_ready ai_ready < backup.sql
```

#### Redis备份

```bash
# RDB备份
docker exec ai-ready-redis redis-cli BGSAVE
docker cp ai-ready-redis:/data/dump.rdb ./redis-backup.rdb

# 恢复
docker cp ./redis-backup.rdb ai-ready-redis:/data/dump.rdb
docker restart ai-ready-redis
```

---

## 附录

### A. 配置文件清单

```
I:\AI-Ready\
├── docker\
│   ├── Dockerfile.api              # API服务镜像
│   ├── Dockerfile.agent            # Agent服务镜像
│   ├── docker-compose.full.yml     # 完整容器编排
│   └── build-image.sh              # 构建脚本
├── k8s\
│   ├── prod\
│   │   ├── api-deployment.yaml     # 生产环境部署
│   │   └── full-deployment.yaml    # 完整K8s配置
│   ├── dev\
│   │   └── api-deployment.yaml     # 开发环境部署
│   └── base\
│       ├── configmap.yml           # 配置映射
│       ├── services.yml            # 服务定义
│       └── hpa.yml                 # 自动扩缩容
├── .github\
│   └── workflows\
│       └── container-build.yml     # GitHub Actions流水线
└── .gitlab-ci.yml                  # GitLab CI流水线
```

### B. 端口说明

| 端口 | 服务 | 协议 | 说明 |
|------|------|------|------|
| 80 | Nginx | HTTP | Web入口 |
| 443 | Nginx | HTTPS | 安全Web入口 |
| 8080 | Core API | HTTP | API服务 |
| 8000 | Core Agent | HTTP | Agent服务 |
| 5432 | PostgreSQL | TCP | 数据库 |
| 6379 | Redis | TCP | 缓存服务 |
| 8848 | Nacos | HTTP | 配置中心 |
| 9848 | Nacos | gRPC | Nacos通信 |
| 2375 | Docker | TCP | Docker API |
| 6443 | Kubernetes | HTTPS | K8s API |

### C. 常用命令速查

```bash
# Docker
docker build -t ai-ready/api:latest -f docker/Dockerfile.api .
docker run -d -p 8080:8080 --name api ai-ready/api:latest
docker exec -it api sh
docker logs -f api

# Docker Compose
docker-compose -f docker-compose.full.yml up -d
docker-compose -f docker-compose.full.yml down
docker-compose -f docker-compose.full.yml logs -f api

# Kubernetes
kubectl apply -f k8s/prod/full-deployment.yaml
kubectl get all -n ai-ready-prod
kubectl logs -f deployment/ai-ready-api -n ai-ready-prod
kubectl scale deployment ai-ready-api --replicas=5 -n ai-ready-prod
kubectl rollout undo deployment/ai-ready-api -n ai-ready-prod
```

---

**文档版本**: v2.0.0  
**最后更新**: 2026-04-04  
**维护人**: devops-engineer  
**项目**: AI-Ready
