# AI监控系统集成方案设计文档

**文档版本**: v1.0
**创建日期**: 2026-04-27
**所属项目**: AI-Ready（企智连系统）
**Sprint**: Sprint 27+1

---

## 1. 文档概述

### 1.1 目标
将AI服务的监控指标与现有的监控系统（Prometheus + Grafana）无缝集成，建立完整的AI服务监控体系，实现数据采集、存储、可视化和告警的全流程闭环。

### 1.2 现有监控架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  AI Services│────▶│  Prometheus  │────▶│  Grafana    │
└─────────────┘     └──────────────┘     └─────────────┘
      │                    │                    │
      │                    ▼                    ▼
      │           ┌─────────────┐     ┌─────────────┐
      │           │ Alertmanager│────▶│  企业微信   │
      │           └─────────────┘     └─────────────┘
      │
      └────▶ Pushgateway（临时指标推送）
```

---

## 2. 与Prometheus集成设计

### 2.1 集成方式选择

#### 2.1.1 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| 暴露/metrics端点 | 实时、标准 | 需要网络可达 | 长期运行的AI服务 |
| Pushgateway | 灵活、无网络限制 | 不是持久化存储 | 批处理任务、短时任务 |
| Remote Write | 高可用、多集群 | 复杂度高 | 大规模部署 |

#### 2.1.2 推荐方案

**AI推理服务**: 暴露/metrics端点
**批处理任务**: Pushgateway
**边缘AI服务**: Remote Write（可选）

---

### 2.2 /metrics端点设计

#### 2.2.1 端点配置

| 配置项 | 值 |
|-------|---|
| 端点路径 | /metrics |
| 访问端口 | 9090（与业务端口分离） |
| 访问协议 | HTTP |
| 认证方式 | Basic Auth（用户名: ai_monitor） |

#### 2.2.2 Prometheus抓取配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'ai-services'
    metrics_path: '/metrics'
    scrape_interval: 10s
    scrape_timeout: 5s
    static_configs:
      - targets:
          - 'ai-approval-service:9090'
          - 'ai-chat-service:9090'
    basic_auth:
      username: 'ai_monitor'
      password: '${AI_MONITOR_PASSWORD}'
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
      - source_labels: [__address__]
        regex: '([^:]+)(:[0-9]+)?'
        target_label: service
        replacement: '$1'
```

#### 2.2.3 Micrometer集成（Spring Boot应用）

**Maven依赖**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.11.0</version>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.11.0</version>
</dependency>
```

**application.yml配置**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        ai.inference.latency: true
      percentiles:
        ai.inference.latency: 0.5, 0.95, 0.99
```

---

### 2.3 Pushgateway集成

#### 2.3.1 部署Pushgateway

**Docker部署**:
```bash
docker run -d \
  --name pushgateway \
  -p 9091:9091 \
  prom/pushgateway
```

**Kubernetes部署**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: pushgateway
spec:
  selector:
    app: pushgateway
  ports:
    - port: 9091
      targetPort: 9091
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pushgateway
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pushgateway
  template:
    metadata:
      labels:
        app: pushgateway
    spec:
      containers:
        - name: pushgateway
          image: prom/pushgateway:latest
          ports:
            - containerPort: 9091
```

#### 2.3.2 Prometheus配置

```yaml
- job_name: 'pushgateway'
  honor_labels: true
  static_configs:
    - targets:
        - 'pushgateway:9091'
```

#### 2.3.3 数据推送示例

**Python示例**:
```python
from prometheus_client import CollectorRegistry, Gauge, push_to_gateway

registry = CollectorRegistry()
g = Gauge('ai_inference_latency', 'AI inference latency', ['model_name'], registry=registry)
g.labels(model_name='approval-model').set(123.45)
push_to_gateway('pushgateway:9091', job='ai-inference-batch', registry=registry)
```

**Java示例**:
```java
import io.prometheus.client.exporter.PushGateway;

PushGateway pg = new PushGateway("pushgateway:9091");
pg.pushAdd(registry, "ai-inference-batch");
```

---

## 3. 监控数据采集和存储方案

### 3.1 数据采集架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  AI Services│────▶│  Prometheus  │────▶│  TSDB       │
│  (Metrics)  │     │  (采集)      │     │  (存储)     │
└─────────────┘     └──────────────┘     └─────────────┘
       │                    │
       │                    ▼
       │            ┌─────────────┐
       └───────────▶│ Thanos      │  ← 长期存储
                    │ (可选)      │
                    └─────────────┘
```

---

### 3.2 数据采集策略

#### 3.2.1 采集频率

| 指标类别 | 采集频率 | 理由 |
|---------|---------|------|
| 实时延迟指标 | 10秒 | 实时告警需要 |
| 资源指标 | 15秒 | 平衡精度和性能 |
| 业务指标 | 10秒 | 实时监控 |
| 离线质量指标 | 每日 | 批处理评估 |

#### 3.2.2 采集配置优化

```yaml
# prometheus.yml优化配置
global:
  scrape_interval: 10s
  evaluation_interval: 10s
  external_labels:
    cluster: 'ai-ready-prod'
    replica: 'replica-1'

# 存储配置
storage:
  tsdb:
    path: /prometheus
    retention.time: 30d
    retention.size: 50GB

# 性能优化
scrape_configs:
  - job_name: 'ai-services'
    sample_limit: 10000
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'ai_.*'
        action: keep
```

---

### 3.3 数据存储方案

#### 3.3.1 Prometheus本地存储

**存储规划**:
| 数据类型 | 保留时间 | 存储大小估算 |
|---------|---------|-------------|
| 原始数据 | 30天 | 50GB |
| 聚合数据 | 90天 | 10GB |
| 合计 | - | 60GB |

**存储配置**:
```yaml
tsdb:
  retention.time: 30d
  retention.size: 50GB
```

#### 3.3.2 Thanos长期存储（可选）

**Thanos架构**:
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Prometheus  │────▶│   Thanos    │────▶│  S3/MinIO   │
│   (sidecar) │     │  (Store)    │     │  (对象存储) │
└─────────────┘     └─────────────┘     └─────────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │   Thanos    │
                   │   (Query)   │
                   └─────────────┘
```

**S3配置**:
```yaml
objstore:
  type: s3
  config:
    bucket: ai-metrics
    endpoint: s3.example.com
    access_key: ${AWS_ACCESS_KEY}
    secret_key: ${AWS_SECRET_KEY}
```

---

### 3.4 数据降采样策略

**降采样规则**:

| 原始数据 | 降采样1 | 降采样2 | 降采样3 |
|---------|--------|--------|--------|
| 10秒间隔 | 5分钟 | 1小时 | 1天 |
| 保留30天 | 保留90天 | 保留1年 | 永久 |

**降采样查询示例**:
```
# 5分钟聚合
rate(ai_inference_latency_sum[5m]) / rate(ai_inference_latency_count[5m])

# 1小时聚合
rate(ai_inference_latency_sum[1h]) / rate(ai_inference_latency_count[1h])
```

---

## 4. 监控数据可视化方案

### 4.1 Grafana仪表盘设计

#### 4.1.1 仪表盘结构

创建4个专门的AI监控仪表盘：

1. **AI服务实时监控仪表盘**
   - 实时请求量、QPS、错误率
   - P50/P95/P99延迟实时趋势
   - 资源使用率（CPU/GPU/内存）
   - 最近1小时关键指标变化

2. **AI模型质量监控仪表盘**
   - 模型准确率、精确率、召回率趋势
   - 用户采纳率、拒绝率趋势
   - 用户评分分布
   - 模型漂移检测状态

3. **AI资源监控仪表盘**
   - GPU利用率、显存使用、温度
   - CPU使用率、核心分配
   - 内存使用、堆内存、GC时间
   - 容器资源限制 vs 实际使用

4. **AI服务告警仪表盘**
   - 活跃告警列表
   - 告警趋势统计
   - 告警分类统计
   - 告警响应时间

---

#### 4.1.2 仪表盘JSON配置示例

**AI服务实时监控仪表盘**（部分配置）:
```json
{
  "dashboard": {
    "title": "AI Services Real-time Monitoring",
    "panels": [
      {
        "title": "Inference Requests per Second",
        "targets": [
          {
            "expr": "rate(ai_requests_total[1m])",
            "legendFormat": "{{service}}"
          }
        ],
        "type": "graph"
      },
      {
        "title": "P95 Inference Latency",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(ai_inference_latency_bucket[5m]))",
            "legendFormat": "{{service}}"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(ai_requests_failed[5m]) / rate(ai_requests_total[5m]) * 100",
            "legendFormat": "{{service}}"
          }
        ],
        "type": "gauge"
      }
    ]
  }
}
```

**导入仪表盘**:
```bash
# 使用Grafana CLI
curl -X POST http://grafana:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @ai-services-dashboard.json

# 或通过Grafana Web UI导入
```

---

### 4.2 可视化最佳实践

#### 4.2.1 图表类型选择

| 指标类型 | 推荐图表类型 | 理由 |
|---------|-------------|------|
| 时间序列趋势 | Line Chart | 展示变化趋势 |
| 当前状态 | Gauge | 展示当前值 |
| 分布统计 | Heatmap | 展示分布情况 |
| 对比分析 | Bar Chart | 对比多个指标 |
| 百分比 | Pie Chart | 展示占比 |

#### 4.2.2 颜色编码

| 状态 | 颜色 | 用途 |
|-----|------|------|
| 正常 | 绿色 | 指标在正常范围内 |
| 警告 | 黄色 | 指标接近阈值 |
| 严重 | 红色 | 指标超过阈值 |
| 未知 | 灰色 | 数据缺失 |

#### 4.2.3 告警可视化

在仪表盘中添加告警状态指示器：
```json
{
  "title": "Alert Status",
  "targets": [
    {
      "expr": "ALERTS{alertname=~\"AI.*\"}",
      "legendFormat": "{{alertname}}"
    }
  ],
  "type": "stat",
  "fieldConfig": {
    "defaults": {
      "thresholds": {
        "steps": [
          { "value": 0, "color": "green" },
          { "value": 1, "color": "yellow" },
          { "value": 2, "color": "red" }
        ]
      }
    }
  }
}
```

---

## 5. 监控告警通知集成方案

### 5.1 告警规则设计

#### 5.1.1 Prometheus告警规则

**告警规则文件**: `alerts/ai-services.yml`

```yaml
groups:
  - name: ai_services
    interval: 30s
    rules:
      # 延迟告警
      - alert: AIInferenceLatencyHigh
        expr: |
          histogram_quantile(0.95, rate(ai_inference_latency_bucket[5m])) > 1500
        for: 3m
        labels:
          severity: warning
          team: ai-team
        annotations:
          summary: "AI inference latency is high (P95 > 1.5s)"
          description: "Service: {{ $labels.service }}, P95: {{ $value }}ms"

      - alert: AIInferenceLatencyCritical
        expr: |
          histogram_quantile(0.99, rate(ai_inference_latency_bucket[5m])) > 3000
        for: 2m
        labels:
          severity: critical
          team: ai-team
        annotations:
          summary: "AI inference latency is critical (P99 > 3s)"
          description: "Service: {{ $labels.service }}, P99: {{ $value }}ms"

      # 错误率告警
      - alert: AIErrorRateHigh
        expr: |
          rate(ai_requests_failed[5m]) / rate(ai_requests_total[5m]) * 100 > 2
        for: 5m
        labels:
          severity: warning
          team: ai-team
        annotations:
          summary: "AI error rate is high (> 2%)"
          description: "Service: {{ $labels.service }}, Error rate: {{ $value }}%"

      - alert: AIErrorRateCritical
        expr: |
          rate(ai_requests_failed[5m]) / rate(ai_requests_total[5m]) * 100 > 5
        for: 2m
        labels:
          severity: critical
          team: ai-team
        annotations:
          summary: "AI error rate is critical (> 5%)"
          description: "Service: {{ $labels.service }}, Error rate: {{ $value }}%"

      # 资源告警
      - alert: AIGPUUtilizationHigh
        expr: ai_gpu_utilization > 90
        for: 5m
        labels:
          severity: warning
          team: ai-team
        annotations:
          summary: "AI GPU utilization is high (> 90%)"
          description: "GPU: {{ $labels.gpu }}, Utilization: {{ $value }}%"

      - alert: AIGPUTemperatureCritical
        expr: ai_gpu_temperature > 85
        for: 2m
        labels:
          severity: critical
          team: ai-team
        annotations:
          summary: "AI GPU temperature is critical (> 85°C)"
          description: "GPU: {{ $labels.gpu }}, Temperature: {{ $value }}°C"

      # 质量告警
      - alert: AIModelAccuracyDrop
        expr: |
          ai_model_accuracy < (ai_model_accuracy offset 1d) * 0.95
        for: 24h
        labels:
          severity: warning
          team: ai-team
        annotations:
          summary: "AI model accuracy dropped > 5%"
          description: "Model: {{ $labels.model }}, Current: {{ $value }}%"

      # 模型漂移告警
      - alert: AIModelDriftDetected
        expr: ai_feature_drift_score > 0.9
        for: 6h
        labels:
          severity: critical
          team: ai-team
        annotations:
          summary: "AI model drift detected"
          description: "Model: {{ $labels.model }}, Drift score: {{ $value }}"
```

**加载告警规则**:
```yaml
# prometheus.yml
rule_files:
  - "alerts/ai-services.yml"
```

---

### 5.2 Alertmanager配置

#### 5.2.1 Alertmanager配置文件

**alertmanager.yml**:
```yaml
global:
  resolve_timeout: 5m
  wechat_api_url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
  wechat_api_secret: 'YOUR_SECRET'

# 路由配置
route:
  receiver: 'default-receiver'
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  routes:
    # 严重告警 -> 所有渠道
    - match:
        severity: critical
      receiver: 'critical-alerts'
      continue: true

    # 警告告警 -> 企业微信 + 邮件
    - match:
        severity: warning
      receiver: 'warning-alerts'

# 接收器配置
receivers:
  - name: 'default-receiver'
    webhook_configs:
      - url: 'http://alertmanager-webhook:8080/webhook'

  - name: 'critical-alerts'
    wechat_configs:
      - corp_id: 'YOUR_CORP_ID'
        agent_id: 'YOUR_AGENT_ID'
        api_secret: 'YOUR_SECRET'
        to_user: '@all'
        message: |
          {{ range .Alerts }}
          【严重告警】{{ .Labels.alertname }}
          服务: {{ .Labels.service }}
          描述: {{ .Annotations.description }}
          时间: {{ .StartsAt.Format "2006-01-02 15:04:05" }}
          {{ end }}
    email_configs:
      - to: 'ai-team@example.com'
        headers:
          Subject: '[CRITICAL] AI Alert: {{ .GroupLabels.alertname }}'

  - name: 'warning-alerts'
    wechat_configs:
      - corp_id: 'YOUR_CORP_ID'
        agent_id: 'YOUR_AGENT_ID'
        api_secret: 'YOUR_SECRET'
        to_user: 'ai-team'
        message: |
          {{ range .Alerts }}
          【告警】{{ .Labels.alertname }}
          服务: {{ .Labels.service }}
          描述: {{ .Annotations.description }}
          时间: {{ .StartsAt.Format "2006-01-02 15:04:05" }}
          {{ end }}

# 抑制规则
inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'service']
```

---

### 5.3 企业微信告警集成

#### 5.3.1 企业微信Webhook配置

**获取Webhook地址**:
1. 登录企业微信管理后台
2. 进入"应用管理" → "应用" → "AI服务监控"
3. 在"Webhook"中配置接收URL
4. 复制Webhook地址

**Webhook URL格式**:
```
https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

#### 5.3.2 告警消息格式

**严重告警消息示例**:
```
【严重告警】AI推理延迟过高
━━━━━━━━━━━━━━━━━━
告警级别: CRITICAL
服务名称: ai-approval-service
当前P95延迟: 3245ms
阈值: 1500ms
开始时间: 2026-04-27 16:15:00
━━━━━━━━━━━━━━━━━━
处理建议:
1. 检查GPU负载情况
2. 检查模型是否需要重启
3. 查看日志排查异常
```

**警告消息示例**:
```
【警告】AI错误率偏高
━━━━━━━━━━━━━━━━━━
告警级别: WARNING
服务名称: ai-chat-service
当前错误率: 2.5%
阈值: 2.0%
开始时间: 2026-04-27 16:10:00
━━━━━━━━━━━━━━━━━━
处理建议:
1. 查看错误日志
2. 检查API依赖服务状态
```

#### 5.3.3 Python告警脚本（可选）

```python
import requests
import json

def send_wecom_alert(webhook_url, alert_data):
    """
    发送企业微信告警
    """
    message = {
        "msgtype": "markdown",
        "markdown": {
            "content": f"""
## 【{alert_data['severity']}】{alert_data['title']}

> 服务: {alert_data['service']}
> 描述: {alert_data['description']}
> 时间: {alert_data['time']}

---
处理建议:
{alert_data['suggestions']}
            """
        }
    }

    response = requests.post(webhook_url, json=message)
    return response.json()

# 使用示例
alert_data = {
    'severity': '严重',
    'title': 'AI推理延迟过高',
    'service': 'ai-approval-service',
    'description': 'P95延迟: 3245ms，阈值: 1500ms',
    'time': '2026-04-27 16:15:00',
    'suggestions': '1. 检查GPU负载\n2. 检查模型状态\n3. 查看日志'
}

send_wecom_alert(webhook_url, alert_data)
```

---

## 6. 监控系统部署方案

### 6.1 本地开发环境部署

#### 6.1.1 Docker Compose部署

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alerts:/etc/prometheus/alerts
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    depends_on:
      - prometheus

  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager-data:/alertmanager

  pushgateway:
    image: prom/pushgateway:latest
    ports:
      - "9091:9091"

volumes:
  prometheus-data:
  grafana-data:
  alertmanager-data:
```

**启动命令**:
```bash
docker-compose up -d
```

---

### 6.2 生产环境部署

#### 6.2.1 Kubernetes部署

**Prometheus部署**（使用Helm）:
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false
```

**自定义Prometheus配置**:
```yaml
# values.yaml
prometheus:
  prometheusSpec:
    serviceMonitorSelectorNilUsesHelmValues: false
    serviceMonitors:
      - name: ai-services
        selector:
          matchLabels:
            app: ai-services
        endpoints:
          - port: metrics
            interval: 10s
            path: /metrics
```

**部署自定义配置**:
```bash
helm upgrade prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  -f values.yaml
```

---

### 6.3 配置管理

#### 6.3.1 配置文件结构

```
monitoring/
├── prometheus/
│   ├── prometheus.yml
│   └── alerts/
│       └── ai-services.yml
├── grafana/
│   └── provisioning/
│       ├── datasources/
│       └── dashboards/
├── alertmanager/
│   └── alertmanager.yml
└── docker-compose.yml
```

#### 6.3.2 环境变量管理

**使用.env文件**:
```bash
# .env
AI_MONITOR_PASSWORD=secure_password_here
WECHAT_WEBHOOK_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
WECHAT_API_SECRET=your_api_secret_here
```

**在docker-compose.yml中引用**:
```yaml
services:
  prometheus:
    environment:
      - AI_MONITOR_PASSWORD=${AI_MONITOR_PASSWORD}
```

---

## 7. 监控系统测试方案

### 7.1 功能测试

#### 7.1.1 指标采集测试

**测试步骤**:
1. 启动AI服务
2. 发送测试请求
3. 访问http://ai-service:9090/metrics
4. 验证指标是否正确暴露

**验证标准**:
- [ ] 所有预定义指标都存在
- [ ] 指标值符合预期
- [ ] 指标标签正确

#### 7.1.2 告警测试

**测试步骤**:
1. 手动触发告警条件（如模拟高延迟）
2. 等待告警触发
3. 验证告警是否发送
4. 验证告警消息格式

**验证标准**:
- [ ] 告警及时触发
- [ ] 告警消息格式正确
- [ ] 企业微信通知成功
- [ ] 告警恢复后通知正常

#### 7.1.3 可视化测试

**测试步骤**:
1. 访问Grafana仪表盘
2. 检查图表数据
3. 验证查询语句

**验证标准**:
- [ ] 所有图表正常显示
- [ ] 数据实时更新
- [ ] 查询语句正确

---

### 7.2 性能测试

#### 7.2.1 采集性能测试

**测试工具**: Apache Bench (ab)

**测试场景**:
```bash
# 测试/metrics端点性能
ab -n 1000 -c 100 http://ai-service:9090/metrics
```

**性能指标**:
| 指标 | 目标值 |
|-----|-------|
| 平均响应时间 | < 100ms |
| P95响应时间 | < 200ms |
| QPS | > 1000 |

#### 7.2.2 存储性能测试

**测试工具**: Prometheus自带的性能检查

**检查命令**:
```bash
# 查看Prometheus性能指标
curl http://prometheus:9090/api/v1/label/__name__/values | jq .data[]
```

---

## 8. 监控系统运维指南

### 8.1 日常维护

#### 8.1.1 每日检查项

- [ ] 检查Prometheus和Grafana服务状态
- [ ] 检查磁盘空间（TSDB数据）
- [ ] 检查活跃告警
- [ ] 检查Grafana仪表盘数据完整性

#### 8.1.2 每周检查项

- [ ] 分析告警趋势
- [ ] 优化告警规则
- [ ] 检查存储使用情况
- [ ] 更新仪表盘配置

#### 8.1.3 每月检查项

- [ ] 评估监控系统性能
- [ ] 优化Prometheus配置
- [ ] 清理历史数据
- [ ] 更新监控最佳实践

---

### 8.2 故障排查

#### 8.2.1 常见问题

**问题1: Prometheus无法抓取指标**

**排查步骤**:
1. 检查目标服务是否正常运行
2. 检查/metrics端点是否可访问
3. 检查Prometheus抓取配置
4. 查看Prometheus日志

**问题2: 告警未发送**

**排查步骤**:
1. 检查Alertmanager状态
2. 检查告警规则是否正确加载
3. 检查企业微信Webhook配置
4. 查看Alertmanager日志

**问题3: Grafana无法显示数据**

**排查步骤**:
1. 检查Prometheus数据源配置
2. 检查查询语句是否正确
3. 检查时间范围是否正确
4. 查看Grafana日志

---

### 8.3 性能优化

#### 8.3.1 Prometheus优化

- 调整抓取间隔（平衡精度和性能）
- 使用指标过滤（减少不必要指标）
- 优化查询语句（避免复杂查询）
- 调整存储保留策略

#### 8.3.2 Grafana优化

- 使用变量（减少重复查询）
- 启用缓存（提升查询速度）
- 优化仪表盘（减少面板数量）
- 使用预聚合数据

---

## 9. 附录

### 9.1 工具清单

| 工具 | 版本 | 用途 |
|-----|------|------|
| Prometheus | 2.45.0 | 监控数据采集和存储 |
| Grafana | 10.0.0 | 监控数据可视化 |
| Alertmanager | 0.26.0 | 告警管理 |
| Pushgateway | 1.6.0 | 临时指标推送 |
| Micrometer | 1.11.0 | 应用指标埋点 |

### 9.2 参考文档

- Prometheus官方文档: https://prometheus.io/docs/
- Grafana官方文档: https://grafana.com/docs/
- Micrometer文档: https://micrometer.io/docs

### 9.3 联系方式

- AI-Ready团队邮箱: ai-team@example.com
- 企业微信群组: AI服务监控群
- 紧急联系人: 待定

---

**文档作者**: AI-Ready测试团队
**审核状态**: 待审核
**下一步**: 等待审核和反馈