# 批次管理模块数据归档与长期存储策略设计

## 1. 概述

### 1.1 目标
为批次管理模块设计一套完整的数据归档和长期存储策略，实现：
- 主数据库性能优化
- 历史数据安全存储
- 高效检索与合规管理
- 存储成本优化

### 1.2 适用范围
本策略适用于ERP批次管理模块的以下数据：
- 批次/序列号主数据
- 流转记录
- 质量检验记录
- 审计日志
- 追溯数据

## 2. 归档需求分析

### 2.1 归档对象分析

| 数据表 | 归档条件 | 归档周期 | 数据特点 |
|--------|----------|----------|----------|
| batch_number | 批次状态=CANCELLED/EXPIRED，且更新时间>3年 | 年归档 | 历史批次信息 |
| serial_number | 序列号状态=SCRAP，且更新时间>5年 | 年归档 | 废弃序列号 |
| batch_flow_record | 记录时间>2年 | 月归档 | 高频流转记录 |
| serial_flow_record | 记录时间>3年 | 月归档 | 序列号流转历史 |
| quality_inspection_log | 质检日期>5年 | 季归档 | 质量追溯数据 |
| audit_log | 记录时间>1年 | 月归档 | 审计合规数据 |

### 2.2 归档策略维度

#### 2.2.1 时间维度归档
- **热数据**：最近3个月数据，频繁访问
- **温数据**：3个月-2年数据，较少访问  
- **冷数据**：2年以上数据，很少访问
- **冻结数据**：5年以上数据，仅合规查询

#### 2.2.2 业务维度归档
- **活跃业务数据**：当前正在执行的业务流程
- **已完成业务数据**：业务流程已完结，可归档
- **异常业务数据**：存在质量问题或争议，需保留

#### 2.2.3 使用频率归档
- **高频访问**：近期的批次流转记录
- **中频访问**：历史批次信息
- **低频访问**：过期的质检记录
- **极低频访问**：合规保留的审计数据

### 2.3 法律法规要求

#### 2.3.1 国内法规要求
- **《中华人民共和国药品管理法》**：药品批次记录保存至药品有效期后至少2年
- **《食品安全法》**：食品生产记录保存期限不得少于2年
- **《医疗器械监督管理条例》**：医疗器械生产记录保存期限不得少于2年

#### 2.3.2 行业标准
- **GSP（药品经营质量管理规范）**：药品经营记录保存至有效期后1年
- **ISO 9001质量管理体系**：质量记录保存期限应明确规定并执行
- **FDA 21 CFR Part 11**：电子记录和电子签名要求

## 3. 技术架构设计

### 3.1 存储架构

```
┌─────────────────────────────────────────────────────────────┐
│                   归档存储架构                              │
├─────────────────────────────────────────────────────────────┤
│  Tier 0：在线主数据库 (MySQL/PostgreSQL)                    │
│  └── 热数据（最近3个月）                                    │
├─────────────────────────────────────────────────────────────┤
│  Tier 1：归档数据库 (MySQL/PostgreSQL副本)                  │
│  └── 温数据（3个月-2年）                                    │
├─────────────────────────────────────────────────────────────┤
│  Tier 2：列式存储数据库 (ClickHouse/TiDB)                   │
│  └── 冷数据（2-5年），支持快速分析查询                       │
├─────────────────────────────────────────────────────────────┤
│  Tier 3：对象存储 (MinIO/OSS/S3)                            │
│  └── 冻结数据（5年以上），压缩存储                          │
├─────────────────────────────────────────────────────────────┤
│  Tier 4：磁带存储/冷存储                                     │
│  └── 合规保留数据（10年以上）                                │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 数据库设计

#### 3.2.1 归档主表设计
```sql
-- 批次归档表
CREATE TABLE batch_number_archive (
    id BIGINT PRIMARY KEY COMMENT '原始ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    -- ... 所有原始字段
    archive_reason VARCHAR(50) COMMENT '归档原因: EXPIRED/CANCELLED/LOW_FREQUENCY',
    archive_date DATE NOT NULL COMMENT '归档日期',
    archive_method VARCHAR(20) COMMENT '归档方式: AUTO/MANUAL',
    archive_version INT DEFAULT 1 COMMENT '归档版本',
    compression_ratio DECIMAL(5,2) COMMENT '压缩比',
    storage_tier VARCHAR(20) COMMENT '存储层级: TIER1/TIER2/TIER3',
    storage_path VARCHAR(500) COMMENT '存储路径',
    retrieval_cost DECIMAL(10,4) COMMENT '检索成本估计',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '批次数据归档表';

-- 创建分区表（按归档年份分区）
PARTITION BY RANGE (archive_date);
```

#### 3.2.2 归档索引策略
```sql
-- 业务查询索引
CREATE INDEX idx_archive_batch_no ON batch_number_archive(batch_no);
CREATE INDEX idx_archive_product_date ON batch_number_archive(product_id, archive_date);
CREATE INDEX idx_archive_status_date ON batch_number_archive(archive_reason, archive_date);

-- 分区索引
CREATE INDEX idx_archive_partition ON batch_number_archive(archive_date);
```

### 3.3 接口设计

#### 3.3.1 归档管理接口
```java
/**
 * 数据归档服务接口
 */
public interface DataArchiveService {
    
    /**
     * 自动归档过期数据
     */
    ArchiveResult autoArchiveExpiredData(AutoArchiveConfig config);
    
    /**
     * 手动归档指定数据
     */
    ArchiveResult manualArchive(ArchiveRequest request);
    
    /**
     * 查询归档数据
     */
    ArchiveDataQueryResult queryArchiveData(ArchiveQuery query);
    
    /**
     * 恢复归档数据
     */
    RestoreResult restoreFromArchive(RestoreRequest request);
    
    /**
     * 归档统计报告
     */
    ArchiveStatistics getArchiveStatistics(StatisticsRequest request);
}
```

#### 3.3.2 归档配置接口
```java
/**
 * 归档配置管理
 */
public interface ArchiveConfigService {
    
    /**
     * 获取归档策略配置
     */
    ArchivePolicy getArchivePolicy(String module);
    
    /**
     * 更新归档策略
     */
    void updateArchivePolicy(ArchivePolicy policy);
    
    /**
     * 获取存储层级配置
     */
    StorageTierConfig getStorageTierConfig();
    
    /**
     * 调整存储层级
     */
    void adjustStorageTier(StorageTierAdjustment adjustment);
}
```

## 4. 实施计划

### 4.1 第一阶段：基础架构搭建（2周）
- 归档数据库环境搭建
- 归档表结构设计
- 基础归档接口开发
- 归档任务调度框架

### 4.2 第二阶段：自动归档功能（3周）
- 自动归档规则配置
- 归档数据迁移工具
- 归档验证机制
- 归档监控告警

### 4.3 第三阶段：存储优化（2周）
- 多层级存储集成
- 数据压缩算法优化
- 检索性能优化
- 成本控制机制

### 4.4 第四阶段：合规与审计（2周）
- 合规性检查
- 审计日志记录
- 数据完整性验证
- 灾难恢复方案

## 5. 技术实现方案

### 5.1 归档流程设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   数据筛选   │────>│   数据迁移   │────>│   数据验证   │
└─────────────┘     └─────────────┘     └─────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   压缩存储   │────>│   索引创建   │────>│   主数据清理   │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 5.2 关键算法

#### 5.2.1 数据选择算法
```java
public class ArchiveSelectionAlgorithm {
    
    /**
     * 基于时间和状态的归档选择
     */
    public List<BatchNumber> selectDataForArchive(LocalDate cutoffDate) {
        return batchNumberRepository.findByConditions(
            condition1: status IN ('EXPIRED', 'CANCELLED'),
            condition2: updatedAt < cutoffDate,
            condition3: lastAccessedAt < cutoffDate.minusYears(1)
        );
    }
    
    /**
     * 基于访问频率的归档选择
     */
    public List<BatchNumber> selectByAccessFrequency(int accessThreshold) {
        // 统计访问频率低于阈值的数据
    }
}
```

#### 5.2.2 压缩算法选择
- **JSON数据**：GZIP压缩（压缩比高）
- **文本数据**：LZ4压缩（速度快）
- **二进制数据**：ZSTD压缩（平衡压缩比与速度）
- **图片数据**：WebP压缩（适合质量要求高的场景）

### 5.3 性能优化

#### 5.3.1 批量处理优化
```java
@Slf4j
@Service
public class BatchArchiveProcessor {
    
    private static final int BATCH_SIZE = 1000;
    
    @Transactional
    public ArchiveResult processBatchArchive(ArchiveJob job) {
        List<BatchNumber> toArchive = selectData(job);
        
        // 分批处理，避免大事务
        for (int i = 0; i < toArchive.size(); i += BATCH_SIZE) {
            List<BatchNumber> batch = toArchive.subList(i, 
                Math.min(i + BATCH_SIZE, toArchive.size()));
            
            processSingleBatch(batch, job);
            
            // 提交事务，释放资源
            entityManager.flush();
            entityManager.clear();
        }
        
        return buildResult(job, toArchive.size());
    }
}
```

#### 5.3.2 异步处理机制
```java
@Component
public class ArchiveAsyncProcessor {
    
    @Async("archiveTaskExecutor")
    public CompletableFuture<ArchiveResult> processAsync(ArchiveRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ArchiveResult result = archiveService.processArchive(request);
            
            // 发送通知
            notificationService.sendArchiveCompleteNotification(
                request.getOperatorId(), 
                result
            );
            
            return result;
        });
    }
}
```

## 6. 监控与运维

### 6.1 监控指标

| 指标类别 | 监控指标 | 告警阈值 | 应对措施 |
|----------|----------|----------|----------|
| 归档性能 | 归档成功率 | < 95% | 检查归档配置 |
| 存储使用 | 归档存储增长率 | > 20%/月 | 优化归档策略 |
| 检索性能 | 归档数据检索延迟 | > 5秒 | 优化索引 |
| 成本控制 | 存储成本增长率 | > 15%/月 | 调整存储层级 |
| 数据完整性 | 归档验证失败率 | > 1% | 立即修复 |

### 6.2 运维流程

#### 6.2.1 日常巡检
- 检查归档任务执行状态
- 监控存储使用情况
- 验证数据完整性
- 检查备份状态

#### 6.2.2 故障处理
1. 归档失败：重试机制 + 人工干预
2. 存储异常：切换存储层级 + 数据修复
3. 检索失败：重建索引 + 缓存优化
4. 合规风险：立即报告 + 合规检查

## 7. 风险评估与应对

### 7.1 技术风险
- **数据丢失风险**：实施多副本存储 + 定期备份
- **性能影响风险**：归档期间限流 + 业务低峰期执行
- **检索延迟风险**：建立缓存层 + 优化查询算法

### 7.2 业务风险
- **合规风险**：定期合规检查 + 审计日志
- **业务中断风险**：灰度发布 + 回滚机制
- **成本超支风险**：成本监控 + 动态调整策略

### 7.3 运维风险
- **运维复杂性**：自动化运维工具 + 完善文档
- **技能要求**：培训计划 + 知识库建设
- **供应商依赖**：多供应商策略 + 技术解耦

## 8. 效益分析

### 8.1 技术效益
- **主数据库性能提升**：预计提升30-50%
- **存储成本优化**：预计降低40-60%
- **查询性能改善**：归档后查询速度提升2-3倍
- **系统稳定性增强**：减少大表对系统的影响

### 8.2 业务效益
- **合规性保障**：满足法规要求的记录保存期限
- **数据安全**：多层级备份确保数据安全
- **查询效率**：历史数据快速检索
- **成本控制**：优化存储成本结构

### 8.3 运维效益
- **运维效率**：自动化归档减少人工干预
- **监控能力**：全面的监控体系
- **故障恢复**：快速的数据恢复能力
- **可扩展性**：支持业务规模扩展

## 9. 后续优化方向

### 9.1 短期优化（3-6个月）
- AI驱动的智能归档策略
- 更高效的数据压缩算法
- 实时归档监控大屏

### 9.2 中期优化（6-12个月）
- 区块链存证技术集成
- 边缘计算支持
- 多云存储策略

### 9.3 长期规划（1-3年）
- 全自动智能存储管理
- 预测性成本优化
- 跨平台数据交换标准

---

## 附录

### A. 归档配置示例
```yaml
archive:
  policies:
    batch_number:
      criteria:
        - field: status
          operator: IN
          value: ["EXPIRED", "CANCELLED"]
        - field: updated_at
          operator: <
          value: "P3Y"  # 3年前
      schedule: "0 2 * * 0"  # 每周日2点执行
      batch_size: 1000
      compression: GZIP
      storage_tier: TIER2
      
    audit_log:
      criteria:
        - field: created_at
          operator: <
          value: "P1Y"  # 1年前
      schedule: "0 3 1 * *"  # 每月1日3点执行
      retention_period: "P10Y"  # 保留10年
      storage_tier: TIER4
```

### B. 监控指标配置
```yaml
monitoring:
  metrics:
    - name: archive_success_rate
      type: gauge
      threshold: 95
      alert_level: warning
      
    - name: archive_storage_growth
      type: counter  
      threshold: 20
      alert_level: critical
      
    - name: archive_retrieval_latency
      type: histogram
      threshold: 5000  # 5秒
      alert_level: warning
```

### C. 相关文档链接
1. [批次管理模块业务逻辑文档](./erp-batch-sn/docs/BUSINESS_LOGIC.md)
2. [数据库设计文档](./erp-batch-sn/sql/batch_sn_schema.sql)
3. [归档API接口文档](./erp-batch-sn/docs/API_DOCUMENTATION.md#归档接口)

---

**文档版本**: v1.0  
**创建日期**: 2026-05-01  
**更新日期**: 2026-05-01  
**负责人**: team-member  
**审核状态**: ✅ 设计完成