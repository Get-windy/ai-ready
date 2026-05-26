# AI-Ready Sprint 27+1 测试环境性能测试

## 概述
本目录包含AI-Ready项目Sprint 27+1测试环境的性能测试脚本和Mock服务。在真实测试环境尚未启动的情况下，使用Mock服务进行性能测试脚本设计和验证。

## 项目结构
```
tests/performance/
├── README.md                    # 本文件
├── run_performance_tests.py     # 性能测试运行器
├── test_config.yaml             # 测试配置文件
├── api/
│   └── performance_benchmark_test.py    # 主性能测试脚本
├── mock_services/
│   └── mock_api_gateway.py              # Mock API Gateway服务
├── reports/                     # 测试报告输出目录
└── docs/                       # 文档目录
```

## 快速开始

### 1. 安装依赖
```bash
pip install requests numpy pandas matplotlib
```

### 2. 运行完整测试
```bash
python run_performance_tests.py
```

这将：
1. 检查依赖包
2. 启动Mock API Gateway服务（端口8080）
3. 运行性能测试套件
4. 生成测试报告
5. 停止Mock服务

### 3. 单独启动Mock服务
```bash
python mock_services/mock_api_gateway.py
```

### 4. 单独运行性能测试
```bash
python api/performance_benchmark_test.py
```

## Mock API Gateway服务

### 服务端点
- **健康检查**: http://localhost:8080/actuator/health
- **统计信息**: http://localhost:8080/api/stats
- **重置统计**: POST http://localhost:8080/api/reset_stats

### 模拟的API接口
| 接口 | 方法 | 端点 | 说明 |
|------|------|------|------|
| 用户登录 | POST | `/api/v1/users/login` | 模拟用户登录 |
| 用户注册 | POST | `/api/v1/users/register` | 模拟用户注册 |
| 创建订单 | POST | `/api/v1/orders` | 模拟创建订单 |
| 查询订单 | GET | `/api/v1/orders/{id}` | 模拟查询订单 |
| 检查库存 | POST | `/api/v1/inventory/check` | 模拟检查库存 |
| 扣减库存 | POST | `/api/v1/inventory/deduct` | 模拟扣减库存 |
| AI预测 | POST | `/api/v1/ai/predict` | 模拟AI预测 |

### Mock服务配置
Mock服务模拟了不同的响应时间和成功率：

#### 响应时间配置（毫秒）
```yaml
user_login: {min: 50, max: 200, p95: 150}
user_register: {min: 100, max: 300, p95: 250}
order_create: {min: 150, max: 500, p95: 400}
order_query: {min: 50, max: 200, p95: 150}
inventory_check: {min: 30, max: 100, p95: 80}
inventory_deduct: {min: 50, max: 200, p95: 150}
ai_predict: {min: 200, max: 800, p95: 600}
```

#### 成功率配置
```yaml
user_login: 0.99      # 99%成功率
user_register: 0.98   # 98%成功率
order_create: 0.97    # 97%成功率
order_query: 0.99     # 99%成功率
inventory_check: 0.995 # 99.5%成功率
inventory_deduct: 0.985 # 98.5%成功率
ai_predict: 0.96      # 96%成功率
```

## 性能测试

### 测试脚本功能
1. **并发测试**: 支持多线程并发请求
2. **性能指标收集**: 响应时间、成功率、吞吐量
3. **性能评估**: 与性能基准对比评估
4. **报告生成**: 自动生成JSON和Markdown报告

### 测试场景
测试脚本支持以下测试场景：

#### 1. 快速测试
- 用户登录 (10并发用户，30秒)
- 订单创建 (10并发用户，30秒)
- 库存检查 (10并发用户，30秒)

#### 2. 综合测试
- 用户登录 (10并发用户，60秒)
- 用户注册 (10并发用户，60秒)
- 订单创建 (10并发用户，60秒)
- 订单查询 (20并发用户，60秒)
- 库存检查 (20并发用户，60秒)
- 库存扣减 (20并发用户，60秒)
- AI预测 (10并发用户，60秒)

#### 3. 压力测试（可选）
- 用户登录 (100并发用户，120秒)
- 订单创建 (100并发用户，120秒)
- 库存扣减 (100并发用户，120秒)

### 性能基准
| API端点 | 平均响应时间 | P95响应时间 | 成功率 |
|----------|--------------|-------------|--------|
| 用户登录 | ≤ 200ms | ≤ 500ms | ≥ 99% |
| 用户注册 | ≤ 300ms | ≤ 800ms | ≥ 98% |
| 订单创建 | ≤ 300ms | ≤ 800ms | ≥ 99% |
| 订单查询 | ≤ 200ms | ≤ 500ms | ≥ 99% |
| 库存检查 | ≤ 100ms | ≤ 300ms | ≥ 99% |
| 库存扣减 | ≤ 200ms | ≤ 500ms | ≥ 99.9% |
| AI预测 | ≤ 500ms | ≤ 2000ms | ≥ 96% |

## 测试报告

### 报告位置
测试报告生成在 `reports/` 目录下：
- `performance_report_YYYYMMDD_HHMMSS.json` - JSON格式详细数据
- `performance_report_YYYYMMDD_HHMMSS.md` - Markdown格式报告

### 报告内容
1. **执行摘要**
   - 测试环境信息
   - 总体性能指标
   - 测试时间范围

2. **详细结果**
   - 每个API端点的性能数据
   - 响应时间统计（最小、平均、最大、P50、P90、P95、P99）
   - 成功率统计
   - 吞吐量分析

3. **性能评估**
   - 与性能基准对比
   - 达标/未达标情况
   - 性能评估结果

4. **建议和改进**
   - 性能优化建议
   - 配置调整建议
   - 监控建议

## 配置说明

### 配置文件
`test_config.yaml` 包含所有可配置项：
- 性能基准标准
- 测试场景定义
- Mock服务配置
- 报告输出配置

### 环境变量
```bash
# 设置测试环境URL
export PERFORMANCE_TEST_BASE_URL=http://localhost:8080

# 设置测试超时时间
export PERFORMANCE_TEST_TIMEOUT=30

# 设置并发用户数
export PERFORMANCE_TEST_CONCURRENT_USERS=10
```

## 注意事项

### Mock服务限制
1. Mock服务仅用于测试脚本逻辑验证
2. 响应时间和成功率是模拟的，不代表真实性能
3. 需要在真实环境中验证实际性能

### 资源使用
1. Mock服务占用端口8080
2. 性能测试会消耗CPU和内存资源
3. 建议在测试环境中运行

### 测试数据安全
1. 测试数据为模拟数据，不包含真实用户信息
2. 建议在生产环境使用脱敏数据进行性能测试
3. 测试后清理测试数据

## 故障排除

### 常见问题
1. **端口8080被占用**
   ```
   netstat -ano | findstr :8080
   taskkill /PID [PID] /F
   ```

2. **Python依赖包缺失**
   ```
   pip install -r requirements.txt
   ```

3. **Mock服务启动失败**
   - 检查Python版本（需要Python 3.7+）
   - 检查Flask是否安装
   - 检查端口是否被占用

### 日志查看
1. Mock服务控制台会输出启动日志
2. 性能测试脚本会输出详细测试结果
3. 错误信息会显示在控制台

## 下一步计划

### 阶段1：Mock环境测试（已完成）
- ✅ 完成Mock服务设计
- ✅ 完成性能测试脚本设计
- ✅ 完成测试数据设计
- ✅ 完成测试场景设计

### 阶段2：真实环境测试（待执行）
- ⏳ 等待真实测试环境就绪
- ⏳ 配置真实环境连接信息
- ⏳ 执行真实性能基准测试

### 阶段3：优化和验证（待执行）
- 🔄 根据测试结果优化测试脚本
- 🔄 添加更多测试场景
- 🔄 完善监控和告警集成

## 联系信息
- **项目**: AI-Ready Sprint 27+1
- **负责人**: test-agent-2
- **文档位置**: `I:\AI-Ready\docs\sprint-27-plus-1-performance-test-design.md`

---

**最后更新**: 2026-04-27  
**状态**: ✅ 第一阶段设计完成