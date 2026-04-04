# AI-Ready 本地性能测试框架设计文档

## 1. 框架概述

### 1.1 设计目标

设计一套完整的本地性能测试框架，支持：
- 离线性能验证（无需启动API服务）
- 多种性能测试场景
- 自动化测试数据生成
- 详细性能报告输出

### 1.2 核心功能

| 功能模块 | 描述 |
|---------|------|
| API性能测试 | 测试API响应时间、吞吐量 |
| 数据库性能测试 | 测试数据库查询性能 |
| 压力测试 | 测试系统极限性能 |
| 负载测试 | 测试系统稳定性 |
| 安全性能测试 | 测试安全处理性能 |

---

## 2. 框架架构

```
tests/performance/
├── __init__.py
├── run_local_performance.py    # 主测试入口
├── stress_test.py              # 压力测试
├── load_test.py                # 负载测试
├── benchmark_test.py           # 基准测试
├── data_generator.py           # 测试数据生成器
├── config/
│   ├── test_config.yaml        # 测试配置
│   └── baseline.json           # 性能基线
├── reports/
│   └── templates/              # 报告模板
└── utils/
    ├── metrics.py              # 性能指标工具
    └── reporter.py             # 报告生成器
```

---

## 3. 测试场景设计

### 3.1 API性能测试场景

```python
API_TEST_SCENARIOS = [
    {
        "name": "用户列表查询",
        "endpoint": "/api/user/page",
        "method": "GET",
        "params": {"pageNum": 1, "pageSize": 10},
        "expected_max_ms": 100,
        "weight": 0.3
    },
    {
        "name": "角色列表查询",
        "endpoint": "/api/role/page",
        "method": "GET",
        "params": {"pageNum": 1, "pageSize": 10},
        "expected_max_ms": 80,
        "weight": 0.2
    },
    {
        "name": "健康检查",
        "endpoint": "/api/health",
        "method": "GET",
        "expected_max_ms": 50,
        "weight": 0.1
    }
]
```

### 3.2 压力测试场景

```python
STRESS_TEST_SCENARIOS = {
    "light": {
        "concurrent_users": 50,
        "duration_seconds": 60,
        "ramp_up_seconds": 10
    },
    "medium": {
        "concurrent_users": 200,
        "duration_seconds": 120,
        "ramp_up_seconds": 20
    },
    "heavy": {
        "concurrent_users": 500,
        "duration_seconds": 180,
        "ramp_up_seconds": 30
    },
    "extreme": {
        "concurrent_users": 1000,
        "duration_seconds": 300,
        "ramp_up_seconds": 60
    }
}
```

### 3.3 负载测试场景

```python
LOAD_TEST_SCENARIOS = {
    "stability": {
        "concurrent_users": 50,
        "duration_minutes": 10,
        "monitor_interval_seconds": 5
    },
    "endurance": {
        "concurrent_users": 30,
        "duration_minutes": 30,
        "monitor_interval_seconds": 10
    }
}
```

---

## 4. 测试数据生成器

### 4.1 用户数据生成

```python
def generate_user_data(count: int) -> List[Dict]:
    """生成测试用户数据"""
    users = []
    for i in range(count):
        users.append({
            "username": f"test_user_{i}",
            "password": generate_random_password(),
            "email": f"test{i}@example.com",
            "phone": generate_random_phone(),
            "tenantId": 1,
            "status": random.choice([0, 1])
        })
    return users
```

### 4.2 订单数据生成

```python
def generate_order_data(count: int) -> List[Dict]:
    """生成测试订单数据"""
    orders = []
    for i in range(count):
        orders.append({
            "orderNo": f"ORD{datetime.now().strftime('%Y%m%d')}{i:06d}",
            "customerId": random.randint(1, 1000),
            "totalAmount": round(random.uniform(100, 10000), 2),
            "status": random.choice(["pending", "paid", "shipped", "completed"])
        })
    return orders
```

---

## 5. 性能指标

### 5.1 核心指标

| 指标 | 描述 | 计算公式 |
|------|------|---------|
| 响应时间(平均) | 平均API响应时间 | Σ(response_time) / n |
| 响应时间(P95) | 95%请求响应时间 | percentile(response_times, 95) |
| 响应时间(P99) | 99%请求响应时间 | percentile(response_times, 99) |
| 吞吐量 | 每秒处理请求数 | total_requests / duration |
| 错误率 | 失败请求比例 | failed_requests / total * 100% |
| 并发用户数 | 同时活跃用户数 | active_threads |

### 5.2 资源指标

| 指标 | 描述 | 采集方式 |
|------|------|---------|
| CPU使用率 | CPU占用百分比 | psutil.cpu_percent() |
| 内存使用率 | 内存占用百分比 | psutil.virtual_memory().percent |
| 线程数 | 活跃线程数 | psutil.Process().num_threads() |
| 网络IO | 网络吞吐量 | psutil.net_io_counters() |

---

## 6. 报告生成

### 6.1 报告格式

- **Markdown报告**: 人类可读的详细报告
- **JSON报告**: 机器可解析的结构化数据
- **HTML报告**: 带图表的可视化报告

### 6.2 报告内容

```markdown
# 性能测试报告

## 测试概览
- 测试时间
- 测试环境
- 测试类型

## 性能指标
- 响应时间统计
- 吞吐量统计
- 错误率统计

## 资源使用
- CPU使用曲线
- 内存使用曲线
- 线程数变化

## 性能对比
- 与基线对比
- 与历史对比

## 改进建议
- 性能优化建议
- 资源调整建议
```

---

## 7. 使用方式

### 7.1 命令行使用

```bash
# 运行所有测试
python run_local_performance.py

# 仅运行API性能测试
python run_local_performance.py --api

# 仅运行压力测试
python run_local_performance.py --stress

# 仅运行负载测试
python run_local_performance.py --load

# 运行基准测试
python run_local_performance.py --benchmark

# 指定并发用户数
python run_local_performance.py --users 100

# 指定持续时间
python run_local_performance.py --duration 60

# 生成HTML报告
python run_local_performance.py --html
```

### 7.2 配置文件使用

```yaml
# test_config.yaml
performance:
  base_url: "http://localhost:8080"
  timeout: 10
  
api_test:
  enabled: true
  iterations: 100
  
stress_test:
  enabled: true
  levels: [100, 500, 1000]
  duration: 60
  
load_test:
  enabled: true
  users: 50
  duration_minutes: 5
  
baseline:
  api_response_time_avg_ms: 100
  api_response_time_p95_ms: 200
  throughput_min_req_s: 100
  error_rate_max_percent: 1
```

---

## 8. 性能基线

| 指标 | 基线值 | 说明 |
|------|--------|------|
| API响应时间(平均) | < 100ms | API平均响应时间 |
| API响应时间(P95) | < 200ms | 95%请求响应时间 |
| 数据库查询时间(平均) | < 50ms | 数据库平均查询时间 |
| 吞吐量 | > 100 req/s | 每秒处理请求数 |
| 错误率 | < 1% | 失败请求比例 |
| 最大并发用户 | > 100 | 系统稳定支撑 |

---

## 9. 扩展性设计

### 9.1 自定义测试场景

```python
# custom_scenario.py
from performance import BaseScenario

class CustomAPIScenario(BaseScenario):
    def setup(self):
        """测试前准备"""
        pass
    
    def execute(self):
        """执行测试"""
        return self.make_request("/api/custom", "GET")
    
    def teardown(self):
        """测试后清理"""
        pass
```

### 9.2 自定义报告器

```python
# custom_reporter.py
from performance import BaseReporter

class CustomReporter(BaseReporter):
    def generate(self, results):
        """生成自定义报告"""
        return custom_format(results)
```

---

## 10. 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0 | 2026-04-03 | 初始版本，支持API性能测试 |
| 1.1 | 2026-04-03 | 添加压力测试和负载测试 |
| 1.2 | 2026-04-03 | 添加测试数据生成器 |

---

**文档生成时间**: 2026-04-03 18:45:00
