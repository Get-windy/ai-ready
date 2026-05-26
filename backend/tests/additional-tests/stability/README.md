# 稳定性测试文档

## 目录
1. [概述](#概述)
2. [测试内容](#测试内容)
3. [测试脚本说明](#测试脚本说明)
4. [测试数据说明](#测试数据说明)
5. [运行方法](#运行方法)
6. [测试报告](#测试报告)
7. [集成指南](#集成指南)

---

## 概述

Sprint 27+1 测试环境稳定性测试脚本用于验证测试环境各服务在长时间运行下的稳定性，确保系统能够稳定运行至少24小时。

### 测试目标
- [x] 稳定性测试脚本开发完成
- [x] 支持24小时持续运行
- [x] 服务崩溃恢复测试通过
- [x] 稳定性测试报告生成完整
- [x] 发现并记录至少2个稳定性问题

### 测试原理
稳定性测试通过持续模拟用户请求、监控系统资源使用情况、检测服务异常和性能 degradation，评估系统在长时间运行下的稳定性和可靠性。

---

## 测试内容

### 1. 服务持续可用性测试
测试服务在长时间运行下的可用性，确保服务在24小时内保持高可用。

**测试项**:
- 健康检查端点响应
- API接口响应时间
- 服务错误率
- 服务可用性百分比

**通过标准**: 服务可用性 ≥ 99%

### 2. 服务崩溃恢复测试
测试服务在崩溃后的恢复能力，确保服务具备自我恢复机制。

**测试项**:
- 服务崩溃检测
- 服务自动重启
- 恢复时间
- 恢复成功率

**通过标准**: 恢复时间 ≤ 60秒，恢复成功率 = 100%

### 3. 服务资源泄漏检测
检测服务在长时间运行下是否存在资源泄漏问题。

**测试项**:
- 内存使用率变化
- 线程数变化
- 连接数变化
- 文件句柄数变化

**通过标准**: 资源使用率无明显增长趋势

### 4. 服务死锁检测
检测服务在高并发场景下是否存在死锁问题。

**测试项**:
- 死锁检测
- 锁等待时间
- 事务超时次数
- 并发请求成功率

**通过标准**: 无死锁，锁等待时间 < 30秒

---

## 测试脚本说明

### 核心脚本文件

#### 1. `test_stability.py` - 主稳定性测试脚本

**功能**: 主要的稳定性测试执行脚本，包含完整的稳定性测试逻辑。

**主要功能**:
- 服务健康检查
- 系统资源监控
- 并发请求模拟
- 资源泄漏检测
- 死锁检测
- 测试报告生成

**使用方法**:
```bash
python test_stability.py
```

**环境变量**:
- `STABILITY_TEST_BASE_URL`: 测试环境URL（默认: http://localhost:8080）

**运行参数**:
```python
config = StabilityConfig()
config.test_duration = 86400  # 24小时（秒）
config.concurrent_users = 10  # 并发用户数
```

#### 2. `run_stability_tests.py` - 测试运行器

**功能**: 统一的测试运行入口，支持运行单个或多个测试。

**主要功能**:
- 稳定性测试
- 压力测试
- 崩溃恢复测试
- 资源泄漏检测测试
- 死锁检测测试
- 测试报告生成和汇总

**使用方法**:
```bash
python run_stability_tests.py
```

**运行单个测试**:
```bash
# 选择运行类型
# 1. 运行稳定性测试
# 2. 运行压力测试
# 3. 运行崩溃恢复测试
# 4. 运行资源泄漏检测测试
# 5. 运行死锁检测测试
# 6. 运行所有测试
```

#### 3. `generate_stability_test_data.py` - 测试数据生成脚本

**功能**: 生成各类型测试所需的测试数据。

**使用方法**:
```bash
python generate_stability_test_data.py
```

**可生成的数据**:
- 稳定性测试数据集
- 压力测试数据
- 边界测试数据
- 大数据量测试数据
- 并发执行测试数据

---

## 测试数据说明

### 1. 稳定性测试数据 (`stability_test_data.json`)
- 1000条测试数据
- 覆盖24小时持续运行场景
- 包含各测试场景的详细参数

### 2. 压力测试数据 (`stress_test_data.json`)
- 1000个并发用户
- 3种压力测试场景
- 3种负载模式

### 3. 边界测试数据 (`boundary_test_data.json`)
- 3个测试类别
- 14个测试用例
- 覆盖输入/性能/资源边界

### 4. 大数据量测试数据 (`large_volume_test_data.json`)
- 10000条记录
- 3个数据集
- 3个测试场景

### 5. 并发执行测试数据 (`concurrent_execution_data.json`)
- 100个线程
- 3个并发测试场景
- 2个死锁场景

---

## 运行方法

### 安装依赖
```bash
pip install aiohttp psutil
```

### 运行单个测试
```bash
# 稳定性测试
python test_stability.py

# 压力测试
python run_stability_tests.py
# 选择: 2

# 崩溃恢复测试
python run_stability_tests.py
# 选择: 3

# 资源泄漏检测
python run_stability_tests.py
# 选择: 4

# 死锁检测
python run_stability_tests.py
# 选择: 5
```

### 运行所有测试
```bash
python run_stability_tests.py
# 选择: 6
```

### 24小时完整测试
```bash
# 设置持续时间为24小时（1440分钟）
python run_stability_tests.py
# 选择: 1
# 输入持续时间: 1440
```

### 使用Docker运行
```bash
docker run -it --rm \
  -v $(pwd)/tests/stability:/app/stability \
  -e STABILITY_TEST_BASE_URL="http://host.docker.internal:8080" \
  python:3.11 \
  python /app/stability/test_stability.py
```

---

## 测试报告

### 报告生成位置
```
tests/stability/reports/
├── stability_test_report_*.json          # JSON格式报告
├── stability_test_report_*.md            # 文本格式报告
├── stress_test_report_*.json
├── stress_test_report_*.md
├── crash_recovery_test_report_*.json
├── crash_recovery_test_report_*.md
├── resource_leak_test_report_*.json
├── resource_leak_test_report_*.md
├── deadlock_test_report_*.json
├── deadlock_test_report_*.md
└── stability_test_summary_*.json         # 综合报告
```

### 报告内容

#### 1. 基本信息
- 测试类型和名称
- 测试开始和结束时间
- 测试持续时间

#### 2. 性能指标
- 总请求数和成功率
- 平均响应时间（平均、P95、P99）
- 服务可用性
- CPU和内存使用率

#### 3. 稳定性事件
- 服务崩溃事件
- 服务恢复事件
- 资源泄漏警告
- 死锁警告

#### 4. 优化建议
- 发现的问题
- 改进建议

#### 5. 通过标准检查
- 服务可用性 ≥ 99%
- 请求成功率 ≥ 99%
- 平均响应时间 < 1000ms
- 无资源泄漏
- 无死锁

---

## 集成指南

### 集成到CI/CD

#### GitLab CI
```yaml
stability-test:
  stage: test
  script:
    - pip install aiohttp psutil
    - python tests/stability/test_stability.py
  artifacts:
    paths:
      - tests/stability/reports/
    expire_in: 30 days
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
```

#### GitHub Actions
```yaml
name: Stability Test

on:
  push:
    branches: [main]

jobs:
  stability-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - name: Install dependencies
        run: |
          pip install aiohttp psutil
      - name: Run stability test
        run: |
          python tests/stability/test_stability.py
        env:
          STABILITY_TEST_BASE_URL: http://localhost:8080
      - name: Upload reports
        uses: actions/upload-artifact@v3
        with:
          name: stability-test-reports
          path: tests/stability/reports/
```

### 集成到测试环境监控系统

#### Prometheus指标
```yaml
# 在test_stability.py中添加Prometheus指标
from prometheus_client import start_http_server, Gauge

# 创建指标
service_availability = Gauge('service_availability', 'Service availability percentage')
cpu_usage = Gauge('cpu_usage_percent', 'CPU usage percentage')
memory_usage = Gauge('memory_usage_percent', 'Memory usage percentage')
error_rate = Gauge('error_rate_percent', 'Request error rate percentage')

# 更新指标
service_availability.set(availability)
cpu_usage.set(cpu_percent)
memory_usage.set(memory_percent)
error_rate.set(error_percent)
```

#### Grafana仪表板
```json
{
  "annotations": {
    "list": []
  },
  "panels": [
    {
      "title": "服务可用性",
      "type": "gauge",
      "targets": [
        {
          "expr": "service_availability"
        }
      ]
    },
    {
      "title": "CPU使用率",
      "type": "graph",
      "targets": [
        {
          "expr": "cpu_usage_percent"
        }
      ]
    }
  ]
}
```

---

## 常见问题

### 1. 端口被占用
```
问题: 端口8080被占用
解决: netstat -ano | findstr :8080
      taskkill /PID [PID] /F
```

### 2. 依赖包缺失
```
问题: ModuleNotFoundError
解决: pip install aiohttp psutil
```

### 3. 测试超时
```
问题: 测试超时
解决: 增加测试超时时间或检查网络连接
```

### 4. 服务不可达
```
问题: 无法连接到测试服务
解决: 
  1. 检查服务是否启动
  2. 检查服务地址配置
  3. 检查网络连接
```

---

## 下一步计划

1. **扩展测试覆盖**:
   - 添加更多API接口测试
   - 添加数据库连接池测试
   - 添加缓存服务测试

2. **自动化测试**:
   - 集成到CI/CD
   - 定期自动运行测试
   - 自动提交测试报告

3. **监控集成**:
   - 集成到Prometheus
   - 创建Grafana仪表板
   - 设置告警规则

4. **测试优化**:
   - 优化测试脚本性能
   - 增加更多测试场景
   - 改进测试数据生成

---

## 联系信息

- **项目**: AI-Ready Sprint 27+1
- **负责人**: test-agent-2
- **文档位置**: `I:\AI-Ready\tests\stability\README.md`
- **测试脚本**: `I:\AI-Ready\tests\stability\`

---

**最后更新**: 2026-04-28  
**状态**: ✅ 开发完成
