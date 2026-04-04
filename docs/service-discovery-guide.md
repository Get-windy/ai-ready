# AI-Ready 服务发现配置文档

**项目**: 智企连·AI-Ready  
**版本**: v1.0  
**日期**: 2026-04-04

---

## 一、服务发现架构

AI-Ready使用Kubernetes原生服务发现机制。

### 服务清单

| 服务名 | 类型 | 端口 | 说明 |
|--------|------|------|------|
| ai-ready-api | ClusterIP | 8080 | 主API服务 |
| ai-ready-api-headless | Headless | 8080 | StatefulSet服务发现 |
| postgres | ClusterIP | 5432 | PostgreSQL服务 |
| redis | ClusterIP | 6379 | Redis服务 |
| ai-engine | ClusterIP | 8080 | AI引擎服务 |

---

## 二、服务注册与发现

### 2.1 服务注册
Kubernetes通过label selector自动注册Pod到Service。

### 2.2 DNS解析
- 集群内: `ai-ready-api.ai-ready.svc.cluster.local`
- 同命名空间: `ai-ready-api`

### 2.3 负载均衡
- ClusterIP: 轮询负载均衡
- SessionAffinity: 支持会话保持

---

## 三、部署命令

```bash
kubectl apply -f k8s/base/services.yml
kubectl get svc -n ai-ready
kubectl get endpoints -n ai-ready
```

---

**文档生成**: devops-engineer
