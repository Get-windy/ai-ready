# 批次管理性能测试执行指南

## 1. 测试环境准备

### 1.1 前置条件检查
```bash
# 检查Java版本
java -version

# 检查JMeter安装
jmeter -v

# 检查Python环境
python --version

# 检查依赖包
pip install -r requirements.txt
```

### 1.2 测试数据生成
```bash
# 生成测试数据
cd I:\AI-Ready\backend\tests\performance\scripts
python generate-test-data.py

# 数据文件说明
# - data/batch-data.csv: 批次数据 (1,000,000条)
# - data/price-data.csv: 价格数据 (100,000条)
# - data/traceability-data.csv: 追溯数据 (500,000条)
# - data/import-export-data.csv: 导入导出数据 (100,000条)
```

## 2. JMeter测试脚本说明

### 2.1 现有脚本
- `batch-creation.jmx`: 批次创建并发性能测试

### 2.2 脚本配置参数
| 参数名 | 说明 | 默认值 |
|--------|------|--------|
| base_url | 应用服务器地址 | http://localhost:8080 |
| api_path | API基础路径 | /api/erp/batch-sn/batches |
| thread_count | 并发线程数 | 50 |
| ramp_up_time | 启动时间(秒) | 60 |
| test_duration | 测试持续时间(秒) | 600 |

### 2.3 运行JMeter测试
```bash
# 运行批次创建测试
jmeter -n -t batch-creation.jmx -l results/batch-creation.jtl -e -o reports/batch-creation/

# 查看HTML报告
start reports/batch-creation/index.html
```

## 3. 创建缺失的JMeter测试脚本

### 3.1 批次查询性能测试脚本
创建 `batch-query.jmx` 用于测试：
- 简单查询：按批次号、产品编码查询
- 复杂查询：多条件组合查询
- 分页查询：大数据量分页性能

### 3.2 价格策略计算性能测试脚本
创建 `price-calculation.jmx` 用于测试：
- 单个批次价格计算
- 批量批次价格计算
- 复杂价格策略计算

### 3.3 批次追溯性能测试脚本
创建 `batch-traceability.jmx` 用于测试：
- 单层追溯查询
- 多层关联追溯
- 全链路追溯分析

### 3.4 大数据导入导出性能测试脚本
创建 `import-export.jmx` 用于测试：
- CSV数据导入
- Excel数据导出
- 批量数据同步

## 4. 监控配置

### 4.1 Prometheus配置
```yaml
# prometheus.yml 批次管理相关配置
scrape_configs:
  - job_name: 'erp-batch-sn'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'postgresql'
    static_configs:
      - targets: ['localhost:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']
```

### 4.2 Grafana仪表板配置
创建以下监控仪表板：
1. **应用性能仪表板**:
   - 响应时间趋势图
   - 吞吐量监控图
   - 错误率监控图

2. **数据库性能仪表板**:
   - 查询耗时分析
   - 连接池使用情况
   - 锁等待监控

3. **系统资源仪表板**:
   - CPU/内存使用率
   - 磁盘IO性能
   - 网络流量监控

### 4.3 JVM监控配置
```bash
# JVM启动参数
java -jar erp-batch-sn.jar \
  -XX:+UseG1GC \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Xloggc:logs/gc.log \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=logs/heapdump.hprof
```

## 5. 测试执行流程

### 5.1 预测试准备阶段（第1天）
1. 环境检查和部署
2. 测试数据生成
3. 监控系统配置
4. 基准性能测试

### 5.2 负载测试阶段（第2-3天）
1. 批次创建并发测试
2. 批次查询性能测试
3. 价格计算性能测试

### 5.3 压力测试阶段（第4-5天）
1. 极限并发测试
2. 大数据量处理测试
3. 长时间稳定性测试

### 5.4 专项测试阶段（第6天）
1. 批次追溯性能测试
2. 数据导入导出测试
3. 故障恢复测试

### 5.5 测试分析和报告（第7天）
1. 性能数据分析
2. 瓶颈识别和优化建议
3. 测试报告编写

## 6. 测试结果分析

### 6.1 关键性能指标检查表
| 指标 | 目标值 | 实际值 | 是否达标 | 备注 |
|------|--------|--------|----------|------|
| 平均响应时间 | < 200ms | | | |
| 95%响应时间 | < 500ms | | | |
| TPS | > 50 | | | |
| 错误率 | < 0.1% | | | |
| CPU使用率 | < 70% | | | |
| 内存使用率 | < 80% | | | |

### 6.2 性能瓶颈分析方法
1. **应用层瓶颈**:
   - 使用JProfiler分析热点方法
   - 检查线程池配置
   - 分析GC日志

2. **数据库瓶颈**:
   - 使用pg_stat_statements分析慢查询
   - 检查索引使用情况
   - 分析锁等待情况

3. **网络瓶颈**:
   - 使用Wireshark分析网络包
   - 检查连接池配置
   - 分析TCP连接状态

## 7. 测试报告模板

### 7.1 报告结构
```
1. 测试概述
2. 测试环境
3. 测试执行情况
4. 性能测试结果
5. 瓶颈分析和优化建议
6. 测试结论
7. 附件
```

### 7.2 自动生成测试报告脚本
```python
# report-generator.py
# 从JMeter结果和监控数据生成HTML测试报告
```

## 8. 故障排除指南

### 8.1 常见问题
1. **JMeter报错**: 连接超时、内存不足
2. **应用错误**: 数据库连接失败、缓存异常
3. **监控问题**: Prometheus无法采集数据

### 8.2 解决方案
1. 调整JMeter JVM内存参数
2. 检查应用日志和数据库连接
3. 验证监控端点可用性

## 9. 后续优化建议

### 9.1 短期优化（1-2周）
1. 调整数据库索引
2. 优化缓存策略
3. 调整线程池配置

### 9.2 中期优化（1-2月）
1. 代码性能优化
2. 数据库分库分表
3. 引入CDN缓存

### 9.3 长期优化（3-6月）
1. 架构重构
2. 微服务拆分
3. 引入消息队列异步处理
```

## 10. 快速启动脚本

### 10.1 一键启动测试环境
```bash
# start-test-env.bat
@echo off
echo 启动批次管理性能测试环境...

REM 启动应用
echo 启动批次管理应用...
java -jar erp-batch-sn.jar --spring.profiles.active=performance

REM 启动监控
echo 启动监控系统...
docker-compose -f docker/monitoring/docker-compose.yml up -d

REM 生成测试数据
echo 生成测试数据...
python scripts/generate-test-data.py

echo 测试环境准备完成！
```

### 10.2 一键执行所有测试
```bash
# run-all-tests.bat
@echo off
echo 执行批次管理性能测试套件...

REM 执行批次创建测试
echo 执行批次创建性能测试...
jmeter -n -t scripts/jmeter/batch-creation.jmx -l results/batch-creation.jtl

REM 执行批次查询测试
echo 执行批次查询性能测试...
jmeter -n -t scripts/jmeter/batch-query.jmx -l results/batch-query.jtl

REM 执行价格计算测试
echo 执行价格计算性能测试...
jmeter -n -t scripts/jmeter/price-calculation.jmx -l results/price-calculation.jtl

REM 生成测试报告
echo 生成测试报告...
python scripts/report-generator.py

echo 所有测试执行完成！
```

---

**最后更新时间**: 2026-05-04  
**版本**: v1.0.0  
**维护者**: 性能测试团队