# 性能基线检查自动化脚本

## 概述

本脚本是AI-Ready测试环境质量检查自动化框架的子任务4，用于建立和管理测试环境的性能基线，并监控性能变化。该脚本通过收集性能样本、建立性能基线、检查当前性能状态，确保测试环境的性能符合预期。

## 功能特性

### 1. 性能指标全面监控
- ✅ **CPU性能**: 总体使用率、每个核心使用率、负载平均值
- ✅ **内存性能**: 使用率、使用量、可用量、交换空间
- ✅ **磁盘IO性能**: 读取速度、写入速度、IOPS、磁盘队列长度
- ✅ **网络IO性能**: 接收速度、发送速度、包丢失率、网络延迟
- ✅ **应用程序性能**: API响应时间、吞吐量、错误率、并发用户数
- ✅ **数据库性能**: 查询响应时间、连接数、锁等待时间

### 2. 智能基线管理
- **自动基线建立**: 基于历史性能数据自动建立性能基线
- **动态基线更新**: 支持基线数据的动态更新和优化
- **多维度基线**: 支持不同时间维度（小时、天、周）的基线
- **异常检测**: 基于统计方法（Z-score、百分位）检测性能异常
- **趋势分析**: 分析性能变化趋势，预测性能问题

### 3. 灵活的检查策略
- **实时检查**: 实时监控性能指标
- **定期检查**: 按计划执行性能检查
- **触发检查**: 基于事件触发性能检查
- **对比检查**: 与历史基线、SLA标准、最佳实践对比

### 4. 丰富的报告功能
- ✅ JSON格式报告（机器可读）
- ✅ HTML格式报告（可视化图表）
- ✅ Markdown格式报告（文档化）
- ✅ 性能趋势图表
- ✅ 异常告警报告
- ✅ 优化建议报告

### 5. 高级特性
- ✅ 多线程并行采样
- ✅ 智能采样频率调整
- ✅ 数据压缩和归档
- ✅ 告警阈值自定义
- ✅ 自动化修复建议
- ✅ 与监控系统集成

## 核心概念

### 1. 性能样本 (PerformanceSample)
性能数据的基本单位，包含：
- **时间戳**: 数据采集时间
- **指标**: 性能指标名称（如cpu_usage）
- **值**: 指标数值
- **单位**: 指标单位（如percent、MB/s）
- **标签**: 附加信息（如source、type）

### 2. 性能基线 (PerformanceBaseline)
性能指标的基准数据，包含：
- **统计信息**: 平均值、中位数、P95、P99、标准差等
- **样本数据**: 用于建立基线的原始样本
- **时间范围**: 基线适用的时间范围
- **置信度**: 基线的置信水平

### 3. 检查结果 (CheckResult)
性能检查的结果，包含：
- **检查状态**: 在基线内、超出基线、警告、错误
- **偏差百分比**: 当前值与基线的偏差
- **Z分数**: 统计意义上的异常程度
- **建议措施**: 优化建议和修复方案

## 快速开始

### 1. 安装依赖

```bash
# 安装Python依赖
pip install -r requirements.txt
```

**requirements.txt:**
```
psutil>=5.9.0
numpy>=1.21.0
requests>=2.28.0
pyyaml>=6.0
matplotlib>=3.5.0  # 可选：用于生成图表
```

### 2. 运行检查

#### 基本使用：
```bash
# 建立性能基线（首次运行）
python performance-baseline-check-script.py --establish-baseline

# 执行性能检查
python performance-baseline-check-script.py --check

# 同时执行基线建立和检查
python performance-baseline-check-script.py --full

# 使用指定配置文件
python performance-baseline-check-script.py --config performance-config.yaml

# 指定输出目录和格式
python performance-baseline-check-script.py --output ./performance-reports --format all

# 启用实时监控
python performance-baseline-check-script.py --monitor
```

#### 命令行参数：
| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| `--config` | `-c` | 配置文件路径 | `performance-config.yaml` |
| `--establish-baseline` | `-b` | 建立性能基线 | `false` |
| `--check` | `-C` | 执行性能检查 | `false` |
| `--full` | `-f` | 完整执行（基线+检查） | `false` |
| `--output` | `-o` | 输出目录 | `./performance-reports` |
| `--format` | `-F` | 输出格式（json/html/markdown/all） | `all` |
| `--monitor` | `-m` | 启用实时监控 | `false` |
| `--duration` | `-d` | 采样持续时间（秒） | `300` |
| `--interval` | `-i` | 采样间隔（秒） | `5` |
| `--verbose` | `-v` | 启用详细输出 | `false` |

### 3. 查看结果

检查完成后，报告会保存在指定的输出目录中：
```
performance-reports/
├── performance-baseline-report-20260429_160022.json
├── performance-baseline-report-20260429_160022.html
├── performance-baseline-report-20260429_160022.md
├── performance-samples-20260429_160022.json
├── performance-baselines.json
└── charts/
    ├── cpu_usage_trend.png
    ├── memory_usage_trend.png
    └── api_response_time_trend.png
```

## 配置说明

### 1. 主配置文件示例

```yaml
# performance-config.yaml
environment:
  name: "ai-ready-test"
  type: "test-environment"
  performance_profile: "balanced"  # balanced/performance/cost-optimized

baseline:
  collection_duration: 300  # 基线收集持续时间（秒）
  sampling_interval: 5      # 采样间隔（秒）
  min_samples: 10           # 最小样本数
  z_score_threshold: 2.0    # Z分数异常阈值
  deviation_threshold: 20   # 偏差百分比阈值（%）
  auto_update_baseline: true  # 自动更新基线
  baseline_file: "performance-baselines.json"  # 基线数据文件

checks:
  enabled_metrics:
    - "cpu_usage"
    - "memory_usage"
    - "disk_io_read"
    - "disk_io_write"
    - "network_io_in"
    - "network_io_out"
    - "api_response_time"
    - "api_throughput"
  
  service_endpoints:
    - name: "erp-purchase-api"
      url: "http://localhost:8080/api/purchase/health"
      method: "GET"
      expected_status: 200
      check_interval: 30
    
    - name: "erp-sales-api"
      url: "http://localhost:8081/api/sales/health"
      method: "GET"
      expected_status: 200
      check_interval: 30

monitoring:
  enable_real_time: false
  alert_on_deviation: true
  alert_threshold: 30  # 偏差超过30%告警
  alert_channels:
    - type: "email"
      recipients: ["perf-team@example.com"]
    - type: "slack"
      webhook: "https://hooks.slack.com/services/xxx"

reporting:
  output_formats: ["json", "html", "markdown"]
  output_dir: "./performance-reports"
  generate_trend_charts: true
  store_samples: true
  sample_retention_days: 30
```

### 2. 性能指标定义

```yaml
metrics:
  cpu_usage:
    name: "CPU使用率"
    unit: "percent"
    description: "系统CPU总体使用率"
    alert_threshold: 80
    warning_threshold: 70
  
  memory_usage:
    name: "内存使用率"
    unit: "percent"
    description: "系统内存使用率"
    alert_threshold: 90
    warning_threshold: 80
  
  api_response_time:
    name: "API响应时间"
    unit: "milliseconds"
    description: "API平均响应时间"
    alert_threshold: 1000  # 1秒
    warning_threshold: 500  # 500毫秒
  
  api_throughput:
    name: "API吞吐量"
    unit: "requests_per_second"
    description: "每秒处理的API请求数"
    alert_threshold: 50  # 低于50请求/秒告警
    warning_threshold: 100
```

### 3. 基线策略配置

```yaml
baseline_strategies:
  hourly_baseline:
    name: "小时基线"
    description: "按小时建立的性能基线"
    time_window: "1h"
    sample_size: 60  # 每小时60个样本（每分钟1个）
    update_frequency: "1h"
  
  daily_baseline:
    name: "日基线"
    description: "按天建立的性能基线"
    time_window: "24h"
    sample_size: 1440  # 每天1440个样本（每分钟1个）
    update_frequency: "1d"
  
  weekly_baseline:
    name: "周基线"
    description: "按周建立的性能基线"
    time_window: "7d"
    sample_size: 10080  # 每周10080个样本（每分钟1个）
    update_frequency: "7d"
  
  seasonal_baseline:
    name: "季节性基线"
    description: "考虑季节性的性能基线"
    time_window: "90d"
    seasonality: "weekly"  # 周季节性
    update_frequency: "1d"
```

## 集成到CI/CD

### 1. Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('性能基线检查') {
            steps {
                script {
                    // 建立性能基线
                    sh '''
                        python performance-baseline-check-script.py \
                            --config performance-config.yaml \
                            --establish-baseline \
                            --output ./baseline-reports
                    '''
                    
                    // 执行性能检查
                    sh '''
                        python performance-baseline-check-script.py \
                            --config performance-config.yaml \
                            --check \
                            --output ./performance-reports
                    '''
                    
                    // 检查性能状态
                    def report = readJSON file: 'performance-reports/latest-report.json'
                    if (report.overall_status != 'PERFORMANCE_NORMAL') {
                        // 生成性能分析报告
                        sh 'python performance-baseline-check-script.py --analyze'
                        
                        // 发送告警
                        if (report.overall_status == 'PERFORMANCE_DEGRADED') {
                            error "性能严重下降: ${report.overall_status}"
                        } else {
                            echo "性能警告: ${report.overall_status}"
                        }
                    }
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'performance-reports/**', fingerprint: true
            archiveArtifacts artifacts: 'baseline-reports/**', fingerprint: true
        }
    }
}
```

### 2. GitLab CI

```yaml
performance-baseline:
  stage: test
  script:
    - |
      # 检查是否已有基线数据
      if [ -f "performance-baselines.json" ]; then
        echo "使用现有性能基线"
        cp performance-baselines.json ./performance-reports/
      else
        echo "建立新的性能基线"
        python performance-baseline-check-script.py \
          --config performance-config.yaml \
          --establish-baseline \
          --output ./baseline-reports
      fi
      
      # 执行性能检查
      python performance-baseline-check-script.py \
        --config performance-config.yaml \
        --check \
        --output ./performance-reports
      
      # 检查性能状态
      REPORT_FILE=$(find performance-reports -name "*.json" | head -1)
      PERFORMANCE_STATUS=$(jq -r '.overall_status' "$REPORT_FILE")
      
      if [ "$PERFORMANCE_STATUS" = "PERFORMANCE_DEGRADED" ]; then
        echo "::error::性能严重下降: $PERFORMANCE_STATUS"
        exit 1
      elif [ "$PERFORMANCE_STATUS" = "PERFORMANCE_WARNING" ]; then
        echo "::warning::性能警告: $PERFORMANCE_STATUS"
      fi
  artifacts:
    paths:
      - performance-reports/
      - baseline-reports/
    expire_in: 1 week
```

### 3. 实时监控集成

```python
# 实时监控示例
from performance_baseline_check_script import PerformanceBaselineChecker
import time

class RealTimeMonitor:
    def __init__(self, config_file):
        self.checker = PerformanceBaselineChecker(config_file)
        self.is_monitoring = False
    
    def start_monitoring(self, check_interval=60):
        """启动实时监控"""
        self.is_monitoring = True
        print("启动性能实时监控...")
        
        while self.is_monitoring:
            try:
                # 执行性能检查
                results = self.checker.run_all_checks()
                
                # 检查性能状态
                if results["overall_status"] == "PERFORMANCE_DEGRADED":
                    self.send_alert("性能严重下降", results)
                elif results["overall_status"] == "PERFORMANCE_WARNING":
                    self.send_warning("性能警告", results)
                
                # 等待下一个检查周期
                time.sleep(check_interval)
                
            except Exception as e:
                print(f"监控执行失败: {e}")
                time.sleep(check_interval)
    
    def stop_monitoring(self):
        """停止实时监控"""
        self.is_monitoring = False
        print("停止性能实时监控")
    
    def send_alert(self, title, results):
        """发送告警"""
        print(f"🚨 {title}")
        print(f"  总体状态: {results['overall_status']}")
        print(f"  超出基线: {results['outside_baseline']} 项")
        
        # 这里可以集成邮件、钉钉、Slack等通知渠道
    
    def send_warning(self, title, results):
        """发送警告"""
        print(f"⚠️ {title}")
        print(f"  总体状态: {results['overall_status']}")
        print(f"  警告项: {results['warning_checks']} 项")

# 使用示例
if __name__ == "__main__":
    monitor = RealTimeMonitor("performance-config.yaml")
    
    # 启动监控（每5分钟检查一次）
    try:
        monitor.start_monitoring(check_interval=300)
    except KeyboardInterrupt:
        monitor.stop_monitoring()
```

## 扩展开发

### 1. 添加自定义性能指标

```python
from performance_baseline_check_script import PerformanceBaselineChecker

class CustomPerformanceChecker(PerformanceBaselineChecker):
    """自定义性能检查器"""
    
    def check_custom_metric(self):
        """检查自定义性能指标"""
        try:
            # 实现自定义指标检查逻辑
            custom_value = self.get_custom_metric_value()
            
            return {
                "metric": "custom_metric",
                "unit": "custom_unit",
                "current_value": custom_value,
                "message": f"自定义指标值: {custom_value}",
                "details": {
                    "calculation_method": "custom",
                    "data_source": "external_api"
                }
            }
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"自定义指标检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def get_custom_metric_value(self):
        """获取自定义指标值"""
        # 这里实现获取自定义指标的逻辑
        # 例如：从外部API、数据库、文件等获取数据
        import random
        return random.uniform(0, 100)  # 模拟数据
```

### 2. 集成外部监控系统

```python
class ExternalMonitorIntegration:
    """外部监控系统集成"""
    
    def __init__(self, monitor_type):
        self.monitor_type = monitor_type
        self.client = self.create_client()
    
    def create_client(self):
        """创建监控系统客户端"""
        if self.monitor_type == "prometheus":
            from prometheus_client import CollectorRegistry, Gauge, push_to_gateway
            return {"type": "prometheus", "registry": CollectorRegistry()}
        elif self.monitor_type == "datadog":
            from datadog import initialize, api
            # 初始化Datadog客户端
            return {"type": "datadog", "api": api}
        else:
            raise ValueError(f"不支持的监控类型: {self.monitor_type}")
    
    def push_metrics(self, performance_data):
        """推送性能指标到监控系统"""
        if self.monitor_type == "prometheus":
            self._push_to_prometheus(performance_data)
        elif self.monitor_type == "datadog":
            self._push_to_datadog(performance_data)
    
    def _push_to_prometheus(self, performance_data):
        """推送到Prometheus"""
        registry = self.client["registry"]
        
        for metric_name, metric_value in performance_data.items():
            gauge = Gauge(metric_name, f"Performance metric: {metric_name}", registry=registry)
            gauge.set(metric_value)
        
        # 推送到Pushgateway
        push_to_gateway('localhost:9091', job='performance_baseline', registry=registry)
    
    def _push_to_datadog(self, performance_data):
        """推送到Datadog"""
        # 实现Datadog推送逻辑
        pass
```

### 3. 性能预测和趋势分析

```python
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import PolynomialFeatures

class PerformancePredictor:
    """性能预测器"""
    
    def __init__(self, historical_data):
        self.historical_data = historical_data
        self.model = None
    
    def train_predictor(self, metric_name, degree=2):
        """训练性能预测模型"""
        # 准备训练数据
        timestamps = []
        values = []
        
        for sample in self.historical_data:
            if sample.metric == metric_name:
                # 使用时间戳作为特征
                timestamp_seconds = sample.timestamp.timestamp()
                timestamps.append([timestamp_seconds])
                values.append(sample.value)
        
        if len(timestamps) < 10:
            return None  # 数据不足
        
        # 多项式特征
        poly = PolynomialFeatures(degree=degree)
        X_poly = poly.fit_transform(timestamps)
        
        # 训练线性回归模型
        self.model = LinearRegression()
        self.model.fit(X_poly, values)
        
        return self.model
    
    def predict_future(self, metric_name, future_minutes=60):
        """预测未来性能"""
        if not self.model:
            self.train_predictor(metric_name)
        
        if not self.model:
            return None
        
        # 生成未来时间点
        last_timestamp = max([s.timestamp.timestamp() for s in self.historical_data 
                            if s.metric == metric_name])
        future_timestamps = []
        
        for i in range(1, future_minutes + 1):
            future_seconds = last_timestamp + (i * 60)  # 每分钟一个预测点
            future_timestamps.append([future_seconds])
        
        # 多项式特征转换
        poly = PolynomialFeatures(degree=2)
        X_future_poly = poly.fit_transform(future_timestamps)
        
        # 预测
        predictions = self.model.predict(X_future_poly)
        
        # 转换为可读格式
        result = []
        for i, pred in enumerate(predictions):
            result.append({
                "minutes_ahead": i + 1,
                "predicted_value": pred,
                "timestamp": datetime.fromtimestamp(last_timestamp + ((i + 1) * 60))
            })
        
        return result
```

## 故障排除

### 1. 常见问题

#### Q: 基线建立时间过长
**解决**: 调整采样参数
```yaml
baseline:
  collection_duration: 60  # 减少到1分钟
  sampling_interval: 1     # 缩短采样间隔
  min_samples: 30          # 减少最小样本数
```

#### Q: 检查结果波动较大
**解决**: 优化采样策略
```yaml
sampling:
  strategy: "adaptive"  # 自适应采样
  min_interval: 1
  max_interval: 10
  stability_threshold: 0.1  # 稳定性阈值
```

#### Q: 内存使用过高
**解决**: 优化数据处理
```yaml
performance:
  max_samples_in_memory: 10000  # 限制内存中的样本数
  enable_compression: true
  persist_to_disk: true
```

#### Q: 网络检查失败
**解决**: 配置代理或调整超时
```yaml
network:
  proxy: "http://proxy.example.com:8080"
  timeout: 30
  retry_count: 3
  verify_ssl: false  # 仅用于测试环境
```

### 2. 调试模式

启用调试模式获取更多信息：

```bash
# 设置环境变量
export PERFORMANCE_DEBUG=1
export PERFORMANCE_LOG_LEVEL=DEBUG

# 运行脚本
python performance-baseline-check-script.py --verbose --debug

# 查看详细日志
tail -f performance-baseline-debug.log
```

### 3. 性能优化建议

对于大规模环境，优化脚本性能：

```yaml
optimization:
  parallel_sampling: true
  max_workers: 4
  batch_size: 100
  cache_enabled: true
  cache_ttl: 300
  
  # 减少不重要的检查
  skip_low_priority: false
  priority_threshold: "medium"
```

## 安全注意事项

### 1. 数据安全
- 性能数据可能包含系统敏感信息
- 实现数据脱敏和加密存储
- 控制报告访问权限

### 2. 访问控制
- 限制脚本的执行权限
- 使用服务账户运行脚本
- 实现API调用的认证机制

### 3. 网络安全
- 加密网络传输的数据
- 使用VPN或专用网络进行监控
- 实现防火墙规则控制

## 版本历史

### v1.0.0 (2026-04-29)
- ✅ 初始版本发布
- ✅ 支持8个核心性能指标
- ✅ 智能基线管理和异常检测
- ✅ 多格式报告和趋势图表
- ✅ 实时监控支持
- ✅ 与CI/CD集成

### v0.9.0 (2026-04-28)
- 🔄 草案版本
- 🔄 基本框架实现
- 🔄 初步的基线算法

### 后续计划
- 🔄 机器学习异常检测
- 🔄 性能根因分析
- 🔄 自动化优化建议
- 🔄 容器和云环境支持
- 🔄 多节点集群监控
- 🔄 性能容量规划

## 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目负责人: qa-lead
- 项目: AI-Ready测试环境配置专项
- Sprint: Sprint 27+1
- 任务ID: task_1777438750224_y4l4dft4a
- 子任务: 子任务4 - 性能基线检查脚本开发