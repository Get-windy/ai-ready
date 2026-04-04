# AI-Ready 本地性能测试执行方案

## 概述

本方案提供AI-Ready系统本地性能测试的完整指南，包括API响应时间、数据库查询性能、前端渲染性能等测试场景。

---

## 测试环境要求

### 软件要求

| 软件 | 版本 | 用途 |
|------|------|------|
| Python | 3.10+ | 测试脚本执行 |
| requests | 2.28+ | HTTP请求 |
| pytest | 7.0+ | 单元测试框架 |

### 硬件要求

| 资源 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 磁盘 | 50GB SSD | 100GB SSD |

### 网络要求

- 本地服务端口: 8080
- 测试客户端与服务同机部署

---

## 测试脚本清单

| 脚本 | 路径 | 用途 |
|------|------|------|
| 本地性能测试套件 | `tests/performance/run_local_performance.py` | 整合测试入口 |
| API性能测试 | `tests/test_api_performance.py` | API响应时间测试 |
| 数据库性能测试 | `tests/run_db_performance.py` | 数据库查询性能 |
| 性能基准测试 | `tests/run_performance_benchmark.py` | 完整基准测试 |
| API响应时间测试 | `tests/run_api_performance.py` | API专项测试 |

---

## 快速开始

### 1. 安装依赖

```bash
pip install requests pytest statistics
```

### 2. 启动API服务

```bash
# 进入项目目录
cd I:\AI-Ready

# 启动服务 (根据项目实际启动方式)
java -jar target/ai-ready.jar
# 或
mvn spring-boot:run
```

### 3. 运行测试

```bash
# 运行所有测试
python tests/performance/run_local_performance.py

# 仅运行API测试
python tests/performance/run_local_performance.py --api

# 仅运行数据库测试
python tests/performance/run_local_performance.py --db

# 仅运行前端测试
python tests/performance/run_local_performance.py --frontend

# 运行完整基准测试
python tests/performance/run_local_performance.py --benchmark
```

---

## 测试场景详解

### 场景1: API响应时间测试

**目的**: 验证API接口响应时间满足性能基线

**测试内容**:
- 根路径访问测试
- 用户列表API测试
- 角色列表API测试
- API文档访问测试

**性能基线**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 100ms |
| P95响应时间 | < 200ms |
| P99响应时间 | < 500ms |

**执行方式**:
```bash
python tests/performance/run_local_performance.py --api
```

---

### 场景2: API吞吐量测试

**目的**: 验证系统并发处理能力

**测试内容**:
- 10并发测试
- 50并发测试
- 100并发测试

**性能基线**:
| 指标 | 目标值 |
|------|--------|
| 最小吞吐量 | > 100 req/s |
| 最大错误率 | < 1% |

**执行方式**:
```bash
python tests/test_api_performance.py -v -k "concurrent"
```

---

### 场景3: 数据库查询性能测试

**目的**: 验证数据库查询性能

**测试内容**:
- 单表查询性能
- 分页查询性能
- 条件查询性能
- 并发查询能力

**性能基线**:
| 指标 | 目标值 |
|------|--------|
| 单表查询时间 | < 50ms |
| 分页查询时间 | < 100ms |

**执行方式**:
```bash
python tests/performance/run_local_performance.py --db
# 或
python tests/run_db_performance.py
```

---

### 场景4: 前端渲染性能测试

**目的**: 验证前端页面加载性能

**测试内容**:
- 首页加载测试
- Swagger UI加载测试
- API文档加载测试
- 静态资源加载测试

**性能基线**:
| 指标 | 目标值 |
|------|--------|
| 页面加载时间 | < 3000ms |
| 静态资源加载 | < 500ms |

**执行方式**:
```bash
python tests/performance/run_local_performance.py --frontend
```

---

### 场景5: 持续负载测试

**目的**: 验证系统稳定性

**测试内容**:
- 30秒持续负载
- 混合API操作
- 响应时间分布分析

**执行方式**:
```bash
python tests/run_performance_benchmark.py
```

---

## 测试报告

### 报告位置

| 报告类型 | 路径 |
|----------|------|
| 本地性能测试报告 | `tests/performance/docs/AI-Ready本地性能测试报告.md` |
| API性能测试报告 | `tests/docs/api-performance-test-report.md` |
| 数据库性能测试报告 | `tests/docs/AI-Ready数据库性能测试报告.md` |
| JSON结果文件 | `tests/docs/*-test-results.json` |

### 报告内容

1. **测试概览**: 测试时间、环境、总体评分
2. **性能基线**: 各项性能指标的目标值
3. **测试结果**: 各测试项的通过/失败状态和实际值
4. **性能分析**: 性能瓶颈识别和分析
5. **优化建议**: 针对性的性能优化建议

---

## 性能优化建议

### API优化

1. **响应压缩**: 启用Gzip/Brotli压缩
2. **接口缓存**: 对频繁访问的数据添加缓存
3. **异步处理**: 耗时操作使用异步处理
4. **连接池优化**: 调整数据库连接池大小

### 数据库优化

1. **索引优化**: 为常用查询条件添加索引
2. **查询优化**: 避免SELECT *，使用合理的分页策略
3. **连接池配置**: 根据并发量调整连接池
4. **缓存层**: 添加Redis缓存减少数据库压力

### 前端优化

1. **资源压缩**: 启用静态资源压缩
2. **CDN加速**: 使用CDN分发静态资源
3. **懒加载**: 实现资源按需加载
4. **缓存策略**: 配置合理的浏览器缓存

---

## 常见问题

### Q1: API服务未启动怎么办？

测试脚本会检测API可用性，若服务未启动会跳过相关测试并给出警告。

### Q2: 如何调整性能基线？

编辑 `run_local_performance.py` 中的 `PERFORMANCE_BASELINE` 配置：

```python
PERFORMANCE_BASELINE = {
    "api_response_time_avg_ms": 100,  # 调整为实际需求
    "api_response_time_p95_ms": 200,
    # ...
}
```

### Q3: 如何添加新的测试场景？

在 `run_local_performance.py` 中添加新的测试函数，并在 `main()` 中调用：

```python
def test_new_scenario():
    """新测试场景"""
    results = []
    # 测试逻辑
    return results

# 在main()中添加
if run_all or args.new:
    TEST_RESULTS["new_tests"].extend(test_new_scenario())
```

---

## 附录

### A. 完整测试命令参考

```bash
# 运行所有测试（推荐）
python tests/performance/run_local_performance.py --all

# 分模块测试
python tests/performance/run_local_performance.py --api
python tests/performance/run_local_performance.py --db
python tests/performance/run_local_performance.py --frontend

# 使用pytest运行API性能测试
cd tests
pytest test_api_performance.py -v --tb=short

# 运行数据库性能测试
python run_db_performance.py

# 运行完整基准测试
python run_performance_benchmark.py
```

### B. 性能基线配置参考

```python
PERFORMANCE_BASELINE = {
    "api_response_time_avg_ms": 100,    # API平均响应时间
    "api_response_time_p95_ms": 200,    # API P95响应时间
    "api_response_time_p99_ms": 500,    # API P99响应时间
    "db_query_time_avg_ms": 50,         # 数据库平均查询时间
    "frontend_load_time_ms": 3000,      # 前端页面加载时间
    "throughput_min_req_s": 100,        # 最小吞吐量
    "concurrent_users": 100,            # 支持并发用户数
    "error_rate_max_percent": 1,        # 最大错误率
}
```

---

**文档版本**: v1.0  
**更新时间**: 2026-04-03  
**维护者**: test-agent-2
