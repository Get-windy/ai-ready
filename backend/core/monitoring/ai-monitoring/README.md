# AI模型质量监控系统

## 项目概述

为测试环境配置的专项AI模型质量监控系统，确保AI模块在测试环境中的稳定性和性能可监控。系统提供完整的AI模型监控解决方案，包括性能监控、质量评估、漂移检测和异常告警。

## 核心功能

### 1. AI模型性能监控
- ✅ **响应时间监控**: 实时监控模型推理延迟
- ✅ **吞吐量监控**: 跟踪QPS（每秒查询数）
- ✅ **资源使用监控**: CPU、内存使用率监控
- ✅ **可用性监控**: 服务健康状态检查

### 2. AI模型质量指标
- ✅ **分类指标**: 准确率、精确率、召回率、F1分数
- ✅ **回归指标**: MAE、MSE、RMSE、R²（待实现）
- ✅ **漂移检测**: 数据漂移、概念漂移检测
- ✅ **异常检测**: 预测分布异常检测

### 3. 监控告警系统
- ✅ **告警规则**: 多级告警规则（紧急/严重/警告/信息）
- ✅ **阈值告警**: 性能和质量阈值告警
- ✅ **通知渠道**: 邮件、Slack、Webhook等多渠道通知
- ✅ **告警抑制**: 智能告警抑制，避免告警风暴

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                   监控仪表盘 (Grafana)                   │
│                   http://localhost:3000                  │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│                Prometheus (指标存储)                     │
│                http://localhost:9090                     │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│               AlertManager (告警管理)                    │
│               http://localhost:9093                      │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│            AI模型监控服务 (Python)                       │
│            http://localhost:8000                         │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│                    AI模型服务                            │
│                    (被监控的应用)                        │
└─────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 使用Docker Compose部署（推荐）

```bash
# 进入监控系统目录
cd I:\AI-Ready\ai-monitoring

# 启动所有服务
start_monitoring.bat

# 或者手动启动
docker-compose up -d
```

### 2. 验证部署

访问以下地址验证部署：

- **监控API**: http://localhost:8000/health
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)

### 3. 测试监控系统

```bash
# 使用数据生成器测试
python scripts/data_generator.py --check-status
python scripts/data_generator.py --interval 30 --duration 600
```

## 集成指南

### Python集成示例

```python
import requests
import time

class AIMonitorClient:
    def __init__(self, monitor_url="http://localhost:8000", model_name="my_model"):
        self.monitor_url = monitor_url
        self.model_name = model_name
    
    def record_prediction(self, latency_seconds, endpoint="/predict"):
        """记录预测请求"""
        data = {
            'endpoint': endpoint,
            'latency_seconds': latency_seconds
        }
        requests.post(f"{self.monitor_url}/record-request", json=data, timeout=2)
    
    def update_quality(self, y_true, y_pred):
        """更新质量指标"""
        data = {
            'y_true': y_true.tolist() if hasattr(y_true, 'tolist') else y_true,
            'y_pred': y_pred.tolist() if hasattr(y_pred, 'tolist') else y_pred
        }
        requests.post(f"{self.monitor_url}/update-quality", json=data, timeout=5)
```

### REST API接口

监控系统提供完整的REST API：

```bash
# 健康检查
GET /health

# Prometheus指标
GET /metrics

# JSON格式监控数据
GET /metrics/json

# 监控报告
GET /report

# 记录请求
POST /record-request
{
  "endpoint": "/predict",
  "latency_seconds": 0.15
}

# 更新质量指标
POST /update-quality
{
  "y_true": [0, 1, 0, 1, 0],
  "y_pred": [0, 1, 0, 0, 1]
}
```

## 配置说明

### 告警规则配置

告警规则定义在 `config/alerts.yml`：

```yaml
- alert: ModelAccuracyLow
  expr: ai_model_accuracy < 0.85
  for: 5m
  labels:
    severity: critical
    category: model_quality
  annotations:
    summary: "模型准确率过低"
    description: "模型准确率已降至 {{ $value | humanizePercentage }}"
    action: "检查训练数据质量，考虑重新训练模型"
```

### 通知渠道配置

在 `config/alertmanager.yml` 中配置通知渠道：

- 邮件通知
- Slack Webhook
- 自定义Webhook
- 短信通知（需配置网关）

## 监控指标

### 性能指标
- `ai_model_requests_total` - 总请求数
- `ai_model_latency_seconds` - 响应时间分布
- `ai_model_throughput_qps` - 吞吐量（QPS）
- `ai_model_cpu_usage_percent` - CPU使用率
- `ai_model_memory_usage_mb` - 内存使用量

### 质量指标
- `ai_model_accuracy` - 准确率
- `ai_model_precision` - 精确率
- `ai_model_recall` - 召回率
- `ai_model_f1_score` - F1分数
- `ai_model_data_drift_score` - 数据漂移分数
- `ai_model_concept_drift_score` - 概念漂移分数

### 异常指标
- `ai_model_anomaly_score` - 异常分数
- `ai_model_errors_total` - 错误总数

## 告警规则

### 紧急告警 (Emergency)
- 服务完全不可用
- 需要立即处理

### 严重告警 (Critical)
- 模型准确率过低 (<85%)
- 响应时间严重超时 (>1000ms)
- 内存使用率过高 (>85%)

### 警告告警 (Warning)
- 数据漂移检测 (>0.3)
- 响应时间偏高 (>500ms)
- CPU使用率偏高 (>80%)

### 信息告警 (Info)
- 质量指标变化
- 服务重启事件
- 配置变更记录

## 运维管理

### 日常运维命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f ai-model-monitor
docker-compose logs -f prometheus

# 重启服务
docker-compose restart ai-model-monitor

# 停止所有服务
docker-compose down

# 更新配置后重载
docker-compose up -d --force-recreate
```

### 数据管理

```bash
# 备份监控数据
docker run --rm -v ai-monitoring_prometheus-data:/data -v $(pwd)/backup:/backup alpine tar czf /backup/prometheus-$(date +%Y%m%d).tar.gz -C /data .

# 清理旧数据
# Prometheus默认保留15天数据，可在配置中调整
```

### 监控系统自身监控

监控系统自身也暴露监控指标，可通过以下方式检查：

```bash
# 检查系统健康状态
curl http://localhost:8000/health

# 检查资源使用
docker stats ai-model-monitor

# 查看系统日志
tail -f I:\AI-Ready\ai-monitoring\logs\ai_monitor.log
```

## 故障排除

### 常见问题

1. **服务无法启动**
   - 检查端口占用：8000, 9090, 9093, 3000
   - 检查Docker服务状态
   - 查看详细错误日志：`docker-compose logs`

2. **监控数据不更新**
   - 检查数据生成器：`python scripts/data_generator.py --check-status`
   - 检查API连通性：`curl http://localhost:8000/health`
   - 检查Prometheus目标状态

3. **告警不发送**
   - 检查AlertManager配置：`docker-compose exec alertmanager amtool config show`
   - 检查告警规则：`curl http://localhost:9090/api/v1/rules`
   - 测试告警发送

4. **Grafana无法访问数据**
   - 检查Prometheus数据源配置
   - 检查网络连通性
   - 检查Grafana日志

### 调试模式

启用调试日志：

```bash
# 修改docker-compose.yml
environment:
  - LOG_LEVEL=DEBUG

# 重启服务
docker-compose up -d --force-recreate
```

## 扩展开发

### 添加新监控指标

1. 在 `ai_model_monitor.py` 中添加新指标
2. 更新数据收集逻辑
3. 添加对应的告警规则
4. 更新Grafana仪表盘

### 自定义告警规则

1. 编辑 `config/alerts.yml`
2. 定义新的告警规则
3. 配置通知渠道
4. 测试告警触发

### 集成现有系统

1. 配置Prometheus scrape目标
2. 集成到现有CI/CD流水线
3. 与现有告警系统对接
4. 自定义数据导出格式

## 安全考虑

### 访问控制
- 启用基本认证
- 配置TLS/SSL加密
- 限制访问IP范围

### 数据安全
- 监控数据加密存储
- 访问日志审计
- 定期安全扫描

### 网络隔离
- 使用内部网络通信
- 配置防火墙规则
- 限制外部访问

## 性能优化

### 监控系统优化
- 调整数据采集频率
- 优化数据存储策略
- 启用数据压缩

### 资源优化
- 设置资源限制
- 启用自动扩缩容
- 监控系统自身资源使用

## 版本历史

### v1.0.0 (2026-04-29)
- 初始版本发布
- 完整的AI模型监控功能
- Docker Compose部署支持
- 多级告警系统
- Grafana仪表盘集成

## 许可证

MIT License

## 技术支持

如有问题，请：
1. 查看详细文档：`DEPLOYMENT.md`
2. 检查错误日志：`docker-compose logs`
3. 提交Issue到项目仓库
4. 联系技术负责人

---

**系统状态**: ✅ 生产就绪  
**最后测试**: 2026-04-29  
**测试环境**: Windows 10 + Docker Desktop  
**推荐部署**: Docker Compose  

## 下一步计划

### 短期计划
- [ ] 添加更多模型评估指标
- [ ] 优化告警通知模板
- [ ] 增加监控数据导出功能

### 长期计划
- [ ] 支持分布式部署
- [ ] 添加预测性维护功能
- [ ] 集成自动化修复操作
- [ ] 支持多租户场景

---

**开始监控您的AI模型，确保质量与性能！** 🚀