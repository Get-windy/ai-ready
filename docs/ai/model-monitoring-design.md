# AI模型性能监控指标设计文档

**文档版本**: v1.0
**创建日期**: 2026-04-27
**所属项目**: AI-Ready（企智连系统）
**Sprint**: Sprint 27+1

---

## 1. 文档概述

### 1.1 目标
为AI-Ready项目的AI服务建立完整的性能监控指标体系，确保AI模型在生产环境中的稳定运行和性能可观测性。

### 1.2 范围
本设计文档涵盖以下AI模块的监控指标：
- 智能审批助手（AI Approval Assistant）
- 智能对话模块（AI Chat Module）
- 其他AI服务组件

---

## 2. 监控指标分类

### 2.1 模型推理延迟监控指标

#### 2.1.1 基础延迟指标

| 指标名称 | 指标类型 | 单位 | 描述 | 告警阈值 |
|---------|---------|------|------|---------|
| `ai_inference_latency_p50` | Histogram | 毫秒 | 50%分位推理延迟 | < 500ms |
| `ai_inference_latency_p95` | Histogram | 毫秒 | 95%分位推理延迟 | < 1000ms |
| `ai_inference_latency_p99` | Histogram | 毫秒 | 99%分位推理延迟 | < 2000ms |
| `ai_inference_latency_avg` | Gauge | 毫秒 | 平均推理延迟 | < 800ms |
| `ai_inference_latency_max` | Gauge | 毫秒 | 最大推理延迟 | < 3000ms |

#### 2.1.2 分阶段延迟指标

| 指标名称 | 指标类型 | 单位 | 描述 |
|---------|---------|------|------|
| `ai_preprocess_latency` | Histogram | 毫秒 | 数据预处理耗时 |
| `ai_model_execution_latency` | Histogram | 毫秒 | 模型实际执行耗时 |
| `ai_postprocess_latency` | Histogram | 毫秒 | 结果后处理耗时 |
| `ai_network_latency` | Histogram | 毫秒 | 网络传输耗时 |

#### 2.1.3 告警规则

**P95延迟告警**:
- 触发条件：`ai_inference_latency_p95` > 1500ms，持续3分钟
- 告警级别：WARNING
- 通知方式：企业微信群组 + 邮件

**P99延迟告警**:
- 触发条件：`ai_inference_latency_p99` > 3000ms，持续2分钟
- 告警级别：CRITICAL
- 通知方式：企业微信群组 + 邮件 + 短信

---

### 2.2 模型准确率/召回率监控指标

#### 2.2.1 质量指标（离线评估）

| 指标名称 | 指标类型 | 描述 | 告警阈值 |
|---------|---------|------|---------|
| `ai_model_accuracy` | Gauge | 模型准确率（批次评估） | 下降 > 5% |
| `ai_model_precision` | Gauge | 模型精确率（批次评估） | 下降 > 5% |
| `ai_model_recall` | Gauge | 模型召回率（批次评估） | 下降 > 5% |
| `ai_model_f1_score` | Gauge | F1分数（批次评估） | 下降 > 5% |

#### 2.2.2 实时质量指标（在线反馈）

| 指标名称 | 指标类型 | 描述 | 告警阈值 |
|---------|---------|------|---------|
| `ai_user_acceptance_rate` | Gauge | 用户采纳率（反馈采纳/总反馈） | < 85% |
| `ai_user_rejection_rate` | Gauge | 用户拒绝率（用户拒绝/总交互） | > 10% |
| `ai_rating_avg` | Gauge | 用户平均评分（1-5分） | < 3.5 |

#### 2.2.3 模型漂移检测指标

| 指标名称 | 指标类型 | 描述 | 告警阈值 |
|---------|---------|------|---------|
| `ai_feature_drift_score` | Gauge | 特征漂移分数（输入分布变化） | > 0.8 |
| `ai_prediction_drift_score` | Gauge | 预测漂移分数（输出分布变化） | > 0.8 |
| `ai_data_drift_detected` | Counter | 数据漂移检测次数 | 持续检测 |

#### 2.2.4 告警规则

**准确率下降告警**:
- 触发条件：`ai_model_accuracy` 较基线下降 > 5%，持续24小时
- 告警级别：WARNING
- 通知方式：企业微信群组 + 邮件

**用户拒绝率告警**:
- 触发条件：`ai_user_rejection_rate` > 15%，持续1小时
- 告警级别：WARNING
- 通知方式：企业微信群组

**模型漂移告警**:
- 触发条件：`ai_feature_drift_score` > 0.9，持续6小时
- 告警级别：CRITICAL
- 通知方式：企业微信群组 + 邮件 + 短信

---

### 2.3 资源使用率监控指标

#### 2.3.1 GPU资源指标（如果使用）

| 指标名称 | 指标类型 | 单位 | 描述 | 告警阈值 |
|---------|---------|------|------|---------|
| `ai_gpu_utilization` | Gauge | 百分比 | GPU利用率 | > 90% |
| `ai_gpu_memory_used` | Gauge | MB | GPU显存使用量 | > 90% 总量 |
| `ai_gpu_memory_available` | Gauge | MB | GPU可用显存 | < 10% 总量 |
| `ai_gpu_temperature` | Gauge | 摄氏度 | GPU温度 | > 80°C |

#### 2.3.2 CPU资源指标

| 指标名称 | 指标类型 | 单位 | 描述 | 告警阈值 |
|---------|---------|------|------|---------|
| `ai_cpu_usage` | Gauge | 百分比 | AI服务CPU使用率 | > 85% |
| `ai_cpu_cores_used` | Gauge | 核心数 | CPU核心使用数 | - |
| `ai_cpu_wait_time` | Histogram | 毫秒 | CPU等待时间 | - |

#### 2.3.3 内存资源指标

| 指标名称 | 指标类型 | 单位 | 描述 | 告警阈值 |
|---------|---------|------|------|---------|
| `ai_memory_used` | Gauge | MB | AI服务内存使用量 | > 85% 总量 |
| `ai_memory_available` | Gauge | MB | 可用内存 | < 15% 总量 |
| `ai_memory_usage_heap` | Gauge | 百分比 | JVM堆内存使用率 | > 85% |
| `ai_memory_gc_time` | Histogram | 毫秒 | GC停顿时间 | P99 > 500ms |

#### 2.3.4 告警规则

**CPU使用率告警**:
- 触发条件：`ai_cpu_usage` > 90%，持续5分钟
- 告警级别：WARNING
- 通知方式：企业微信群组

**内存使用率告警**:
- 触发条件：`ai_memory_used` > 90%，持续5分钟
- 告警级别：WARNING
- 通知方式：企业微信群组

**GPU温度告警**:
- 触发条件：`ai_gpu_temperature` > 85°C，持续2分钟
- 告警级别：CRITICAL
- 通知方式：企业微信群组 + 邮件

---

### 2.4 业务指标

#### 2.4.1 请求量指标

| 指标名称 | 指标类型 | 单位 | 描述 |
|---------|---------|------|------|
| `ai_requests_total` | Counter | 次数 | 总请求数 |
| `ai_requests_success` | Counter | 次数 | 成功请求数 |
| `ai_requests_failed` | Counter | 次数 | 失败请求数 |
| `ai_requests_per_second` | Gauge | 次/秒 | 每秒请求数（QPS） |

#### 2.4.2 错误率指标

| 指标名称 | 指标类型 | 单位 | 描述 | 告警阈值 |
|---------|---------|------|------|---------|
| `ai_error_rate` | Gauge | 百分比 | 错误率 | > 1% |
| `ai_timeout_rate` | Gauge | 百分比 | 超时率 | > 0.5% |
| `ai_rate_limit_rejected` | Counter | 次数 | 限流拒绝次数 | - |

#### 2.4.3 告警规则

**错误率告警**:
- 触发条件：`ai_error_rate` > 2%，持续5分钟
- 告警级别：WARNING
- 通知方式：企业微信群组

**错误率严重告警**:
- 触发条件：`ai_error_rate` > 5%，持续2分钟
- 告警级别：CRITICAL
- 通知方式：企业微信群组 + 邮件 + 短信

---

## 3. 指标数据采集方案

### 3.1 采集频率

| 指标类别 | 采集频率 | 存储周期 |
|---------|---------|---------|
| 延迟指标 | 10秒 | 30天 |
| 资源指标 | 15秒 | 30天 |
| 质量指标（离线） | 每日 | 1年 |
| 业务指标 | 10秒 | 90天 |
| 漂移检测 | 每小时 | 90天 |

### 3.2 数据采集方式

1. **应用埋点**: 通过Micrometer在应用代码中埋点
2. **JMX采集**: 通过JMX Exporter采集JVM指标
3. **系统监控**: 通过Node Exporter采集系统指标
4. **GPU监控**: 通过DCGM Exporter采集GPU指标

---

## 4. 指标计算规则

### 4.1 分位数计算
- 采用Prometheus的histogram_quantile函数
- 保留窗口：10分钟（用于实时告警）、1小时（用于趋势分析）

### 4.2 基线设定
- 初始基线：测试环境基线数据
- 动态基线：滚动7天平均值
- 基线更新频率：每日

### 4.3 漂移检测算法
- 特征漂移：KL散度（Kullback-Leibler divergence）
- 预测漂移：Population Stability Index (PSI)
- 阈值：PSI > 0.8 视为显著漂移

---

## 5. 指标仪表盘设计

### 5.1 实时监控仪表盘
- 实时请求量、QPS、错误率
- P50/P95/P99延迟趋势
- 资源使用率（CPU/GPU/内存）
- 近1小时关键指标变化

### 5.2 质量监控仪表盘
- 模型准确率、精确率、召回率趋势
- 用户采纳率、拒绝率趋势
- 用户评分分布
- 模型漂移检测状态

### 5.3 资源监控仪表盘
- GPU利用率、显存使用、温度
- CPU使用率、核心分配
- 内存使用、堆内存、GC时间
- 容器资源限制 vs 实际使用

---

## 6. 附录

### 6.1 指标命名规范
- 前缀：`ai_`
- 中间段：模块_类别_具体指标
- 后缀：单位（如_latency_ms、_usage_percent）

### 6.2 监控优先级
- **P0（必须监控）**: 延迟、错误率、资源使用率
- **P1（重要监控）**: 用户采纳率、模型准确率、GPU指标
- **P2（可选监控）**: 漂移检测、详细GC指标

### 6.3 与现有系统集成
- 与Prometheus集成：通过Pushgateway或暴露/metrics端点
- 与Grafana集成：配置仪表盘JSON文件
- 与企业微信集成：通过Webhook发送告警

---

**文档作者**: AI-Ready测试团队
**审核状态**: 待审核
**下一步**: 开始性能优化方案设计