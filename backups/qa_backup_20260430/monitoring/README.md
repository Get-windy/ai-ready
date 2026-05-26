# API接口监控脚本

## 概述

本目录包含 Sprint 27+1 测试环境API接口监控脚本，用于监控API服务的可用性、性能和错误率。

## 脚本列表

### 1. API监控脚本 (`api-monitor.py`)

**用途**: 监控测试环境API服务的可用性、性能和错误率

**功能**:
- 定期监控API端点（默认30秒间隔）
- 收集响应时间、状态码、错误信息
- 计算错误率、可用性、响应时间统计（P50/P95/P99）
- 支持自定义告警规则
- 导出Prometheus格式指标
- 生成JSON格式监控报告

**使用方法**:
```bash
python api-monitor.py [选项]
```

**参数**:
- `--base-url`: API基础URL（默认: http://localhost:8080）
- `--interval`: 检查间隔，单位秒（默认: 30）
- `--prometheus-port`: Prometheus指标端口（默认: 9091）
- `--config`: 配置文件路径（默认: api-monitor-config.json）
- `--report`: 报告输出文件（默认: api-monitor-report.json）
- `--daemon`: 后台运行模式
- `--once`: 只执行一次检查
- `--verbose`: 显示详细日志

**示例**:
```bash
# 单次检查
python api-monitor.py --once --verbose

# 后台运行
python api-monitor.py --daemon --interval 60

# 自定义配置
python api-monitor.py --config custom-config.json --report custom-report.json
```

### 2. Prometheus配置 (`config/prometheus-api.yml`)

**用途**: Prometheus监控系统配置

**功能**:
- 定义API监控指标采集任务
- 配置告警规则文件
- 设置采集间隔和目标

### 3. 告警规则 (`config/api-alert-rules.yml`)

**用途**: 定义API监控告警规则

**告警规则**:
- `APIHighResponseTime`: 响应时间超过1秒（Warning）
- `APICriticalResponseTime`: 响应时间超过3秒（Critical）
- `APIHighErrorRate`: 错误率超过5%（Warning）
- `APICriticalErrorRate`: 错误率超过20%（Critical）
- `APILowAvailability`: 可用性低于95%（Critical）
- `APIServiceDown`: 监控服务宕机（Critical）
- `APIEndpointDown`: API端点不可用（Critical）

### 4. Grafana仪表盘 (`config/grafana-api-dashboard.json`)

**用途**: Grafana监控仪表盘配置

**面板**:
- API可用性（百分比）
- 平均响应时间（毫秒）
- 请求总量
- 错误率（百分比）
- 响应时间趋势图
- 请求量趋势图
- 错误率趋势图
- HTTP状态码分布饼图
- 各端点性能概览表格

### 5. 监控配置 (`config/api-monitor-config.json`)

**用途**: API监控配置文件示例

**配置项**:
- 基础URL
- 检查间隔
- 监控端点列表
- 告警规则

## 目录结构

```
qa/monitoring/
├── api-monitor.py                        # API监控脚本
├── README.md                             # 本文档
├── config/
│   ├── prometheus-api.yml               # Prometheus配置
│   ├── api-alert-rules.yml              # 告警规则
│   ├── grafana-api-dashboard.json       # Grafana仪表盘
│   └── api-monitor-config.json          # 监控配置示例
└── logs/                                 # 日志目录
    ├── api-monitor.log                  # 监控日志
    └── api-monitor-report.json          # 监控报告
```

## 监控指标

### Prometheus指标

| 指标名 | 类型 | 说明 |
|--------|------|------|
| api_response_time_ms | Gauge | API响应时间（毫秒） |
| api_requests_total | Counter | API请求总数 |
| api_errors_total | Counter | API错误总数 |

### 统计指标

| 指标 | 说明 |
|------|------|
| total_requests | 总请求数 |
| error_count | 错误数 |
| error_rate | 错误率 |
| availability | 可用性 |
| avg_response_time | 平均响应时间 |
| min_response_time | 最小响应时间 |
| max_response_time | 最大响应时间 |
| p50_response_time | P50响应时间 |
| p95_response_time | P95响应时间 |
| p99_response_time | P99响应时间 |

## 使用流程

### 1. 配置监控端点

编辑 `config/api-monitor-config.json`，添加需要监控的API端点：

```json
{
  "endpoints": [
    {
      "path": "/api/health",
      "method": "GET",
      "name": "health_check"
    }
  ]
}
```

### 2. 启动监控

```bash
# 后台运行模式
python api-monitor.py --daemon --interval 30

# 或单次检查模式
python api-monitor.py --once --verbose
```

### 3. 配置Prometheus

将 `config/prometheus-api.yml` 添加到Prometheus配置中：

```bash
# 复制配置文件
cp config/prometheus-api.yml /etc/prometheus/

# 重启Prometheus
sudo systemctl restart prometheus
```

### 4. 导入Grafana仪表盘

1. 登录Grafana
2. 进入 "Configuration" > "Data Sources"
3. 添加Prometheus数据源
4. 进入 "Create" > "Import"
5. 上传 `config/grafana-api-dashboard.json`

### 5. 查看监控报告

```bash
# 查看JSON报告
cat api-monitor-report.json

# 或查看日志
tail -f api-monitor.log
```

## 告警通知

### 配置Alertmanager

在 `prometheus-api.yml` 中配置Alertmanager：

```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### 告警渠道

支持以下告警渠道：
- 邮件
- 企业微信
- 钉钉
- Slack
- Webhook

## 依赖

### Python依赖
```bash
pip install requests
```

### 系统依赖
- Python 3.8+
- Prometheus（可选，用于指标采集）
- Grafana（可选，用于可视化）

## 故障排除

### 常见问题

1. **监控脚本无法启动**
   - 检查Python版本（需要3.8+）
   - 检查依赖是否安装：`pip install requests`

2. **Prometheus无法采集指标**
   - 检查Prometheus配置是否正确
   - 确认指标文件路径：`api-metrics.prom`
   - 检查文件权限

3. **Grafana仪表盘无数据**
   - 确认Prometheus数据源配置正确
   - 检查指标名称是否匹配
   - 查看时间范围设置

4. **告警不触发**
   - 检查告警规则表达式
   - 确认Alertmanager配置
   - 查看Prometheus告警状态

## 性能优化

### 大规模监控

对于大量API端点的监控，建议：

1. **增加检查间隔**: `--interval 60` 或更大
2. **使用批量检查**: 将相关端点分组
3. **启用指标缓存**: 减少重复计算
4. **分布式监控**: 使用多个监控实例

### 资源使用

| 端点数量 | 内存使用 | CPU使用 |
|----------|----------|---------|
| 1-10     | ~50MB    | ~1%     |
| 10-50    | ~100MB   | ~2%     |
| 50-100   | ~200MB   | ~5%     |

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-04-28 | 初始版本，包含基础监控功能 |

## 维护者

- **团队**: AI-Ready QA Team
- **项目**: Sprint 27+1: 测试环境配置专项
