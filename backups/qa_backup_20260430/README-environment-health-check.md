# 环境健康检查自动化脚本

## 概述

本脚本是AI-Ready测试环境质量检查自动化框架的一部分，用于自动执行测试环境的健康检查。该脚本支持检查基础设施、服务和应用程序的健康状态，并生成详细的检查报告。

## 功能特性

### 1. 基础设施检查
- ✅ CPU使用率检查
- ✅ 内存使用率检查  
- ✅ 磁盘空间检查
- ✅ 网络连通性检查
- ✅ 关键进程检查

### 2. 服务健康检查
- ✅ ERP核心服务健康检查（采购、销售、库存、财务）
- ✅ 数据库服务检查（MySQL、Redis）
- ✅ 中间件服务检查（Kafka、ZooKeeper）
- ✅ 监控服务检查（Prometheus、Grafana）
- ✅ API网关健康检查

### 3. 应用程序检查
- ✅ ERP模块基本功能检查
- ✅ 数据库连接检查
- ✅ API端点可用性检查

### 4. 报告功能
- ✅ JSON格式报告
- ✅ HTML格式报告（可视化）
- ✅ Markdown格式报告
- ✅ 实时日志输出
- ✅ 历史报告归档

### 5. 高级功能
- ✅ 并行执行检查（提高效率）
- ✅ 可配置的阈值和规则
- ✅ 自定义检查脚本支持
- ✅ 错误重试机制
- ✅ 通知集成（邮件、钉钉、Slack）

## 快速开始

### 1. 安装依赖

```bash
# 安装Python依赖
pip install -r requirements.txt
```

**requirements.txt:**
```
requests>=2.28.0
psutil>=5.9.0
pyyaml>=6.0
```

### 2. 运行检查

#### 基本使用：
```bash
# 使用默认配置运行检查
python environment-health-check-script.py

# 使用指定配置文件
python environment-health-check-script.py --config health-check-config.yaml

# 指定输出目录和格式
python environment-health-check-script.py --config config.yaml --output ./reports --format all

# 启用详细输出
python environment-health-check-script.py --verbose
```

#### 命令行参数：
| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| `--config` | `-c` | 配置文件路径 | `health-check-config.yaml` |
| `--output` | `-o` | 输出目录 | `./health-reports` |
| `--format` | `-f` | 输出格式（json/html/markdown/all） | `all` |
| `--verbose` | `-v` | 启用详细输出 | `false` |

### 3. 查看结果

检查完成后，报告会保存在指定的输出目录中：
```
health-reports/
├── environment-health-report-20260429_143022.json
├── environment-health-report-20260429_143022.html
└── environment-health-report-20260429_143022.md
```

## 配置说明

### 1. 基础配置示例

```yaml
# health-check-config.yaml
environment:
  name: "test-environment"
  description: "测试环境"

checks:
  infrastructure:
    enabled: true
    cpu_threshold: 80
    memory_threshold: 85
    
  services:
    enabled: true
    timeout: 10
    
    expected_services:
      - name: "erp-purchase"
        host: "localhost"
        port: 8080
        health_endpoint: "/actuator/health"
        expected_status: 200
```

### 2. 添加自定义服务检查

```yaml
services:
  expected_services:
    - name: "my-custom-service"
      host: "192.168.1.100"
      port: 9000
      protocol: "http"
      health_endpoint: "/health"
      expected_status: 200
      required: true
      
      # 自定义健康检查逻辑
      custom_check: |
        import requests
        def check():
            response = requests.get("http://192.168.1.100:9000/health")
            if response.status_code == 200:
                data = response.json()
                return data.get("status") == "UP"
            return False
```

### 3. 配置通知

```yaml
reporting:
  send_notifications: true
  
  notification_channels:
    email:
      enabled: true
      smtp_server: "smtp.example.com"
      recipients: ["team@example.com"]
      
    dingtalk:
      enabled: true
      webhook: "https://oapi.dingtalk.com/robot/send"
      at_users: ["18888888888"]
```

## 集成到CI/CD

### 1. Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('环境健康检查') {
            steps {
                script {
                    sh 'python environment-health-check-script.py --config health-check-config.yaml'
                    
                    // 检查总体状态
                    def report = readJSON file: 'health-reports/latest-report.json'
                    if (report.overall_status != 'HEALTHY') {
                        error "环境健康检查失败: ${report.overall_status}"
                    }
                }
            }
        }
    }
}
```

### 2. GitLab CI

```yaml
health-check:
  stage: test
  script:
    - python environment-health-check-script.py --config health-check-config.yaml
    - |
      if [ "$(jq -r '.overall_status' health-reports/latest-report.json)" != "HEALTHY" ]; then
        echo "环境健康检查失败"
        exit 1
      fi
  artifacts:
    paths:
      - health-reports/
    expire_in: 1 week
```

### 3. GitHub Actions

```yaml
name: Environment Health Check

on:
  schedule:
    - cron: '0 */2 * * *'  # 每2小时执行一次
  workflow_dispatch:

jobs:
  health-check:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.9'
    
    - name: Install dependencies
      run: pip install -r requirements.txt
    
    - name: Run health check
      run: python environment-health-check-script.py --config health-check-config.yaml
    
    - name: Upload reports
      uses: actions/upload-artifact@v3
      with:
        name: health-check-reports
        path: health-reports/
```

## 扩展开发

### 1. 添加自定义检查项

创建自定义检查脚本 `custom-checks/check_custom_service.py`:

```python
def check_custom_service(config):
    """
    自定义服务检查
    """
    import requests
    
    result = {
        "status": "UNKNOWN",
        "message": "",
        "details": {}
    }
    
    try:
        response = requests.get(
            f"http://{config['host']}:{config['port']}{config.get('endpoint', '/')}",
            timeout=config.get('timeout', 10)
        )
        
        if response.status_code == 200:
            result["status"] = "PASS"
            result["message"] = "自定义服务正常"
        else:
            result["status"] = "FAIL"
            result["message"] = f"自定义服务异常: HTTP {response.status_code}"
            
        result["details"]["response_time"] = response.elapsed.total_seconds()
        result["details"]["status_code"] = response.status_code
        
    except Exception as e:
        result["status"] = "ERROR"
        result["message"] = f"自定义服务检查失败: {str(e)}"
        result["details"]["error"] = str(e)
    
    return result
```

### 2. 集成到监控系统

#### Prometheus Metrics:

```python
from prometheus_client import Counter, Gauge, Histogram

# 定义指标
health_check_total = Counter('health_check_total', 'Total health checks')
health_check_duration = Histogram('health_check_duration_seconds', 'Health check duration')
service_status = Gauge('service_status', 'Service status (0=down, 1=up)', ['service'])

# 在检查中更新指标
def check_service_with_metrics(service_config):
    with health_check_duration.time():
        result = check_service(service_config)
        health_check_total.inc()
        
        status_value = 1 if result["status"] == "PASS" else 0
        service_status.labels(service=service_config["name"]).set(status_value)
        
    return result
```

## 故障排除

### 1. 常见问题

#### Q: 脚本执行时间过长
**原因**: 某些服务响应慢或网络延迟高
**解决**: 调整 `timeout` 配置，或启用并行执行

```yaml
execution:
  parallel_execution:
    enabled: true
    max_workers: 10
  timeout:
    per_check_timeout: 30
```

#### Q: 报告文件过大
**原因**: 保存了过多的历史报告
**解决**: 配置报告保留策略

```yaml
reporting:
  retention:
    max_reports: 50
    max_age_days: 7
    compress_old_reports: true
```

#### Q: 某些检查项失败但不影响测试
**原因**: 非必需的服务检查失败
**解决**: 将非必需服务标记为 `required: false`

```yaml
services:
  expected_services:
    - name: "monitoring-service"
      required: false  # 非必需服务
```

### 2. 调试模式

启用调试模式获取更多信息：

```bash
# 设置环境变量
export HEALTH_CHECK_DEBUG=1
export HEALTH_CHECK_LOG_LEVEL=DEBUG

# 运行脚本
python environment-health-check-script.py --verbose
```

## 性能优化建议

### 1. 并行执行优化
- 根据检查项数量调整 `max_workers`
- IO密集型检查可以使用更多worker
- CPU密集型检查需要限制worker数量

### 2. 缓存优化
- 对频繁检查的服务实现结果缓存
- 设置合理的缓存过期时间
- 避免重复检查相同的指标

### 3. 资源监控
- 监控脚本自身的CPU和内存使用
- 设置脚本执行时间限制
- 实现优雅的异常处理

## 安全注意事项

### 1. 配置文件安全
- 不要将敏感信息（密码、密钥）硬编码在配置文件中
- 使用环境变量或密钥管理服务
- 限制配置文件的访问权限

### 2. 网络访问安全
- 限制脚本的网络访问范围
- 使用防火墙规则控制访问
- 实现请求速率限制

### 3. 数据安全
- 检查结果可能包含敏感信息
- 实现报告数据的脱敏处理
- 设置适当的报告访问控制

## 版本历史

### v1.0.0 (2026-04-29)
- ✅ 初始版本发布
- ✅ 支持基础设施检查
- ✅ 支持服务健康检查
- ✅ 支持多格式报告
- ✅ 支持并行执行

### 后续计划
- 🔄 集成容器健康检查
- 🔄 支持Kubernetes环境
- 🔄 实现趋势分析和预测
- 🔄 添加更多通知渠道

## 贡献指南

欢迎提交Issue和Pull Request！

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开Pull Request

## 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目负责人: qa-lead
- 项目: AI-Ready测试环境配置专项
- Sprint: Sprint 27+1
- 任务ID: task_1777438750224_y4l4dft4a