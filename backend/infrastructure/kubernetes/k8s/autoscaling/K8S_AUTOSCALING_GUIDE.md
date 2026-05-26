# AI-Ready Kubernetes 自动扩缩容配置指南

> 版本: v1.0.0  
> 更新日期: 2026-04-09  
> 适用环境: 开发/测试/生产

---

## 目录

1. [概述](#概述)
2. [HPA - 水平Pod自动扩缩容](#hpa---水平pod自动扩缩容)
3. [VPA - 垂直Pod自动扩缩容](#vpa---垂直pod自动扩缩容)
4. [Cluster Autoscaler - 集群自动扩缩容](#cluster-autoscaler---集群自动扩缩容)
5. [资源配额和限制](#资源配额和限制)
6. [部署步骤](#部署步骤)
7. [验证和监控](#验证和监控)
8. [故障排查](#故障排查)

---

## 概述

AI-Ready项目采用三层自动扩缩容策略，确保系统在高负载下保持高可用性：

```
┌─────────────────────────────────────────────────────────────────┐
│                    三层自动扩缩容架构                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │   HPA        │    │   VPA        │    │ Cluster Autoscaler│  │
│  │  (水平扩缩容) │    │  (垂直扩缩容) │    │   (集群扩缩容)     │  │
│  └──────┬───────┘    └──────┬───────┘    └────────┬─────────┘  │
│         │                   │                     │             │
│         ▼                   ▼                     ▼             │
│    Pod数量调整          Pod资源调整          节点数量调整        │
│    (3-50个)            (CPU/Memory)          (自动增减)         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 扩缩容触发条件

| 层级 | 触发条件 | 响应时间 | 影响范围 |
|------|----------|----------|----------|
| HPA | CPU > 70% 或 Memory > 80% 或 QPS > 1000 | 30-60秒 | Pod数量 |
| VPA | 资源使用率持续偏离配置 | 自动或手动 | Pod资源配置 |
| Cluster Autoscaler | 资源不足无法调度Pod | 30-120秒 | 节点数量 |

---

## HPA - 水平Pod自动扩缩容

### 1. 基础HPA配置 (CPU/Memory)

```yaml
# hpa-basic.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-api-hpa
  namespace: ai-ready
  labels:
    app: ai-ready-api
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-api
  minReplicas: 3
  maxReplicas: 50
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
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 缩容冷却时间5分钟
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Min
    scaleUp:
      stabilizationWindowSeconds: 60   # 扩容冷却时间1分钟
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 5
        periodSeconds: 30
      selectPolicy: Max
```

### 2. 自定义指标HPA (QPS)

```yaml
# hpa-custom-metrics.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-api-hpa-qps
  namespace: ai-ready
  labels:
    app: ai-ready-api
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-api
  minReplicas: 3
  maxReplicas: 50
  metrics:
  # 基于HTTP请求QPS扩缩容
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"  # 每Pod平均1000 QPS
  # 基于请求延迟扩缩容
  - type: Object
    object:
      metric:
        name: http_server_requests_seconds_count
      describedObject:
        apiVersion: apps/v1
        kind: Deployment
        name: ai-ready-api
      target:
        type: AverageValue
        averageValue: "500"
```

### 3. 多服务HPA配置

```yaml
# hpa-services.yaml
---
# API服务HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-api-hpa
  namespace: ai-ready
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-api
  minReplicas: 3
  maxReplicas: 30
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Pods
        value: 2
        periodSeconds: 30
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Pods
        value: 1
        periodSeconds: 120

---
# Agent服务HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-agent-hpa
  namespace: ai-ready
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-agent
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 75

---
# NLP服务HPA (AI计算密集型)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-ready-nlp-hpa
  namespace: ai-ready
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-nlp
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 75
```

---

## VPA - 垂直Pod自动扩缩容

### 1. VPA基础配置

```yaml
# vpa.yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: ai-ready-api-vpa
  namespace: ai-ready
  labels:
    app: ai-ready-api
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-api
  updatePolicy:
    updateMode: "Auto"  # Auto/Off/Initial/Recreate
  resourcePolicy:
    containerPolicies:
    - containerName: ai-ready-api
      minAllowed:
        cpu: 250m
        memory: 512Mi
      maxAllowed:
        cpu: 4000m
        memory: 8Gi
      controlledResources: ["cpu", "memory"]
      controlledValues: RequestsAndLimits
```

### 2. 多服务VPA配置

```yaml
# vpa-services.yaml
---
# API服务VPA
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: ai-ready-api-vpa
  namespace: ai-ready
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-api
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: ai-ready-api
      minAllowed:
        cpu: 250m
        memory: 512Mi
      maxAllowed:
        cpu: 2000m
        memory: 4Gi
      controlledResources: ["cpu", "memory"]

---
# NLP服务VPA (AI计算需要更多资源)
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: ai-ready-nlp-vpa
  namespace: ai-ready
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-nlp
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: ai-ready-nlp
      minAllowed:
        cpu: 1000m
        memory: 2Gi
      maxAllowed:
        cpu: 8000m
        memory: 16Gi
      controlledResources: ["cpu", "memory"]

---
# Agent服务VPA
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: ai-ready-agent-vpa
  namespace: ai-ready
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-ready-agent
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: ai-ready-agent
      minAllowed:
        cpu: 100m
        memory: 256Mi
      maxAllowed:
        cpu: 1000m
        memory: 2Gi
      controlledResources: ["cpu", "memory"]
```

### 3. VPA更新模式说明

| 模式 | 说明 | 使用场景 |
|------|------|----------|
| Auto | 自动更新Pod资源 | 生产环境推荐 |
| Off | 仅计算推荐值，不更新 | 测试/观察模式 |
| Initial | 仅对新Pod生效 | 滚动更新场景 |
| Recreate | 删除并重建Pod以更新资源 | 需要立即生效时 |

---

## Cluster Autoscaler - 集群