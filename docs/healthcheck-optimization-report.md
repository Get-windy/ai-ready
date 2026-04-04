# AI-Ready 容器健康检查优化报告

**项目**: 智企连·AI-Ready  
**版本**: v1.0  
**日期**: 2026-04-03

---

## 一、优化内容

| 探针类型 | 路径 | 初始延迟 | 周期 | 超时 | 失败阈值 |
|----------|------|----------|------|------|----------|
| **startup** | /actuator/health/liveness | 10s | 5s | 3s | 30次 |
| **liveness** | /actuator/health/liveness | 60s | 10s | 5s | 3次 |
| **readiness** | /actuator/health/readiness | 30s | 5s | 3s | 3次 |

## 二、关键优化

1. **启动探针**: 处理慢启动场景，最多等待150秒
2. **就绪探针**: 快速检测，5秒周期
3. **优雅关闭**: 60秒grace period

## 三、部署

```bash
kubectl apply -f k8s/base/deployment-healthcheck.yml
```

---

**报告生成**: devops-engineer
