# Sprint 27+1 测试环境性能测试设计方案

## 📋 项目概述
本文档描述AI-Ready项目Sprint 27+1测试环境的性能测试设计方案。在真实测试环境尚未启动的情况下，使用Mock服务进行性能测试脚本设计和验证。

## 🎯 设计目标
1. **测试脚本设计** - 设计完整的性能测试脚本和用例
2. **Mock服务实现** - 实现模拟API服务，验证测试逻辑
3. **测试数据设计** - 设计合理的测试数据和场景
4. **验证测试逻辑** - 确保测试脚本逻辑正确，为真实环境测试做准备

## 📊 性能测试架构

### 1. 测试环境架构
```
┌─────────────────────────────────────────────────────────────┐
│                   性能测试执行环境                           │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ 性能测试    │  │ 测试数据    │  │ Mock服务    │        │
│  │ 执行器      │◄─┤ 生成器      │◄─┤ 管理器      │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│         │              │                     │             │
│         ▼              ▼                     ▼             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Mock API Gateway (8080)                │   │
│  │  - 用户管理API                                      │   │
│  │  - 订单管理API                                      │   │
│  │  - 库存管理API                                      │   │
│  │  - AI服务API                                        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2. Mock服务设计
#### 2.1 Mock API Gateway
- **端口**: 8080
- **协议**: HTTP/JSON
- **功能**: 模拟真实API Gateway的所有接口
- **特点**: 支持可配置的响应时间和成功率

#### 2.2 模拟接口列表
| 接口类型 | 端点 | 方法 | 说明 |
|----------|------|------|------|
| 用户管理 | `/api/v1/users/login` | POST | 用户登录 |
| 用户管理 | `/api/v1/users/register` | POST | 用户注册 |
| 订单管理 | `/api/v1/orders` | POST | 创建订单 |
| 订单管理 | `/api/v1/orders/{id}` | GET | 查询订单 |
| 库存管理 | `/api/v1/inventory/check` | POST | 检查库存 |
| 库存管理 | `/api/v1/inventory/deduct` | POST | 扣减库存 |
| AI服务 | `/api/v1/ai/predict` | POST | AI预测 |
| 监控 | `/actuator/health` | GET | 健康检查 |
| 统计 | `/api/stats` | GET | 统计信息 |

#### 2.3 响应时间配置
```yaml
response_time_config:
  user_login: {min: 50, max: 200, p95: 150}  # 单位：毫秒
  user_register: {min: 100, max: 300, p95: 250}
  order_create: {min: 150, max: 500, p95: 400}
  order_query: {min: 50, max: 200, p95: 150}
  inventory_check: {min: 30, max: 100, p95: 80}
  inventory_deduct: {min: 50, max: 200, p95: 150}
  ai_predict: {min: 200, max: 800, p95: 600}
```

#### 2.4 成功率配置
```yaml
success_rate_config:
  user_login: 0.99      # 99%成功率
  user_register: 0.98   # 98%成功率
  order_create: 0.97    # 97%成功率
  order_query: 0.99     # 99%成功率
  inventory_check: 0.995 # 99.5%成功率
  inventory_deduct: 0.985 # 98.5%成功率
  ai_predict: 0.96      # 96%成功率
```

## 🧪 性能测试脚本设计

### 1. 性能测试脚本结构
```
tests/performance/
├── api/
│   └── performance_benchmark_test.py    # 主性能测试脚本
├── mock_services/
│   └── mock_api_gateway.py              # Mock API Gateway服务
├── reports/                             # 测试报告目录
├── test_config.yaml                     # 测试配置
└── run_performance_tests.py             # 测试运行器
```

### 2. 性能测试类设计
#### 2.1 PerformanceBenchmark 类
- **功能**: 执行并发性能测试
- **特点**: 支持多线程并发、响应时间统计、性能评估

#### 2.2 支持的功能
- 用户登录性能测试
- 用户注册性能测试
- 订单创建性能测试
- 订单查询性能测试
- 库存检查性能测试
- 库存扣减性能测试
- AI预测性能测试

#### 2.3 性能指标收集
- 响应时间统计（最小、最大、平均、P50、P90、P95、P99）
- 请求成功率
- 系统吞吐量（请求/秒）
- 并发用户数
- 测试持续时间

### 3. 测试场景设计
#### 3.1 快速测试场景
```python
test_suite = [
    ("用户登录", benchmark.test_user_login, 10, 30),
    ("订单创建", benchmark.test_order_create, 10, 30),
    ("库存检查", benchmark.test_inventory_check, 10, 30)
]
```

#### 3.2 综合测试场景
```python
test_suite = [
    ("用户登录", benchmark.test_user_login, 10, 60),
    ("用户注册", benchmark.test_user_register, 10, 60),
    ("订单创建", benchmark.test_order_create, 10, 60),
    ("订单查询", benchmark.test_order_query, 20, 60),
    ("库存检查", benchmark.test_inventory_check, 20, 60),
    ("库存扣减", benchmark.test_inventory_deduct, 20, 60),
    ("AI预测", benchmark.test_ai_predict, 10, 60)
]
```

#### 3.3 压力测试场景
```python
test_suite = [
    ("用户登录压力测试", benchmark.test_user_login, 100, 120),
    ("订单创建压力测试", benchmark.test_order_create, 100, 120),
    ("库存扣减压力测试", benchmark.test_inventory_deduct, 100, 120)
]
```

## 📈 测试数据设计

### 1. 用户管理测试数据
```python
user_login_data = {
    "username": f"test_user_{random.randint(1, 1000)}",
    "password": "test_password"
}

user_register_data = {
    "username": f"user_{random.randint(10000, 99999)}_{int(time.time())}",
    "password": "password123",
    "email": f"test{random.randint(10000, 99999)}@example.com",
    "phone": f"138{random.randint(10000000, 99999999)}"
}
```

### 2. 订单管理测试数据
```python
order_create_data = {
    "user_id": random.randint(1000, 9999),
    "items": [
        {
            "product_id": random.randint(1, 100),
            "quantity": random.randint(1, 5),
            "price": random.randint(10, 1000)
        }
        for _ in range(random.randint(1, 3))
    ],
    "shipping_address": {
        "address": f"测试地址{random.randint(1, 100)}号",
        "city": "测试市",
        "province": "测试省",
        "postal_code": str(random.randint(100000, 999999))
    }
}
```

### 3. 库存管理测试数据
```python
inventory_check_data = {
    "product_id": random.randint(1, 100),
    "warehouse_id": random.randint(1, 5)
}

inventory_deduct_data = {
    "product_id": random.randint(1, 100),
    "quantity": random.randint(1, 10),
    "order_id": random.randint(1000, 9999),
    "deduct_type": "order"
}
```

### 4. AI服务测试数据
```python
ai_predict_data = {
    "model_name": "mock_ai_model",
    "input_data": {
        "text": "这是一个测试文本，用于AI预测性能测试。" * random.randint(1, 5),
        "features": [random.random() for _ in range(10)]
    },
    "parameters": {
        "temperature": random.uniform(0.1, 1.0),
        "max_tokens": random.randint(50, 200)
    }
}
```

## ✅ 性能验收标准

### 1. API性能验收标准
| API端点 | 平均响应时间 | P95响应时间 | 成功率 | 优先级 |
|----------|--------------|-------------|--------|--------|
| 用户登录 | ≤ 200ms | ≤ 500ms | ≥ 99% | P0 |
| 用户注册 | ≤ 300ms | ≤ 800ms | ≥ 98% | P0 |
| 订单创建 | ≤ 300ms | ≤ 800ms | ≥ 99% | P0 |
| 订单查询 | ≤ 200ms | ≤ 500ms | ≥ 99% | P0 |
| 库存检查 | ≤ 100ms | ≤ 300ms | ≥ 99% | P0 |
| 库存扣减 | ≤ 200ms | ≤ 500ms | ≥ 99.9% | P0 |
| AI预测 | ≤ 500ms | ≤ 2000ms | ≥ 96% | P1 |

### 2. 系统性能验收标准
| 测试类型 | 并发用户数 | 吞吐量 | 资源使用率 | 优先级 |
|----------|------------|--------|------------|--------|
| 基准测试 | 10-50 | ≥ 100 QPS | CPU≤70%,内存≤80% | P0 |
| 负载测试 | 50-100 | ≥ 200 QPS | CPU≤80%,内存≤85% | P0 |
| 压力测试 | 100-500 | - | 系统不崩溃 | P1 |
| 稳定性测试 | 50 | - | 无内存泄漏 | P1 |

## 🚀 测试执行计划

### 1. 第一阶段：Mock环境测试（当前阶段）
- **目标**: 验证测试脚本逻辑和设计
- **时间**: 2小时
- **内容**:
  1. 启动Mock API Gateway服务
  2. 运行快速测试场景
  3. 验证测试数据生成逻辑
  4. 验证性能指标收集逻辑
  5. 生成测试报告模板

### 2. 第二阶段：真实环境测试（待环境就绪）
- **目标**: 执行真实性能基准测试
- **时间**: 4小时
- **内容**:
  1. 启动真实测试环境
  2. 运行综合测试场景
  3. 收集真实性能数据
  4. 分析性能瓶颈
  5. 生成正式性能报告

### 3. 第三阶段：优化和验证
- **目标**: 性能优化和回归测试
- **时间**: 2小时
- **内容**:
  1. 根据测试结果进行优化
  2. 运行回归测试
  3. 验证优化效果
  4. 更新性能基准

## 📊 测试报告设计

### 1. 报告格式
- **JSON格式**: 用于程序化处理
- **Markdown格式**: 用于人工阅读
- **CSV格式**: 用于数据分析和图表

### 2. 报告内容
#### 2.1 执行摘要
- 测试环境信息
- 测试时间范围
- 总体性能指标

#### 2.2 详细结果
- 每个API端点的性能数据
- 响应时间分布
- 成功率统计
- 吞吐量分析

#### 2.3 性能评估
- 与性能基准对比
- 达标/未达标情况
- 性能瓶颈分析

#### 2.4 建议和改进
- 性能优化建议
- 配置调整建议
- 监控建议

### 3. 可视化图表
- 响应时间分布图
- 成功率趋势图
- 吞吐量对比图
- 并发性能图

## 🛠 使用指南

### 1. 环境准备
```bash
# 安装依赖
pip install requests numpy pandas matplotlib

# 切换到测试目录
cd I:\AI-Ready\tests\performance
```

### 2. 启动Mock服务
```bash
# 手动启动Mock服务
python mock_services/mock_api_gateway.py

# 或使用运行器
python run_performance_tests.py
```

### 3. 运行性能测试
```bash
# 运行完整测试套件
python api/performance_benchmark_test.py

# 或使用运行器（包含Mock服务启动）
python run_performance_tests.py
```

### 4. 查看测试报告
测试报告将生成在：
- `I:\AI-Ready\tests\performance\reports\`
- 包含JSON、Markdown和CSV格式

### 5. 访问Mock服务
- 健康检查: http://localhost:8080/actuator/health
- 统计信息: http://localhost:8080/api/stats
- API文档: 查看mock_api_gateway.py中的路由定义

## 🔧 配置说明

### 1. 测试配置 (test_config.yaml)
- 性能基准标准
- 测试场景定义
- Mock服务配置
- 报告输出配置

### 2. 环境变量配置
```bash
# 设置测试环境URL
export PERFORMANCE_TEST_BASE_URL=http://localhost:8080

# 设置测试超时时间
export PERFORMANCE_TEST_TIMEOUT=30

# 设置并发用户数
export PERFORMANCE_TEST_CONCURRENT_USERS=10
```

## 📝 注意事项

### 1. Mock服务限制
- Mock服务仅用于测试脚本逻辑验证
- 响应时间和成功率是模拟的，不代表真实性能
- 需要在真实环境中验证实际性能

### 2. 测试数据安全
- 测试数据为模拟数据，不包含真实用户信息
- 建议在生产环境使用脱敏数据进行性能测试
- 测试后清理测试数据

### 3. 资源使用
- Mock服务会占用系统资源（端口8080）
- 性能测试会消耗CPU和内存资源
- 建议在测试环境中运行

## 🎯 下一步计划

### 1. 立即执行（阶段1完成）
- ✅ 完成Mock服务设计
- ✅ 完成性能测试脚本设计
- ✅ 完成测试数据设计
- ✅ 完成测试场景设计

### 2. 等待执行（阶段2准备）
- ⏳ 等待真实测试环境就绪
- ⏳ 配置真实环境连接信息
- ⏳ 执行真实性能基准测试

### 3. 后续优化
- 🔄 根据测试结果优化测试脚本
- 🔄 添加更多测试场景
- 🔄 完善监控和告警集成

---

**文档状态**: ✅ 已完成 - 性能测试设计方案  
**创建时间**: 2026-04-27  
**更新时间**: 2026-04-27  
**负责人**: test-agent-2  

**下一步**: 执行阶段1测试，验证测试脚本逻辑