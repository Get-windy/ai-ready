# AI-Ready Istio Configuration Guide

## 一、Istio安装

### 1.1 安装Istio Operator
```bash
istioctl operator init
kubectl apply -f istio-config.yaml
```

### 1.2 验证安装
```bash
kubectl get pods -n istio-system
kubectl get svc -n istio-system
```

## 二、核心组件

### 2.1 Gateway
- HTTP/HTTPS入口流量管理
- TLS证书管理
- 域名绑定

### 2.2 VirtualService
- 请求路由规则
- 重试配置（3次/2秒超时）
- 版本分流

### 2.3 DestinationRule
- 熔断配置（最大连接数100）
- 异例检测（5次5xx错误剔除30秒）
- mTLS启用

## 三、安全配置

### 3.1 mTLS
- PeerAuthentication: STRICT模式
- ISTIO_MUTUAL证书

### 3.2 AuthorizationPolicy
- 域内服务互访权限
- API访问控制

## 四、可观测性

### 4.1 Prometheus
- 服务指标采集
- Envoy stats监控

### 4.2 Grafana
- Istio Dashboard
- 请求率/错误率/P99延迟

### 4.3 Zipkin
- 分布式追踪
- 调用链可视化

## 五、命令速查

```bash
# 启用Sidecar注入
kubectl label namespace ai-ready istio-injection=enabled

# 查看代理状态
istioctl proxy-status

# 分析配置
istioctl analyze

# 查看指标
kubectl exec -it deploy/prometheus -n istio-system -- promtool query instant 'istio_requests_total'
```