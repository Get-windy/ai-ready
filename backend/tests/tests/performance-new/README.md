# 企智连性能测试项目

## 项目概述

企智连系统性能测试方案，包含接口性能测试、业务流程性能测试和数据库性能测试。

## 目录结构

```
performance-new/
├── PERFORMANCE_TEST_PLAN.md    # 性能测试方案文档
├── README.md                    # 项目说明
├── scripts/                     # 测试脚本
│   ├── jmeter/                  # JMeter脚本
│   └── k6/                      # k6脚本
├── config/                      # 配置文件
└── reports/                     # 测试报告
```

## 测试范围

### 1. 接口性能测试
- 登录接口并发测试
- 订单创建接口压力测试
- 库存查询接口负载测试
- 报表生成接口性能测试

### 2. 业务流程性能测试
- 下单流程端到端测试
- 采购入库流程测试
- 财务结算流程测试
- 批量数据处理测试

### 3. 数据库性能测试
- 大数据量查询性能
- 并发写入性能
- 索引优化验证
- 慢查询分析

## 测试指标

| 指标 | 目标值 |
|------|--------|
| 响应时间(P95) | < 500ms |
| 响应时间(P99) | < 1s |
| 吞吐量(QPS) | > 1000 |
| 并发用户数 | > 1000 |
| 错误率 | < 0.1% |
| CPU利用率 | < 70% |
| 内存利用率 | < 80% |

## 工具选型

| 工具 | 用途 |
|------|------|
| JMeter | 接口压测 |
| k6 | 现代压测工具 |
| Prometheus | 指标采集 |
| Grafana | 可视化监控 |
| Arthas | Java应用诊断 |

## 快速开始

### 使用k6进行压测

```bash
# 安装k6
brew install k6

# 运行登录接口压测
k6 run scripts/k6/login_test.js

# 运行订单接口压测
k6 run scripts/k6/order_test.js

# 生成HTML报告
k6 run --out html=report.html scripts/k6/login_test.js
```

### 使用JMeter进行压测

```bash
# 运行JMeter测试
jmeter -n -t scripts/jmeter/login_test.jmx -l results.jtl

# 生成报告
jmeter -g results.jtl -o report
```

## 监控配置

### Prometheus + Grafana

```bash
# 启动监控
docker-compose up -d prometheus grafana

# 访问Grafana
http://localhost:3000
```

## 测试阶段

| 阶段 | 时间 | 内容 |
|------|------|------|
| 准备阶段 | Day 1 | 环境搭建、脚本准备 |
| 基准测试 | Day 2 | 单接口基准测试 |
| 负载测试 | Day 3 | 多接口负载测试 |
| 压力测试 | Day 4 | 峰值压力测试 |
| 稳定性测试 | Day 5-7 | 7x24小时测试 |
| 报告阶段 | Day 8 | 报告编写 |

## 交付物

- [x] 性能测试方案文档
- [ ] JMeter/k6测试脚本
- [ ] 测试环境配置
- [ ] 性能测试报告

---

**负责人**: QA Lead  
**创建日期**: 2026-04-14