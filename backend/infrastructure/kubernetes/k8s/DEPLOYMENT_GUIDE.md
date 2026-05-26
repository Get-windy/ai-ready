# 企智连项目 Kubernetes 部署指南

## 📋 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细部署步骤](#详细部署步骤)
- [配置文件说明](#配置文件说明)
- [运维操作](#运维操作)
- [故障排查](#故障排查)
- [安全建议](#安全建议)

---

## 环境要求

### Kubernetes 集群

- **版本**: Kubernetes 1.24+
- **节点数**: 至少 3 个 Worker 节点
- **资源**: 
  - CPU: 8 核以上
  - 内存: 16GB 以上
  - 存储: 100GB 以上

### 必需组件

| 组件 | 版本 | 用途 |
|------|------|------|
| Nginx Ingress Controller | 1.5+ | 流量入口 |
| cert-manager | 1.10+ | TLS 证书管理 |
| Prometheus + Grafana | 2.40+ | 监控告警 |
| Metrics Server | 0.6+ | HPA 指标采集 |

### 可选组件

- **Istio**: 服务网格（推荐用于生产环境）
- **Velero**: 备份恢复
- **Vault**: 密钥管理

---

## 快速开始

### 1. 克隆配置

```bash
# 创建部署目录
mkdir -p ~/ai-ready-k8s
cd ~/ai-ready-k8s

# 复制配置文件
cp -r /path/to/k8s/* .
```

### 2. 修改配置

```bash
# 编辑镜像地址
sed -i 's/your-registry.com\/ai-ready\/backend:v1.0.0/your-real-registry\/ai-ready-backend:latest/g' 03-deployment-backend.yaml
sed -i 's/your-registry.com\/ai-ready\/frontend:v1.0.0/your-real-registry\/ai-ready-frontend:latest/g' 04-deployment-frontend.yaml

# 编辑域名
sed -i 's/aiready.example.com/your-domain.com/g' 08-ingress.yaml
```

### 3. 一键部署

```bash
# 使用 kubectl 直接部署
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secret.yaml
kubectl apply -f 03-deployment-backend.yaml
kubectl apply -f 04-deployment-frontend.yaml
kubectl apply -f 05-deployment-postgres.yaml
kubectl apply -f 06-deployment-redis.yaml
kubectl apply -f 07-service.yaml
kubectl apply -f 08-ingress.yaml
kubectl apply -f 09-hpa.yaml
kubectl apply -f 10-monitoring.yaml

# 或使用 Kustomize
kubectl apply -k .
```

### 4. 验证部署

```bash
# 查看 Pod 状态
kubectl get pods -n ai-ready -w

# 查看服务状态
kubectl get svc -n ai-ready

# 查看 Ingress
kubectl get ingress -n ai-ready

# 测试访问
curl https://your-domain.com/health
curl https://api.your-domain.com/actuator/health
```

---

## 详细部署步骤

### 步骤 1: 创建命名空间

```bash
kubectl apply -f 00-namespace.yaml
```

此步骤创建：
- `ai-ready` 命名空间
- 资源配额限制
- 默认网络策略

### 步骤 2: 配置 ConfigMap

```bash
kubectl apply -f 01-configmap.yaml
```

包含配置：
- 应用配置 (`ai-ready-app-config`)
- 数据库配置 (`ai-ready-db-config`)
- 消息队列配置 (`ai-ready-mq-config`)
- Nginx 配置 (`ai-ready-nginx-config`)

### 步骤 3: 配置 Secret

```bash
# ⚠️ 生产环境必须修改密码！
kubectl apply -f 02-secret.yaml
```

**⚠️ 安全警告**: 
- 生产环境请使用强密码替换默认密码
- 建议使用 Vault 或云厂商 KMS 管理密钥
- 定期轮换密码

### 步骤 4: 部署数据库

```bash
kubectl apply -f 05-deployment-postgres.yaml
kubectl apply -f 06-deployment-redis.yaml
```

等待数据库就绪：
```bash
kubectl wait --for=condition=ready pod -l component=database -n ai-ready --timeout=120s
```

### 步骤 5: 部署后端服务

```bash
kubectl apply -f 03-deployment-backend.yaml
```

检查状态：
```bash
kubectl rollout status deployment/ai-ready-backend -n ai-ready
```

### 步骤 6: 部署前端服务

```bash
kubectl apply -f 04-deployment-frontend.yaml
```

### 步骤 7: 配置 Service

```bash
kubectl apply -f 07-service.yaml
```

### 步骤 8: 配置 Ingress

```bash
kubectl apply -f 08-ingress.yaml
```

**前提条件**:
- 已安装 Nginx Ingress Controller
- 已配置 DNS 解析
- 已安装 cert-manager（如需自动证书）

### 步骤 9: 配置 HPA

```bash
kubectl apply -f 09-hpa.yaml
```

**前提条件**:
- 已安装 Metrics Server

验证 HPA：
```bash
kubectl get hpa -n ai-ready
```

### 步骤 10: 配置监控

```bash
kubectl apply -f 10-monitoring.yaml
```

**前提条件**:
- 已安装 Prometheus Operator

---

## 配置文件说明

| 文件 | 说明 | 关键配置 |
|------|------|----------|
| `00-namespace.yaml` | 命名空间和基础资源 | 资源配额、网络策略 |
| `01-configmap.yaml` | 应用配置 | 数据库连接、Redis配置 |
| `02-secret.yaml` | 敏感信息 | 密码、密钥、TLS证书 |
| `03-deployment-backend.yaml` | 后端服务 | 3副本、资源限制、健康检查 |
| `04-deployment-frontend.yaml` | 前端服务 | 2副本、Nginx配置 |
| `05-deployment-postgres.yaml` | PostgreSQL | StatefulSet、50GB存储 |
| `06-deployment-redis.yaml` | Redis缓存 | 内存限制400MB |
| `07-service.yaml` | 服务发现 | ClusterIP类型 |
| `08-ingress.yaml` | 入口路由 | HTTPS、速率限制 |
| `09-hpa.yaml` | 自动扩缩容 | CPU 70%、内存 80% |
| `10-monitoring.yaml` | 监控告警 | Prometheus规则 |
| `kustomization.yaml` | Kustomize配置 | 资源管理 |

---

## 运维操作

### 查看日志

```bash
# 后端日志
kubectl logs -f deployment/ai-ready-backend -n ai-ready --tail=100

# 前端日志
kubectl logs -f deployment/ai-ready-frontend -n ai-ready --tail=100

# 数据库日志
kubectl logs -f statefulset/postgres -n ai-ready --tail=100
```

### 扩容缩容

```bash
# 手动扩容后端
kubectl scale deployment ai-ready-backend --replicas=5 -n ai-ready

# 查看 HPA 状态
kubectl get hpa -n ai-ready -w
```

### 滚动更新

```bash
# 更新镜像
kubectl set image deployment/ai-ready-backend backend=your-registry/ai-ready-backend:v1.1.0 -n ai-ready

# 查看更新进度
kubectl rollout status deployment/ai-ready-backend -n ai-ready

# 回滚更新
kubectl rollout undo deployment/ai-ready-backend -n ai-ready
```

### 备份数据

```bash
# 备份 PostgreSQL
kubectl exec -it postgres-0 -n ai-ready -- pg_dump -U aiready_user aiready > backup_$(date +%Y%m%d).sql

# 备份 Redis
kubectl exec -it deployment/redis -n ai-ready -- redis-cli -a $(kubectl get secret ai-ready-redis-secret -n ai-ready -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d) SAVE
```

---

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 事件
kubectl describe pod <pod-name> -n ai-ready

# 查看容器日志
kubectl logs <pod-name> -n ai-ready --previous
```

### 服务无法访问

```bash
# 检查 Service 端点
kubectl get endpoints -n ai-ready

# 检查 Ingress
kubectl describe ingress ai-ready-ingress -n ai-ready

# 测试内部访问
kubectl run debug --rm -it --image=busybox -n ai-ready -- wget -O- http://backend-service:8080/actuator/health
```

### HPA 不工作

```bash
# 检查 Metrics Server
kubectl top nodes
kubectl top pods -n ai-ready

# 检查 HPA 详情
kubectl describe hpa ai-ready-backend-hpa -n ai-ready
```

### 数据库连接失败

```bash
# 测试数据库连接
kubectl exec -it postgres-0 -n ai-ready -- psql -U aiready_user -d aiready -c "SELECT 1;"

# 检查 Secret
kubectl get secret ai-ready-db-secret -n ai-ready -o yaml
```

---

## 安全建议

### 1. 网络安全

- 启用 NetworkPolicy 限制 Pod 间通信
- 使用 Ingress TLS 加密传输
- 配置 WAF 防护

### 2. 密钥管理

- 使用 Vault 或云厂商 KMS
- 定期轮换密码
- 避免将密钥提交到 Git

### 3. 镜像安全

- 使用私有镜像仓库
- 启用镜像扫描
- 定期更新基础镜像

### 4. 访问控制

- 配置 RBAC
- 启用审计日志
- 限制特权容器

### 5. 监控告警

- 配置关键指标告警
- 设置日志聚合
- 定期安全扫描

---

## 附录

### 常用命令速查

```bash
# 查看所有资源
kubectl get all -n ai-ready

# 进入 Pod 调试
kubectl exec -it <pod-name> -n ai-ready -- /bin/sh

# 端口转发
kubectl port-forward svc/backend-service 8080:8080 -n ai-ready

# 导出配置
kubectl get all -n ai-ready -o yaml > ai-ready-backup.yaml

# 清理资源
kubectl delete namespace ai-ready
```

### 资源限制参考

| 服务 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|-------------|-----------|----------------|--------------|
| Backend | 500m | 2000m | 512Mi | 1536Mi |
| Frontend | 100m | 500m | 128Mi | 256Mi |
| PostgreSQL | 500m | 2000m | 512Mi | 2Gi |
| Redis | 100m | 500m | 256Mi | 512Mi |

---

**维护者**: DevOps Team  
**版本**: 1.0.0  
**更新日期**: 2026-04-15
