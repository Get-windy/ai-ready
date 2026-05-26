# AI-Ready 容器编排优化文档

**版本**: v2.0  
**日期**: 2026-04-01  
**作者**: devops-engineer

---

## 一、优化概述

### 1.1 优化目标
- 提升服务调度效率
- 增强服务可用性
- 优化资源利用率
- 加强安全防护

### 1.2 优化内容

| 优化项 | 原配置 | 优化后 | 改进效果 |
|--------|--------|--------|----------|
| Deployment副本 | 固定3副本 | HPA 2-10副本 | 自动伸缩，节省资源 |
| 更新策略 | maxSurge=1 | maxSurge=25% | 平滑滚动更新 |
| 资源请求 | 1Gi内存 | 512Mi内存 | 节省50%资源 |
| 探针配置 | 基础配置 | 优化延迟和阈值 | 减少误判 |
| 安全加固 | 基础 | 完整安全上下文 | 安全增强 |

---

## 二、Deployment优化

### 2.1 副本管理优化

**优化前**:
```yaml
replicas: 3  # 固定副本数
```

**优化后**:
```yaml
replicas: 2  # 基础副本
# 配合HPA实现2-10副本自动伸缩
```

**改进**:
- 低负载时减少副本，节省资源
- 高负载时自动扩容，保证性能

### 2.2 更新策略优化

**优化前**:
```yaml
maxSurge: 1
maxUnavailable: 0
```

**优化后**:
```yaml
maxSurge: 25%
maxUnavailable: 25%
```

**改进**:
- 百分比策略更灵活
- 支持更大规模部署

### 2.3 Pod调度优化

**新增配置**:
```yaml
priorityClassName: high-priority
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          topologyKey: topology.kubernetes.io/zone
```

**改进**:
- 跨可用区分散部署
- 高优先级保证调度

---

## 三、Service优化

### 3.1 服务发现优化

```yaml
spec:
  type: ClusterIP
  sessionAffinity: None  # 无状态服务
```

### 3.2 负载均衡优化

```yaml
annotations:
  traffic.sidecar.istio.io/loadBalancer: ROUND_ROBIN
```

---

## 四、ConfigMap/Secret优化

### 4.1 ConfigMap结构

```yaml
data:
  SPRING_PROFILES_ACTIVE: "production"
  LOGGING_LEVEL_ROOT: "INFO"
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,metrics,prometheus"
```

### 4.2 Secret管理建议

| 方案 | 适用场景 | 安全级别 |
|------|----------|----------|
| Sealed Secrets | GitOps | 高 |
| External Secrets | 企业级 | 高 |
| Vault集成 | 大规模 | 最高 |
| 原生Secret | 开发测试 | 中 |

---

## 五、资源限制优化

### 5.1 资源配置对比

| 环境 | CPU请求 | CPU限制 | 内存请求 | 内存限制 |
|------|---------|---------|----------|----------|
| Dev | 100m | 500m | 256Mi | 512Mi |
| Staging | 250m | 750m | 512Mi | 768Mi |
| Prod | 250m | 1000m | 512Mi | 1Gi |

### 5.2 JVM参数优化

```yaml
JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"
```

---

## 六、HPA自动伸缩

### 6.1 配置说明

```yaml
minReplicas: 2
maxReplicas: 10
metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 6.2 伸缩行为

| 操作 | 稳定窗口 | 策略 |
|------|----------|------|
| 扩容 | 60秒 | 每次+2个Pod |
| 缩容 | 300秒 | 每次-1个Pod |

---

## 七、健康检查优化

### 7.1 探针配置

| 探针 | 初始延迟 | 间隔 | 超时 | 失败阈值 |
|------|----------|------|------|----------|
| liveness | 60s | 15s | 5s | 3 |
| readiness | 30s | 10s | 5s | 3 |
| startup | 10s | 10s | 5s | 30 |

### 7.2 探针端点

- **liveness**: `/actuator/health/liveness`
- **readiness**: `/actuator/health/readiness`

---

## 八、安全加固

### 8.1 Pod安全上下文

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  seccompProfile:
    type: RuntimeDefault
```

### 8.2 容器安全

```yaml
securityContext:
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
```

---

## 九、部署指南

### 9.1 前置条件

- Kubernetes 1.25+
- Metrics Server 已安装
- Ingress Controller 已部署

### 9.2 部署步骤

```bash
# 1. 创建命名空间
kubectl create namespace ai-ready

# 2. 创建密钥
kubectl create secret generic ai-ready-secrets \
  --from-literal=db-password=YOUR_PASSWORD \
  --from-literal=jwt-secret=YOUR_SECRET \
  -n ai-ready

# 3. 应用配置
kubectl apply -f api-deployment-optimized.yaml

# 4. 验证部署
kubectl get pods -n ai-ready
kubectl get hpa -n ai-ready
```

---

## 十、监控告警

### 10.1 关键指标

| 指标 | 阈值 | 告警级别 |
|------|------|----------|
| CPU使用率 | >80% | Warning |
| 内存使用率 | >85% | Warning |
| Pod重启次数 | >3/5min | Critical |
| HPA扩容次数 | >5/hour | Warning |

---

**文档完成时间**: 2026-04-01  
**版本**: v2.0