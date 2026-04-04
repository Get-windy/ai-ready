# AI-Ready 容器化部署方案

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-30  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [Docker 配置](#docker-配置)
3. [Kubernetes 部署](#kubernetes-部署)
4. [CI/CD 流水线](#cicd-流水线)
5. [部署方案](#部署方案)
6. [最佳实践](#最佳实践)

---

## 概述

### 容器化目标

- **标准化部署**: 所有环境一致的部署流程
- **快速扩展**: 根据负载自动伸缩
- **高可用性**: 多副本部署，故障自动恢复
- **资源隔离**: 容器级别的资源限制

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Layer                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Dockerfile  │  │ compose     │  │ Production  │             │
│  │ Java 17 JRE │  │ compose.yml │  │ compose.yml │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Kubernetes Layer                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Namespaces                            │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │   │
│  │  │   dev       │  │  staging    │  │   prod      │      │   │
│  │  │   namespace │  │  namespace  │  │   namespace │      │   │
│  │  │             │  │             │  │             │      │   │
│  │  │  Deployment │  │  Deployment │  │  Deployment │      │   │
│  │  │  Service    │  │  Service    │  │  Service    │      │   │
│  │  │  Ingress    │  │  Ingress    │  │  Ingress    │      │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   Components                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │   │
│  │  │   ConfigMap │  │   Secret    │  │  HPA / PDB  │      │   │
│  │  │   (shared)  │  │   (secure)  │  │  (autoscale)│      │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘      │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Docker 配置

### Dockerfile 优化

**特征**:
- ✅ 多阶段构建 - 减小镜像体积
- ✅ 非root用户 - 安全加固
- ✅ JVM 优化 - G1GC + 内存配置
- ✅ 健康检查 - 自动恢复

**镜像大小**: ~300MB (vs ~800MB unchanged)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /build/target/*.jar /app/app.jar
```

### Docker Compose

#### 基础配置 (docker-compose.yml)

```yaml
services:
  api:
    build:
      context: .
      dockerfile: Dockerfile
    image: ai-ready:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: development
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
```

#### 生产配置 (docker-compose.production.yml)

```yaml
services:
  api:
    image: ghcr.io/ai-ready/ai-ready-api:latest
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 1G
          cpus: '1.0'
```

---

## Kubernetes 部署

### 目录结构

```
k8s/
├── base/                    # 共享配置
│   ├── components.yaml      # ConfigMap, Secret, RBAC, HPA, NetworkPolicy
│   └── services.yaml        # Service definitions
├── dev/                     # 开发环境
│   ├── api-deployment.yaml  # 开发部署
│   └── ingress.yaml         # 开发 Ingress
├── staging/                 # 预发布环境
│   ├── api-deployment.yaml
│   └── ingress.yaml
└── prod/                    # 生产环境
    ├── api-deployment.yaml  # 生产部署
    ├── ingress.yaml         # HTTPS Ingress
    └── resource-quota.yaml  # 资源配额
```

### 基础组件 (base/components.yaml)

包含:
- **ConfigMap**: 应用配置
- **Secret**: 敏感数据
- **ServiceAccount**: 服务账户
- **Role/RoleBinding**: RBAC 权限
- **HorizontalPodAutoscaler**: 自动伸缩
- **NetworkPolicy**: 网络策略

```yaml
# HPA 配置示例
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-api-hpa
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          averageUtilization: 70
```

### 开发环境配置

**文件**: k8s/dev/api-deployment.yaml

**特性**:
- 1个副本
- DEBUG 日志级别
- 资源限制: 512Mi/0.5 CPU
- 本地开发域名: `dev.ai-ready.local`

### 生产环境配置

**文件**: k8s/prod/api-deployment.yaml

**特性**:
- 3个副本 (HA)
- WARN 日志级别
- 资源限制: 2Gi/1 CPU
- 启动探针 (60秒超时)
- 资源配额 (ResourceQuota)
- 网络策略 (隔离访问)
- HTTPS Ingress

---

## CI/CD 流水线

### 镜像构建流程

```
1. 代码提交 (Git Push/Pull Request)
   │
   ▼
2. CI Pipeline (GitHub Actions)
   ├─→ 代码检查 (Checkstyle/SpotBugs/PMD)
   ├─→ 单元测试 (JUnit/Jacoco)
   ├─→ 构建 JAR (Maven Package)
   ├─→ Docker Build & Push
   └─→ K8s Deployment (if prod)
```

### GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
jobs:
  docker-build:
    steps:
      - name: Build Docker image
        run: docker build -t ${{ env.IMAGE_NAME }}:${{ env.TAG }} .
      
      - name: Push to Registry
        run: |
          docker login -u ${{ github.actor }} -p ${{ secrets.GITHUB_TOKEN }}
          docker push ${{ env.IMAGE_NAME }}:${{ env.TAG }}
```

### GitLab CI

```yaml
# .gitlab-ci.yml
docker:build:
  script:
    - docker build -t ${DOCKER_IMAGE}:${CI_COMMIT_SHA} .
    - docker push ${DOCKER_IMAGE}:${CI_COMMIT_SHA}
```

---

## 部署方案

### 方法1: Docker Compose

```bash
# 开发环境
cd I:\AI-Ready
docker-compose up -d

# 生产环境
docker-compose -f docker-compose.production.yml up -d
```

### 方法2: Kubernetes

```bash
# 开发环境
kubectl apply -f k8s/base/
kubectl apply -f k8s/dev/

# 预发布环境
kubectl apply -f k8s/base/
kubectl apply -f k8s/staging/

# 生产环境
kubectl apply -f k8s/base/
kubectl apply -f k8s/prod/
```

### 部署检查清单

```markdown
## 部署前检查

### Docker
- [ ] Dockerfile 构建成功
- [ ] 镜像大小合理 (<500MB)
- [ ] 健康检查通过
- [ ] 环境变量配置正确

### Kubernetes
- [ ] Namespace 存在
- [ ] ConfigMap/Secret 已创建
- [ ] ServiceAccount 绑定正确
- [ ] HPA 配置合理

### 生产部署
- [ ] 备份完成
- [ ] 监控告警配置
- [ ] 备份回滚方案
- [ ] 团队通知
```

---

## 最佳实践

### Docker 最佳实践

1. **多阶段构建**: 减小镜像体积
2. **非root用户**: 安全加固
3. **健康检查**: 自动恢复
4. **资源限制**: 防止资源滥用
5. **镜像签名**: 确保镜像来源可信

### Kubernetes 最佳实践

1. **命名空间隔离**: dev/staging/prod 分离
2. **资源配额**: 防止资源滥用
3. **HPA 自动伸缩**: 根据负载自动调整
4. **网络策略**: 隔离服务通信
5. **PDB PodDisruption预算**: 保证可用性

### 安全最佳实践

1. **Secret 管理**: 使用 Kubernetes Secret 或 Vault
2. **镜像扫描**: Trivy 安全扫描
3. **RBAC 权限**: 最小权限原则
4. **网络策略**: 限制服务间通信
5. **非root运行**: 容器安全上下文

### 监控指标

| 指标 | 描述 | 阈值 |
|------|------|------|
| Container CPU | 容器 CPU 使用率 | >80% |
| Container Memory | 容器内存使用率 | >85% |
| Pod Restart | Pod 重启次数 | >3次/小时 |
| Health Check | 健康检查失败 | 任何失败 |
| HPA Scale | HPA 扩缩容次数 | 监控频率 |

---

## 故障排查

### 常见问题

1. **容器启动失败**
   - 检查镜像是否存在
   - 检查环境变量
   - 查看容器日志

2. **服务无法访问**
   - 检查 Service 配置
   - 检查 Ingress 配置
   - 检查网络策略

3. **健康检查失败**
   - 检查应用日志
   - 检查健康检查路径
   - 调整 healthcheck 超时

### 调试命令

```bash
# 查看Pod状态
kubectl get pods -n ai-ready-prod

# 查看Pod日志
kubectl logs -f ai-ready-api-xxx -n ai-ready-prod

# 进入容器
kubectl exec -it ai-ready-api-xxx -n ai-ready-prod -- sh

# 查看事件
kubectl get events -n ai-ready-prod --sort-by='.lastTimestamp'
```

---

## 附录

### 配置文件清单

| 文件 | 用途 | 大小 |
|------|------|------|
| Dockerfile | Java应用镜像构建 | 1.9KB |
| docker-compose.yml | 开发环境 | 3.5KB |
| docker-compose.production.yml | 生产环境 | 6.4KB |
| base/components.yaml | K8s共享组件 | 4KB |
| dev/api-deployment.yaml | 开发部署 | 5.5KB |
| prod/api-deployment.yaml | 生产部署 | 10KB |

### 常用命令

```bash
# Docker
docker build -t ai-ready:latest .
docker run -p 8080:8080 ai-ready:latest

# Kubernetes
kubectl apply -f k8s/ -R
kubectl rollout status deployment/ai-ready-api -n ai-ready-prod
kubectl autoscale deployment/ai-ready-api --min=2 --max=10 --cpu-percent=70
```

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-30*