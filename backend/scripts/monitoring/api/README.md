# API接口监控脚本

## 概述
用于监控测试环境API接口可用性和性能的Python脚本。支持多服务、多端点监控，提供详细的检查结果和告警功能。

## 功能特性
- ✅ 多服务并发监控
- ✅ HTTP端点可用性检查
- ✅ 响应时间监控
- ✅ 自动重试机制
- ✅ JSON格式结果输出
- ✅ 控制台实时报告
- ✅ 配置文件驱动
- ✅ 持续监控模式
- ✅ 告警功能（基础）

## 快速开始

### 1. 安装依赖
```bash
pip install aiohttp PyYAML
```

### 2. 配置监控
复制配置文件示例并修改：
```bash
cp config.yaml.example config.yaml
# 编辑config.yaml，修改服务地址和监控配置
```

### 3. 运行监控

#### 单次运行（测试）
```bash
python api_monitor.py --once
```

#### 持续监控
```bash
python api_monitor.py --continuous
```

#### 使用自定义配置
```bash
python api_monitor.py -c myconfig.yaml --once
```

## 配置文件说明

### 服务配置示例
```yaml
services:
  user-service:
    name: "用户服务"
    host: "user-service"      # 服务主机名
    port: 8085                # 服务端口
    endpoints:
      - name: "健康检查"
        path: "/actuator/health"
        method: "GET"
        expected_status: 200   # 期望的HTTP状态码
        timeout: 5             # 超时时间(秒)
```

### 监控配置
```yaml
monitoring:
  check_interval: 30    # 检查间隔(秒)
  timeout: 10           # 请求超时(秒)
  retry_times: 2        # 失败重试次数
  max_workers: 10       # 最大并发数
```

## 输出结果

### 控制台输出
```
============================================================
API监控检查报告
============================================================
检查时间: 2026-04-28T10:30:00
服务总数: 5
健康服务: 4 (80.0%)
端点总数: 15
成功端点: 14 (93.3%)
失败端点: 1
平均响应时间: 0.245秒
============================================================
```

### 结果文件
脚本会在 `results/` 目录下生成JSON格式的结果文件：
- `api_results_YYYYMMDD_HHMMSS.json` - 详细检查结果
- `api_summary_YYYYMMDD_HHMMSS.json` - 检查摘要

## 目录结构
```
scripts/monitoring/api/
├── api_monitor.py          # 主监控脚本
├── config.yaml.example     # 配置文件示例
├── config.yaml            # 配置文件（用户创建）
├── requirements.txt       # Python依赖
├── README.md             # 本文档
├── results/              # 检查结果目录
│   ├── api_results_20260428_103000.json
│   └── api_summary_20260428_103000.json
└── logs/                 # 日志目录（自动创建）
    └── api_monitor.log
```

## 监控指标

### 1. 可用性指标
- 服务健康状态（Spring Boot Actuator）
- 端点HTTP状态码
- 连接成功率

### 2. 性能指标
- 响应时间（毫秒）
- 请求成功率
- 错误率

### 3. 业务指标
- 关键接口可用性
- 服务依赖关系状态

## 告警功能

### 告警条件
1. 连续失败告警：端点连续失败3次
2. 响应时间告警：响应时间超过阈值（1秒警告，5秒严重）
3. 错误率告警：错误率超过阈值（1%警告，5%严重）

### 告警渠道
- 控制台输出（默认）
- 日志文件
- 邮件通知（需配置）
- Webhook（需配置）

## 部署方式

### 1. 直接运行
```bash
# 开发/测试环境
python api_monitor.py --continuous
```

### 2. Systemd服务（Linux）
创建服务文件 `/etc/systemd/system/api-monitor.service`：
```ini
[Unit]
Description=API Monitor Service
After=network.target

[Service]
Type=simple
User=monitor
WorkingDirectory=/opt/api-monitor
ExecStart=/usr/bin/python3 api_monitor.py --continuous
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable api-monitor
sudo systemctl start api-monitor
```

### 3. Docker容器
```dockerfile
FROM python:3.9-slim
WORKDIR /app
COPY . .
RUN pip install -r requirements.txt
CMD ["python", "api_monitor.py", "--continuous"]
```

构建和运行：
```bash
docker build -t api-monitor .
docker run -d --name api-monitor api-monitor
```

## 集成Prometheus

### 1. 启用Prometheus集成
在 `config.yaml` 中启用：
```yaml
prometheus:
  enabled: true
  pushgateway_url: "http://prometheus:9091"
  job_name: "api_monitor"
  interval: 30
```

### 2. 安装Prometheus客户端
```bash
pip install prometheus-client
```

### 3. 监控指标
- `api_endpoint_status` - 端点状态（0=失败，1=成功）
- `api_response_time_seconds` - 响应时间
- `api_error_rate` - 错误率
- `api_service_health` - 服务健康状态

## 故障排除

### 常见问题

1. **连接超时**
   - 检查服务地址和端口
   - 调整超时时间配置
   - 检查网络连接

2. **认证失败**
   - 检查API认证配置
   - 添加请求头或请求体

3. **性能问题**
   - 减少并发数
   - 增加检查间隔
   - 优化网络配置

### 日志查看
```bash
# 查看实时日志
tail -f logs/api_monitor.log

# 查看错误日志
grep ERROR logs/api_monitor.log
```

## 开发指南

### 代码结构
- `APIMonitor` - 主监控类
- `check_service()` - 服务检查方法
- `check_endpoint()` - 端点检查方法
- `generate_summary()` - 结果汇总方法
- `save_results()` - 结果保存方法

### 扩展功能
1. 添加新的告警渠道
2. 支持更多HTTP方法
3. 添加数据库存储
4. 实现Web界面
5. 集成更多监控系统

## 版本历史

### v1.0 (2026-04-28)
- 初始版本发布
- 基础监控功能
- 配置文件支持
- JSON结果输出
- 基础告警功能

## 许可证
MIT License

## 支持
如有问题或建议，请提交Issue或联系开发团队。