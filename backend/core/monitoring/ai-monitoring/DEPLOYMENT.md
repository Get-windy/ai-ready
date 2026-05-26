# AI模型质量监控系统部署指南

## 系统概述

AI模型质量监控系统是为测试环境配置的专项监控系统，用于确保AI模块在测试环境中的稳定性和性能可监控。系统包括：

1. **AI模型性能监控** - 响应时间、吞吐量、资源使用
2. **AI模型质量指标** - 准确率、召回率、F1分数、漂移检测
3. **监控告警系统** - 性能阈值告警、质量指标异常告警

## 部署要求

### 硬件要求
- CPU: 2核心以上
- 内存: 4GB以上
- 存储: 10GB可用空间

### 软件要求
- Docker 20.10+
- Docker Compose 2.0+
- Python 3.9+ (仅用于本地开发)

### 网络要求
- 端口 8000: 监控API服务
- 端口 9090: Prometheus
- 端口 9093: AlertManager
- 端口 3000: Grafana

## 快速部署

### 1. 使用Docker Compose部署（推荐）

```bash
# 进入监控系统目录
cd I:\AI-Ready\ai-monitoring

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 2. 验证部署

```bash
# 检查监控API服务
curl http://localhost:8000/health

# 检查Prometheus
curl http://localhost:9090/-/healthy

# 检查Grafana
curl http://localhost:3000/api/health
```

### 3. 访问Web界面

- **监控API**: http://localhost:8000
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093
- **Grafana**: http://localhost:3000
  - 用户名: admin
  - 密码: admin123

## 配置说明

### 1. 环境变量配置

在 `docker-compose.yml` 中可配置以下环境变量：

```yaml
environment:
  - MODEL_NAME=your_model_name      # 模型名称
  - MONITOR_PORT=8000               # 监控API端口
  - PROMETHEUS_URL=http://prometheus:9090
  - ALERTMANAGER_URL=http://alertmanager:9093
  - LOG_LEVEL=INFO                  # 日志级别
```

### 2. 告警规则配置

告警规则定义在 `config/alerts.yml` 中，包括：

- 模型准确率告警
- 响应时间告警
- 资源使用告警
- 漂移检测告警
- 服务可用性告警

### 3. 通知渠道配置

在 `config/alertmanager.yml` 中配置告警通知：

1. **邮件通知**: 配置SMTP服务器
2. **Slack通知**: 配置Webhook URL
3. **Webhook通知**: 自定义Webhook端点
4. **短信通知**: 配置短信网关

## 监控系统使用

### 1. 集成AI模型

#### Python集成示例

```python
import requests
import time

class AIModelWithMonitoring:
    def __init__(self, monitor_url="http://localhost:8000", model_name="my_model"):
        self.monitor_url = monitor_url
        self.model_name = model_name
    
    def predict(self, data):
        # 记录开始时间
        start_time = time.time()
        
        try:
            # 执行模型预测
            result = self._do_predict(data)
            
            # 计算延迟
            latency = time.time() - start_time
            
            # 记录请求到监控系统
            self._record_request(latency)
            
            return result
            
        except Exception as e:
            # 记录错误
            self._record_error(str(e))
            raise
    
    def _record_request(self, latency):
        """记录请求到监控系统"""
        data = {
            'endpoint': '/predict',
            'latency_seconds': latency
        }
        
        try:
            requests.post(
                f"{self.monitor_url}/record-request",
                json=data,
                timeout=2
            )
        except:
            pass  # 监控不可用时不中断主流程
    
    def update_quality_metrics(self, y_true, y_pred):
        """更新质量指标"""
        data = {
            'y_true': y_true.tolist() if hasattr(y_true, 'tolist') else y_true,
            'y_pred': y_pred.tolist() if hasattr(y_pred, 'tolist') else y_pred
        }
        
        try:
            requests.post(
                f"{self.monitor_url}/update-quality",
                json=data,
                timeout=5
            )
        except:
            pass
```

#### REST API集成

监控系统提供以下API端点：

```bash
# 记录模型请求
POST /record-request
{
  "endpoint": "/predict",
  "latency_seconds": 0.15
}

# 更新质量指标
POST /update-quality
{
  "y_true": [0, 1, 0, 1, 0],
  "y_pred": [0, 1, 0, 0, 1],
  "y_prob": [[0.9, 0.1], [0.2, 0.8], [0.8, 0.2], [0.3, 0.7], [0.6, 0.4]]
}

# 获取监控数据
GET /metrics/json

# 获取监控报告
GET /report

# 健康检查
GET /health

# Prometheus指标
GET /metrics
```

### 2. 使用数据生成器测试

```bash
# 启动数据生成器（需要先启动监控系统）
python scripts/data_generator.py

# 带参数运行
python scripts/data_generator.py \
  --monitor-url http://localhost:8000 \
  --model-name test_model \
  --interval 30 \
  --duration 3600  # 运行1小时

# 只检查监控状态
python scripts/data_generator.py --check-status
```

### 3. 查看监控数据

#### 通过API查看

```bash
# 获取JSON格式监控数据
curl http://localhost:8000/metrics/json | jq .

# 获取监控报告
curl http://localhost:8000/report | jq .

# 获取Prometheus指标
curl http://localhost:8000/metrics
```

#### 通过Grafana查看

1. 访问 http://localhost:3000
2. 使用 admin/admin123 登录
3. 导入预配置的仪表盘
4. 查看各项监控指标

预配置仪表盘包括：
- AI模型性能监控
- 质量指标趋势
- 资源使用情况
- 告警统计

## 告警管理

### 1. 告警级别

系统定义四级告警级别：

1. **紧急 (Emergency)**: 服务完全不可用，需要立即处理
2. **严重 (Critical)**: 关键功能受影响，需要尽快处理
3. **警告 (Warning)**: 潜在问题，需要关注
4. **信息 (Info)**: 一般信息，仅记录不通知

### 2. 告警处理流程

1. **告警触发**: 监控系统检测到异常
2. **告警发送**: 通过配置的渠道发送告警
3. **告警确认**: 值班人员确认告警
4. **问题处理**: 技术人员处理问题
5. **告警解决**: 问题解决后关闭告警
6. **事后分析**: 分析告警原因，优化系统

### 3. 告警抑制规则

系统配置了告警抑制规则：

- 紧急告警抑制相同模型的警告告警
- 服务不可用告警抑制性能告警
- 避免重复告警和告警风暴

## 运维管理

### 1. 日常维护

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f ai-model-monitor
docker-compose logs -f prometheus
docker-compose logs -f alertmanager

# 重启服务
docker-compose restart ai-model-monitor

# 更新配置后重载
docker-compose up -d --force-recreate
```

### 2. 数据管理

```bash
# 查看数据卷使用情况
docker volume ls
docker volume inspect ai-monitoring_prometheus-data

# 备份监控数据
docker run --rm -v ai-monitoring_prometheus-data:/data -v $(pwd)/backup:/backup alpine tar czf /backup/prometheus-$(date +%Y%m%d).tar.gz -C /data .

# 清理旧数据（Prometheus自动管理）
# 默认保留15天数据，可在prometheus.yml中配置
```

### 3. 监控系统自身监控

监控系统自身也暴露监控指标：

```bash
# 监控系统健康状态
curl http://localhost:8000/health

# 监控系统资源使用
docker stats ai-model-monitor prometheus alertmanager grafana

# 监控系统日志
tail -f I:\AI-Ready\ai-monitoring\logs\ai_monitor.log
```

## 故障排除

### 常见问题

#### 1. 监控服务无法启动

```bash
# 检查端口占用
netstat -ano | findstr :8000
netstat -ano | findstr :9090
netstat -ano | findstr :3000

# 检查Docker服务状态
docker info
docker-compose version

# 查看详细错误日志
docker-compose logs --tail=100 ai-model-monitor
```

#### 2. 监控数据不更新

```bash
# 检查数据生成器
python scripts/data_generator.py --check-status

# 检查API连通性
curl http://localhost:8000/health
curl http://localhost:8000/metrics

# 检查Prometheus目标
curl http://localhost:9090/api/v1/targets | jq .
```

#### 3. 告警不发送

```bash
# 检查AlertManager配置
docker-compose exec alertmanager amtool config show

# 检查告警规则
curl http://localhost:9090/api/v1/rules | jq .

# 测试告警
docker-compose exec alertmanager amtool alert add \
  alertname=TestAlert \
  severity=warning \
  --annotation=description="测试告警"
```

#### 4. Grafana无法访问Prometheus

```bash
# 检查Prometheus状态
curl http://localhost:9090/-/healthy

# 检查Grafana数据源配置
# 访问 http://localhost:3000/datasources
# 检查Prometheus数据源连接
```

### 调试模式

启用调试日志：

```bash
# 修改docker-compose.yml
environment:
  - LOG_LEVEL=DEBUG

# 重启服务
docker-compose up -d --force-recreate
```

## 扩展和定制

### 1. 添加新监控指标

修改 `ai_model_monitor.py`：

```python
# 添加新指标
new_metric = Gauge(
    'ai_model_custom_metric',
    '自定义监控指标',
    ['model_name', 'metric_type'],
    registry=self.registry
)

# 更新指标值
new_metric.labels(model_name=self.model_name, metric_type='custom').set(value)
```

### 2. 添加新告警规则

修改 `config/alerts.yml`：

```yaml
- alert: CustomAlert
  expr: ai_model_custom_metric > threshold_value
  for: 5m
  labels:
    severity: warning
    category: custom
  annotations:
    summary: "自定义告警"
    description: "自定义指标 {{ $labels.metric_type }} 值为 {{ $value }}"
    action: "检查相关配置"
```

### 3. 集成现有监控系统

#### 集成到Prometheus

在现有Prometheus配置中添加：

```yaml
scrape_configs:
  - job_name: 'ai-model-monitoring'
    static_configs:
      - targets: ['ai-model-monitor:8000']
    metrics_path: '/metrics'
```

#### 集成到Grafana

添加新的数据源：
1. 类型: Prometheus
2. URL: http://prometheus:9090
3. 导入预配置的AI监控仪表盘

## 性能优化

### 1. 监控系统优化

```yaml
# 调整数据保留策略
global:
  scrape_interval: 30s  # 增加采集间隔
  evaluation_interval: 30s

# 调整存储保留时间
rule_files:
  - 'alerts.yml'

storage:
  tsdb:
    retention: 30d  # 保留30天数据
```

### 2. 资源限制

在 `docker-compose.yml` 中添加资源限制：

```yaml
deploy:
  resources:
    limits:
      cpus: '1'
      memory: 2G
    reservations:
      cpus: '0.5'
      memory: 1G
```

### 3. 高可用部署

对于生产环境，建议：

1. **多实例部署**: 部署多个监控实例
2. **负载均衡**: 使用负载均衡器分发请求
3. **数据持久化**: 使用外部存储卷
4. **备份策略**: 定期备份配置和数据

## 安全考虑

### 1. 访问控制

```yaml
# 启用基本认证
environment:
  - BASIC_AUTH_USERNAME=admin
  - BASIC_AUTH_PASSWORD=secure_password

# 配置TLS/SSL
ports:
  - "8443:8443"
environment:
  - SSL_CERT_PATH=/path/to/cert.pem
  - SSL_KEY_PATH=/path/to/key.pem
```

### 2. 网络隔离

```yaml
networks:
  monitoring-internal:
    internal: true  # 内部网络，外部无法访问
  monitoring-external:
    driver: bridge  # 外部网络，可访问互联网
```

### 3. 数据保护

- 监控数据加密存储
- 访问日志记录和审计
- 定期安全扫描和更新

## 附录

### 监控指标列表

| 指标名称 | 类型 | 描述 | 告警阈值 |
|---------|------|------|----------|
| ai_model_requests_total | Counter | 总请求数 | - |
| ai_model_latency_seconds | Histogram | 响应时间 | >1.0s |
| ai_model_accuracy | Gauge | 准确率 | <0.85 |
| ai_model_data_drift_score | Gauge | 数据漂移分数 | >0.3 |
| ai_model_cpu_usage_percent | Gauge | CPU使用率 | >80% |

### 告警规则列表

| 告警名称 | 触发条件 | 严重程度 | 处理建议 |
|---------|----------|----------|----------|
| ModelAccuracyLow | 准确率<85% | Critical | 重新训练模型 |
| ModelLatencyHigh | P95延迟>500ms | Warning | 优化模型或增加资源 |
| DataDriftDetected | 数据漂移>0.3 | Warning | 检查数据质量 |
| ServiceUnavailable | 服务不可用 | Emergency | 立即重启服务 |

### 联系支持

如遇问题，请联系：
- 技术负责人: AI-Ready Team
- 文档: I:\AI-Ready\ai-monitoring\README.md
- 问题反馈: 创建GitHub Issue

---

**最后更新**: 2026-04-29  
**版本**: 1.0.0  
**状态**: 生产就绪