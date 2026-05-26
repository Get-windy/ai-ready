# 资源监控与成本分析模块

## 项目概述
为Sprint 27+1测试环境开发的资源监控与成本分析模块，提供基础设施、应用服务和容器的资源使用监控，以及成本分析和优化建议。

## 功能特性

### 1. 资源监控
- **基础设施监控**：CPU、内存、磁盘、网络使用率
- **应用服务监控**：Java应用、Python服务、数据库、消息队列
- **容器监控**：Docker容器资源使用（CPU、内存、网络、存储）
- **Kubernetes监控**：Pod、Node、Namespace资源使用

### 2. 成本分析
- **资源成本计算**：基于云服务商定价或内部成本模型
- **成本趋势分析**：历史成本变化趋势
- **成本优化建议**：资源使用优化建议
- **预算预测**：未来成本预测

### 3. 可视化
- **实时监控仪表板**：资源使用实时视图
- **历史趋势图表**：资源使用历史趋势
- **成本分析报表**：成本明细和汇总报表
- **告警面板**：资源异常告警

## 技术架构

### 数据采集层
- **Prometheus**：监控数据采集和存储
- **Node Exporter**：主机指标采集
- **cAdvisor**：容器指标采集
- **数据库导出器**：MySQL、PostgreSQL、Redis监控
- **应用导出器**：Spring Boot Actuator、自定义指标

### 数据处理层
- **Python数据处理**：Pandas + NumPy
- **数据分析**：资源使用分析、成本计算
- **机器学习**：异常检测、趋势预测

### 数据存储层
- **Prometheus TSDB**：监控数据存储
- **PostgreSQL**：成本数据和元数据
- **Redis**：缓存和实时数据

### 可视化层
- **Grafana**：监控仪表板
- **Vue.js前端**：自定义监控界面
- **ECharts**：数据可视化图表

### 告警层
- **Alertmanager**：告警管理
- **智能告警**：基于机器学习的异常检测告警
- **通知渠道**：邮件、Slack、Webhook

## 项目结构

```
resource-monitoring-cost-analysis/
├── README.md                          # 项目说明
├── requirements.txt                   # Python依赖
├── docker-compose.yml                 # Docker编排
├── config/                            # 配置文件
│   ├── prometheus/                    # Prometheus配置
│   ├── grafana/                       # Grafana配置
│   ├── alertmanager/                  # Alertmanager配置
│   └── cost-analysis/                 # 成本分析配置
├── src/                               # 源代码
│   ├── data_collectors/               # 数据采集器
│   ├── data_processors/               # 数据处理
│   ├── cost_calculators/              # 成本计算器
│   ├── analyzers/                     # 分析器
│   ├── visualizers/                   # 可视化
│   └── alerts/                        # 告警模块
├── tests/                             # 测试
│   ├── unit/                          # 单元测试
│   ├── integration/                   # 集成测试
│   └── performance/                   # 性能测试
├── docs/                              # 文档
│   ├── api/                           # API文档
│   ├── deployment/                    # 部署文档
│   └── user_guide/                    # 用户指南
└── scripts/                           # 脚本
    ├── deploy/                        # 部署脚本
    ├── monitoring/                    # 监控脚本
    └── analysis/                      # 分析脚本
```

## 安装部署

### 环境要求
- Python 3.8+
- Node.js 16+
- Docker 20.10+
- Prometheus 2.30+
- Grafana 8.0+

### 快速开始
```bash
# 克隆项目
git clone <repository-url>

# 安装Python依赖
pip install -r requirements.txt

# 启动监控栈
docker-compose up -d

# 启动成本分析服务
python src/main.py
```

## 配置说明

### 监控配置
1. 在`config/prometheus/`中配置监控目标
2. 在`config/grafana/`中配置数据源和仪表板
3. 在`config/alertmanager/`中配置告警规则

### 成本配置
1. 在`config/cost-analysis/cost-models.yml`中配置成本模型
2. 在`config/cost-analysis/pricing.yml`中配置定价
3. 在`config/cost-analysis/optimization-rules.yml`中配置优化规则

## API接口

### 监控API
- `GET /api/v1/metrics/resource` - 获取资源监控数据
- `GET /api/v1/metrics/application` - 获取应用监控数据
- `GET /api/v1/metrics/container` - 获取容器监控数据

### 成本API
- `GET /api/v1/cost/summary` - 获取成本汇总
- `GET /api/v1/cost/details` - 获取成本明细
- `GET /api/v1/cost/trend` - 获取成本趋势
- `GET /api/v1/cost/optimization` - 获取优化建议

### 分析API
- `GET /api/v1/analysis/resource` - 资源使用分析
- `GET /api/v1/analysis/performance` - 性能分析
- `GET /api/v1/analysis/anomaly` - 异常检测

## 使用示例

### Python客户端
```python
from resource_monitoring_client import ResourceMonitoringClient

client = ResourceMonitoringClient(base_url="http://localhost:8000")

# 获取资源监控数据
resource_metrics = client.get_resource_metrics()

# 获取成本分析
cost_analysis = client.get_cost_analysis()

# 获取优化建议
optimization = client.get_optimization_suggestions()
```

### 命令行工具
```bash
# 查看资源使用
python -m resource_monitoring.cli resource --time-range 1h

# 计算成本
python -m resource_monitoring.cli cost --period month

# 生成优化报告
python -m resource_monitoring.cli optimize --output report.pdf
```

## 监控指标

### 基础设施指标
- `node_cpu_usage` - CPU使用率
- `node_memory_usage` - 内存使用率
- `node_disk_usage` - 磁盘使用率
- `node_network_io` - 网络IO

### 应用指标
- `app_cpu_usage` - 应用CPU使用率
- `app_memory_usage` - 应用内存使用率
- `app_request_rate` - 请求速率
- `app_error_rate` - 错误率

### 容器指标
- `container_cpu_usage` - 容器CPU使用率
- `container_memory_usage` - 容器内存使用率
- `container_network_io` - 容器网络IO
- `container_disk_io` - 容器磁盘IO

### 成本指标
- `cost_per_hour` - 每小时成本
- `cost_per_day` - 每天成本
- `cost_per_month` - 每月成本
- `cost_optimization_potential` - 成本优化潜力

## 告警规则

### 资源告警
- CPU使用率 > 80% 持续5分钟
- 内存使用率 > 85% 持续5分钟
- 磁盘使用率 > 90%
- 网络带宽使用率 > 75%

### 成本告警
- 日成本超过预算50%
- 周成本增长超过20%
- 资源浪费率 > 30%

### 性能告警
- 应用响应时间 > 500ms
- 错误率 > 1%
- 可用性 < 99.9%

## 成本模型

### 云服务成本
```yaml
aws:
  ec2:
    t3.micro: 0.0104  # 美元/小时
    t3.small: 0.0208
    t3.medium: 0.0416
  rds:
    db.t3.micro: 0.017
    db.t3.small: 0.034
```

### 内部成本
```yaml
internal:
  server:
    physical: 1000    # 美元/月
    virtual: 200      # 美元/月
  storage:
    hdd: 0.03         # 美元/GB/月
    ssd: 0.10         # 美元/GB/月
  network:
    bandwidth: 0.05   # 美元/GB
```

## 优化建议

### 资源优化
1. **CPU优化**：调整实例类型，启用自动扩展
2. **内存优化**：优化应用内存配置，使用缓存
3. **存储优化**：清理无用数据，使用分层存储
4. **网络优化**：压缩数据，使用CDN

### 成本优化
1. **预留实例**：使用预留实例降低长期成本
2. **竞价实例**：使用竞价实例处理非关键工作负载
3. **自动缩放**：根据负载自动调整资源
4. **资源回收**：定期清理无用资源

## 开发指南

### 添加新的监控指标
1. 在`src/data_collectors/`中添加数据采集器
2. 在`src/data_processors/`中添加数据处理逻辑
3. 在`config/prometheus/`中添加Prometheus配置
4. 在`config/grafana/`中添加Grafana仪表板

### 添加新的成本模型
1. 在`src/cost_calculators/`中添加成本计算器
2. 在`config/cost-analysis/`中添加成本配置
3. 更新API接口和文档

### 测试
```bash
# 运行单元测试
pytest tests/unit/

# 运行集成测试
pytest tests/integration/

# 运行性能测试
pytest tests/performance/
```

## 贡献指南

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 许可证

本项目采用MIT许可证。详见LICENSE文件。

## 支持

如有问题或建议，请通过以下方式联系：
- 创建Issue
- 发送邮件到 support@example.com
- 加入Slack频道

## 更新日志

### v1.0.0 (2026-04-29)
- 初始版本发布
- 基础资源监控功能
- 成本分析功能
- 可视化仪表板
- 告警系统