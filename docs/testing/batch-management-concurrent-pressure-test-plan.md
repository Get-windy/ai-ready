# 批次管理模块并发压力测试方案

## 1. 测试目标
设计批次管理模块的并发压力测试方案，验证系统在高并发场景下的稳定性和性能表现，确保系统在真实业务负载下的可靠运行。

## 2. 测试范围

### 2.1 核心接口测试
- **批次信息查询接口**: GET /api/v1/batches/{id}/details
- **合同约束检查接口**: POST /api/v1/contracts/validate-batch
- **批次创建接口**: POST /api/v1/batches
- **批次更新接口**: PUT /api/v1/batches/{id}
- **批次删除接口**: DELETE /api/v1/batches/{id}
- **批次列表查询接口**: GET /api/v1/batches

### 2.2 业务流程测试
- 供应商门户→批次管理→价格策略完整业务流程
- 批量批次数据导入导出流程
- 并发批次处理流程

## 3. 压力测试场景设计

### 3.1 并发用户场景
| 场景编号 | 并发用户数 | 持续时间 | 业务场景 | 预期目标 |
|---------|-----------|----------|----------|----------|
| PT-001 | 100并发 | 30分钟 | 日常业务负载 | 系统稳定，响应时间≤2s |
| PT-002 | 500并发 | 30分钟 | 峰值业务负载 | 系统稳定，响应时间≤3s |
| PT-003 | 1000并发 | 30分钟 | 极端峰值负载 | 系统稳定，响应时间≤5s，错误率<1% |

### 3.2 混合业务场景
| 场景编号 | 业务组合 | 比例 | 并发数 | 目标 |
|---------|---------|------|--------|------|
| PT-101 | 查询:创建:修改:删除 | 4:3:2:1 | 500并发 | 混合业务稳定 |
| PT-102 | 复杂查询:简单操作 | 3:7 | 800并发 | 查询负载均衡 |
| PT-103 | 批量操作:单笔操作 | 1:9 | 300并发 | 批量处理优化 |

### 3.3 峰值压力场景
| 场景编号 | 场景描述 | 并发数 | 持续时间 | 监控指标 |
|---------|----------|--------|----------|----------|
| PT-201 | 秒杀场景模拟 | 2000并发 | 5分钟 | TPS、错误率、响应时间 |
| PT-202 | 批量数据导入 | 100并发批量 | 10分钟 | 处理速度、内存使用 |
| PT-203 | 复杂计算压力 | 500并发 | 15分钟 | CPU使用率、计算时间 |

### 3.4 长时间稳定性场景
| 场景编号 | 场景描述 | 并发数 | 持续时间 | 监控频率 |
|---------|----------|--------|----------|----------|
| PT-301 | 24小时稳定性测试 | 200并发 | 24小时 | 每小时一次 |
| PT-302 | 业务连续性测试 | 300并发 | 8小时 | 每30分钟一次 |
| PT-303 | 内存泄漏检测 | 100并发 | 48小时 | 每2小时一次 |

## 4. 压力测试工具配置

### 4.1 JMeter测试脚本配置
```xml
<!-- JMeter线程组配置示例 -->
<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="批次管理压力测试" enabled="true">
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>
  <longProp name="ThreadGroup.start_time">1714593600000</longProp>
  <longProp name="ThreadGroup.end_time">1714595400000</longProp>
  <boolProp name="ThreadGroup.scheduler">true</boolProp>
  <stringProp name="ThreadGroup.duration">1800</stringProp>
  <stringProp name="ThreadGroup.delay">0</stringProp>
</ThreadGroup>

<!-- HTTP请求配置 -->
<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="批次查询接口" enabled="true">
  <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="参数配置" enabled="true">
    <collectionProp name="Arguments.arguments"/>
  </elementProp>
  <stringProp name="HTTPSampler.domain">${server_host}</stringProp>
  <stringProp name="HTTPSampler.port">${server_port}</stringProp>
  <stringProp name="HTTPSampler.protocol">https</stringProp>
  <stringProp name="HTTPSampler.contentEncoding"></stringProp>
  <stringProp name="HTTPSampler.path">/api/v1/batches/${batch_id}/details</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
  <stringProp name="HTTPSampler.follow_redirects">true</stringProp>
  <stringProp name="HTTPSampler.auto_redirects">false</stringProp>
  <stringProp name="HTTPSampler.use_keepalive">true</stringProp>
  <stringProp name="HTTPSampler.DO_MULTIPART_POST">false</stringProp>
  <stringProp name="HTTPSampler.implementation">HttpClient4</stringProp>
  <stringProp name="HTTPSampler.connect_timeout"></stringProp>
  <stringProp name="HTTPSampler.response_timeout"></stringProp>
</HTTPSamplerProxy>
```

### 4.2 性能监控配置
```yaml
monitoring_config:
  system_metrics:
    cpu_usage_threshold: 80%
    memory_usage_threshold: 85%
    disk_io_threshold: 90%
    network_bandwidth_threshold: 80%
  
  application_metrics:
    response_time_p95: 3000ms
    error_rate_threshold: 1%
    throughput_target: 1000 req/sec
    concurrent_connections: 2000
    
  database_metrics:
    query_execution_time: 500ms
    connection_pool_usage: 80%
    lock_wait_time: 1000ms
    transaction_timeout: 30s
```

### 4.3 测试数据生成
```python
# test_data_generator.py
import random
import json
from datetime import datetime, timedelta

class BatchTestDataGenerator:
    def __init__(self, base_count=10000):
        self.base_count = base_count
        self.batch_types = ['PRODUCTION', 'QUALITY', 'SHIPPING', 'STORAGE']
        self.statuses = ['CREATED', 'PROCESSING', 'COMPLETED', 'CANCELLED']
    
    def generate_batch_data(self, count):
        """生成批次测试数据"""
        batches = []
        for i in range(count):
            batch_id = f"BATCH-{100000 + i}"
            batch = {
                "batch_id": batch_id,
                "batch_number": f"20240501-{i:04d}",
                "batch_type": random.choice(self.batch_types),
                "status": random.choice(self.statuses),
                "created_at": datetime.now().isoformat(),
                "updated_at": datetime.now().isoformat(),
                "supplier_id": f"SUP-{random.randint(1000, 9999)}",
                "product_id": f"PROD-{random.randint(10000, 99999)}",
                "quantity": random.randint(100, 10000),
                "unit_price": round(random.uniform(10.0, 1000.0), 2),
                "total_amount": 0,
                "contract_id": f"CONTRACT-{random.randint(100, 999)}",
                "notes": f"测试批次数据 #{i+1}"
            }
            batch["total_amount"] = round(batch["quantity"] * batch["unit_price"], 2)
            batches.append(batch)
        return batches
    
    def generate_concurrent_test_data(self, scenario_type, user_count):
        """生成并发测试数据"""
        test_data = {
            "scenario": scenario_type,
            "user_count": user_count,
            "test_start_time": datetime.now().isoformat(),
            "batches": self.generate_batch_data(self.base_count),
            "users": []
        }
        
        for user_id in range(1, user_count + 1):
            user_data = {
                "user_id": f"TEST-USER-{user_id:04d}",
                "role": random.choice(['ADMIN', 'OPERATOR', 'VIEWER']),
                "assigned_batches": random.sample(
                    [b["batch_id"] for b in test_data["batches"]], 
                    random.randint(5, 20)
                ),
                "operation_frequency": random.randint(1, 10)  # 操作频率（次/分钟）
            }
            test_data["users"].append(user_data)
        
        return test_data

# 生成测试数据
generator = BatchTestDataGenerator()
test_data_100 = generator.generate_concurrent_test_data("100并发场景", 100)
test_data_500 = generator.generate_concurrent_test_data("500并发场景", 500)
test_data_1000 = generator.generate_concurrent_test_data("1000并发场景", 1000)

# 保存测试数据
with open("batch_concurrent_test_data_100.json", "w", encoding="utf-8") as f:
    json.dump(test_data_100, f, indent=2, ensure_ascii=False)

with open("batch_concurrent_test_data_500.json", "w", encoding="utf-8") as f:
    json.dump(test_data_500, f, indent=2, ensure_ascii=False)

with open("batch_concurrent_test_data_1000.json", "w", encoding="utf-8") as f:
    json.dump(test_data_1000, f, indent=2, ensure_ascii=False)
```

## 5. 测试执行计划

### 5.1 第一阶段：基础性能验证（5月2-3日）
- 目标：验证单接口基础性能
- 范围：批次查询、创建、更新、删除接口
- 并发：50、100、200并发
- 指标：响应时间、TPS、错误率

### 5.2 第二阶段：混合业务测试（5月4-5日）
- 目标：验证混合业务场景性能
- 范围：业务流程组合测试
- 并发：300、500、800并发
- 指标：系统资源使用、事务成功率

### 5.3 第三阶段：峰值压力测试（5月6日）
- 目标：验证系统极限处理能力
- 范围：秒杀场景、批量导入
- 并发：1000、2000并发
- 指标：系统稳定性、错误处理

### 5.4 第四阶段：稳定性测试（5月7日）
- 目标：验证长时间运行稳定性
- 范围：24小时持续压力测试
- 并发：200并发持续运行
- 指标：内存泄漏、资源回收

## 6. 监控与分析

### 6.1 实时监控指标
```python
# monitoring_dashboard.py
class PressureTestMonitor:
    def __init__(self):
        self.metrics = {
            "response_times": [],
            "error_counts": [],
            "throughput": [],
            "resource_usage": []
        }
    
    def record_metric(self, metric_type, value, timestamp):
        """记录监控指标"""
        self.metrics[metric_type].append({
            "timestamp": timestamp,
            "value": value
        })
    
    def generate_report(self):
        """生成测试报告"""
        report = {
            "test_summary": {
                "total_requests": len(self.metrics["response_times"]),
                "success_rate": self.calculate_success_rate(),
                "avg_response_time": self.calculate_avg_response_time(),
                "peak_tps": self.calculate_peak_tps()
            },
            "performance_analysis": self.analyze_performance(),
            "issue_findings": self.identify_issues(),
            "recommendations": self.generate_recommendations()
        }
        return report
```

### 6.2 问题识别规则
```yaml
issue_detection_rules:
  response_time_degradation:
    threshold: 200%  # 响应时间增长超过200%
    duration: 5分钟
    severity: HIGH
    
  error_rate_increase:
    threshold: 5%    # 错误率超过5%
    duration: 3分钟
    severity: CRITICAL
    
  resource_saturation:
    cpu_threshold: 90%
    memory_threshold: 95%
    duration: 10分钟
    severity: HIGH
    
  throughput_decline:
    threshold: 50%   # 吞吐量下降50%
    duration: 5分钟
    severity: MEDIUM
```

## 7. 风险评估与应对

### 7.1 风险识别
| 风险类型 | 概率 | 影响 | 应对措施 |
|---------|------|------|----------|
| 数据库连接池耗尽 | 中 | 高 | 增加连接池大小，优化连接管理 |
| 内存泄漏 | 低 | 高 | 加强内存监控，定期重启 |
| 网络带宽瓶颈 | 中 | 中 | 增加带宽，优化数据传输 |
| 第三方服务超时 | 高 | 中 | 设置合理超时，添加重试机制 |
| 缓存穿透 | 中 | 高 | 使用布隆过滤器，空值缓存 |

### 7.2 应急预案
```python
# emergency_plan.py
class PressureTestEmergencyPlan:
    def __init__(self):
        self.plans = {
            "database_overload": self.handle_database_overload,
            "memory_leak": self.handle_memory_leak,
            "network_congestion": self.handle_network_congestion,
            "service_timeout": self.handle_service_timeout
        }
    
    def execute_emergency_plan(self, issue_type, severity):
        """执行应急预案"""
        if issue_type in self.plans:
            print(f"执行应急预案: {issue_type}, 严重程度: {severity}")
            self.plans[issue_type](severity)
        else:
            print(f"未知问题类型: {issue_type}")
```

## 8. 验收标准

### 8.1 性能指标要求
| 指标 | 100并发 | 500并发 | 1000并发 | 2000并发 |
|------|---------|---------|----------|----------|
| 平均响应时间 | ≤500ms | ≤1000ms | ≤2000ms | ≤3000ms |
| P95响应时间 | ≤1000ms | ≤2000ms | ≤3000ms | ≤5000ms |
| 错误率 | <0.1% | <0.5% | <1% | <2% |
| TPS | ≥200 | ≥500 | ≥800 | ≥1000 |
| CPU使用率 | ≤70% | ≤80% | ≤85% | ≤90% |

### 8.2 稳定性要求
1. 24小时持续测试中，系统不可用时间不超过5分钟
2. 内存使用率在测试期间保持稳定（波动不超过±10%）
3. 数据库连接池使用率不超过80%
4. 网络延迟波动不超过基准值的±20%

## 9. 交付物

### 9.1 文档交付物
1. 本压力测试方案文档
2. JMeter测试脚本文件集
3. 测试数据生成脚本
4. 监控配置脚本
5. 应急预案文档

### 9.2 报告交付物
1. 性能测试执行报告
2. 问题识别与分析报告
3. 系统优化建议报告
4. 风险评估报告
5. 验收测试报告

## 10. 资源需求

### 10.1 测试环境
- 测试服务器：4核8G × 3台（应用、数据库、监控各一台）
- 网络环境：千兆局域网，独立测试网络
- 数据库：MySQL 8.0，16G内存，SSD存储

### 10.2 工具软件
- 性能测试工具：JMeter 5.5, Gatling 3.9
- 监控工具：Prometheus + Grafana
- 代码版本：Git 2.40+
- 开发环境：Python 3.9+, Java 11+

### 10.3 人力资源
- 测试工程师：2人（测试执行、监控）
- 开发工程师：1人（问题修复支持）
- DBA：1人（数据库性能优化）
- 运维工程师：1人（环境维护）

---

**方案版本**: 1.0  
**编制日期**: 2026-05-01  
**编制人**: test-agent-2  
**审核人**: 待审核  
**批准人**: 待批准