# AI-Ready 网络策略配置文档

**项目**: 智企连·AI-Ready  
**日期**: 2026-04-04

---

## 一、网络隔离方案

- API服务: 允许Ingress访问
- PostgreSQL: 仅允许API服务访问
- Redis: 仅允许API服务访问
- AI引擎: 仅允许API服务访问

## 二、部署

```bash
kubectl apply -f k8s/base/networkpolicy.yml
kubectl get networkpolicy -n ai-ready
```

---

**文档生成**: devops-engineer
