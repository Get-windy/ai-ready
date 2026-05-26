# ERP批次管理系统数据库设计实施文档

## 文档信息
- **项目名称**: ERP批次/序列号管理系统
- **模块名称**: erp-batch-sn
- **版本**: v1.0
- **作者**: devops-engineer
- **创建日期**: 2026-05-05
- **最后更新**: 2026-05-05

## 一、数据库架构概述

### 1.1 设计目标
为ERP批次管理系统提供稳定、高效的数据存储和访问能力，支持批次管理、序列号追踪、流转记录、审计追溯等核心业务功能。

### 1.2 技术栈
- **数据库**: PostgreSQL 14+ / MySQL 8.0
- **ORM框架**: MyBatis Plus 3.5.5
- **Java版本**: Java 17
- **Spring Boot**: 3.2.0

### 1.3 核心设计原则
1. **数据完整性**: 通过约束、索引确保数据一致性
2. **性能优化**: 合理设计索引，支持高效查询
3. **可扩展性**: 支持业务增长和数据量扩展
4. **安全性**: 敏感数据加密，访问权限控制
5. **可追溯性**: 完整的审计日志和操作记录

## 二、数据库表设计

### 2.1 表结构总览

| 序号 | 表名 | 中文名 | 记录数预估 | 主要用途 |
|------|------|--------|------------|----------|
| 1 | batch_number | 批次号主表 | 10万-100万 | 批次基础信息 |
| 2 | serial_number | 序列号主表 | 100万-1000万 | 序列号追踪 |
| 3 | batch_flow_record | 批次流转记录 | 100万-500万 | 批次操作记录 |
| 4 | serial_flow_record | 序列号流转记录 | 500万-2000万 | 序列号操作记录 |
| 5 | batch_rule | 批次规则配置 | 100-1000 | 批次规则管理 |
| 6 | traceability_log | 追溯查询日志 | 10万-50万 | 追溯查询记录 |
| 7 | batchsn_audit_log | 审计日志 | 50万-200万 | 数据变更审计 |
| 8 | batch_snapshot_cache | 批次快照缓存 | 10万-100万 | 批次状态快照 |
| 9 | serial_status_cache | 序列号状态快照 | 100万-500万 | 序列号状态快照 |

### 2.2 核心表详细设计

#### 2.2.1 batch_number (批次号主表)
```sql
CREATE TABLE batch_number (
    id BIGSERIAL PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    -- 详细字段见DDL脚本
);
```

**关键索引**:
- `idx_batch_no` (batch_no) - 批次号查询
- `idx_product_id` (product_id) - 产品查询
- `idx_production_date` (production_date) - 生产日期查询
- `idx_expiration_date` (expiration_date) - 有效期查询
- `idx_batch_status` (batch_status) - 状态查询

#### 2.2.2 serial_number (序列号主表)
```sql
CREATE TABLE serial_number (
    id BIGSERIAL PRIMARY KEY,
    serial_no VARCHAR(128) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    batch_id BIGINT,
    sn_status VARCHAR(20) DEFAULT 'AVAILABLE',
    -- 详细字段见DDL脚本
);
```

**关键索引**:
- `idx_serial_no` (serial_no) - 序列号查询
- `idx_batch_id` (batch_id) - 批次关联查询
- `idx_sn_status` (sn_status) - 状态查询
- `idx_warranty_end_date` (warranty_end_date) - 质保期查询

### 2.3 数据关系设计

```
batch_rule (1) ──── (0..*) batch_number
batch_number (1) ──── (0..*) serial_number
batch_number (1) ──── (0..*) batch_flow_record
serial_number (1) ──── (0..*) serial_flow_record
```

## 三、实体类实现

### 3.1 实体类清单

| 实体类 | 对应表 | 状态 | 说明 |
|--------|--------|------|------|
| BatchNumber | batch_number | ✅ 已实现 | 批次号主表实体 |
| SerialNumber | serial_number | ✅ 已实现 | 序列号主表实体 |
| BatchFlowRecord | batch_flow_record | ✅ 已实现 | 批次流转记录实体 |
| SerialFlowRecord | serial_flow_record | ✅ 已实现 | 序列号流转记录实体 |
| BatchRule | batch_rule | ✅ 已实现 | 批次规则配置实体 |
| TraceabilityLog | traceability_log | ✅ 已实现 | 追溯查询日志实体 |
| BatchSnAuditLog | batchsn_audit_log | ✅ 已实现 | 审计日志实体 |
| BatchSnapshotCache | batch_snapshot_cache | ✅ 已实现 | 批次快照缓存实体 |
| SerialStatusCache | serial_status_cache | ✅ 已实现 | 序列号状态快照实体 |

### 3.2 实体类特点

1. **MyBatis Plus注解**: 使用`@TableName`, `@TableField`, `@TableId`等注解
2. **Lombok支持**: 使用`@Data`注解简化代码
3. **Java 8时间API**: 使用`LocalDateTime`, `LocalDate`等
4. **业务逻辑方法**: 每个实体类包含业务相关的方法
5. **枚举类型**: 定义状态、类型等枚举

### 3.3 示例实体类结构
```java
@Data
@TableName("batch_number")
public class BatchNumber {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("batch_no")
    private String batchNo;
    
    // 其他字段...
    
    // 业务方法
    public boolean isExpired() {
        return expirationDate != null && 
               expirationDate.isBefore(LocalDate.now());
    }
}
```

## 四、数据访问层实现

### 4.1 Mapper接口清单

| Mapper接口 | 对应实体 | 状态 | 说明 |
|------------|----------|------|------|
| BatchNumberMapper | BatchNumber | ✅ 已实现 | 批次数据访问 |
| SerialNumberMapper | SerialNumber | ✅ 已实现 | 序列号数据访问 |
| BatchFlowRecordMapper | BatchFlowRecord | ✅ 已实现 | 批次流转记录访问 |
| SerialFlowRecordMapper | SerialFlowRecord | ✅ 已实现 | 序列号流转记录访问 |
| BatchRuleMapper | BatchRule | ✅ 已实现 | 批次规则配置访问 |

### 4.2 Mapper接口特点

1. **继承BaseMapper**: 继承MyBatis Plus的`BaseMapper`接口
2. **自定义方法**: 包含业务相关的查询方法
3. **参数注解**: 使用`@Param`注解指定参数名
4. **分页支持**: 支持MyBatis Plus的分页功能

### 4.3 示例Mapper接口
```java
@Mapper
public interface BatchNumberMapper extends BaseMapper<BatchNumber> {
    BatchNumber selectByBatchNo(@Param("batchNo") String batchNo);
    List<BatchNumber> selectExpiringBatches(@Param("warningDays") int warningDays);
    int updateAvailableQuantity(@Param("batchId") Long batchId, 
                               @Param("quantityChange") BigDecimal quantityChange);
}
```

## 五、服务层实现

### 5.1 Service接口清单

| Service接口 | 实现类 | 状态 | 说明 |
|-------------|--------|------|------|
| BatchNumberService | BatchNumberServiceImpl | ✅ 已实现 | 批次管理服务 |
| SerialNumberService | SerialNumberServiceImpl | ✅ 已实现 | 序列号管理服务 |

### 5.2 服务层功能

#### BatchNumberService核心功能
- 批次创建、查询、更新、删除
- 批次入库、出库、转移操作
- 批次状态管理
- 批次库存管理
- 批次查询和统计

#### SerialNumberService核心功能
- 序列号创建、查询、更新
- 序列号状态管理
- 序列号流转追踪
- 序列号质保管理
- 序列号查询和统计

## 六、数据库初始化脚本

### 6.1 DDL脚本位置
```
src/main/resources/sql/
├── batch_sn_schema.sql      # 数据库表结构创建脚本
└── (其他初始化脚本)
```

### 6.2 脚本执行方式

#### PostgreSQL
```bash
psql -U postgres -d ai_ready -f batch_sn_schema.sql
```

#### MySQL
```bash
mysql -u root -p ai_ready < batch_sn_schema.sql
```

### 6.3 脚本内容概述
- 创建9个核心表
- 创建必要的索引
- 添加表注释和字段注释
- 设置合适的约束

## 七、性能优化策略

### 7.1 索引优化
1. **查询频繁字段**: 为`batch_no`, `serial_no`, `product_id`等字段创建索引
2. **复合索引**: 为常用查询组合创建复合索引
3. **覆盖索引**: 为只读查询创建覆盖索引
4. **定期维护**: 定期重建索引，优化统计信息

### 7.2 查询优化
1. **分页查询**: 所有列表查询支持分页
2. **查询缓存**: 使用Redis缓存热点数据
3. **批量操作**: 支持批量插入和更新
4. **异步处理**: 大数据量操作异步执行

### 7.3 分区策略
1. **时间分区**: `batch_flow_record`, `serial_flow_record`按时间分区
2. **范围分区**: `batch_number`按产品类别分区
3. **列表分区**: `serial_number`按状态分区

## 八、安全考虑

### 8.1 数据安全
1. **SQL注入防护**: 使用MyBatis Plus参数绑定
2. **敏感数据加密**: 用户信息、操作IP等敏感字段加密存储
3. **访问日志**: 所有数据访问记录审计日志
4. **权限控制**: 基于角色的数据访问控制

### 8.2 备份恢复
1. **定期备份**: 每日全量备份，每小时增量备份
2. **备份验证**: 定期验证备份数据完整性
3. **恢复演练**: 定期进行数据恢复演练
4. **异地备份**: 重要数据异地备份

## 九、监控与维护

### 9.1 监控指标
1. **性能指标**: 查询响应时间、连接数、锁等待
2. **容量指标**: 表大小、索引大小、磁盘使用率
3. **业务指标**: 批次数量、序列号数量、操作频率
4. **错误指标**: 连接错误、查询超时、死锁

### 9.2 维护计划
1. **日常维护**: 监控告警、日志分析、性能调优
2. **每周维护**: 索引重建、统计信息更新、备份验证
3. **每月维护**: 数据归档、分区维护、安全审计
4. **季度维护**: 性能评估、容量规划、架构优化

## 十、部署与验证

### 10.1 部署步骤
1. 执行数据库初始化脚本
2. 配置应用连接参数
3. 启动应用服务
4. 验证数据库连接
5. 执行功能测试

### 10.2 验证清单
- [ ] 数据库表创建成功
- [ ] 索引创建成功
- [ ] 实体类映射正确
- [ ] Mapper接口可用
- [ ] Service功能正常
- [ ] 性能满足要求（查询<50ms）
- [ ] 数据完整性验证通过

## 十一、性能基准测试

### 11.1 测试环境
- **数据库**: PostgreSQL 14 / 8核16GB
- **数据量**: 批次10万条，序列号100万条
- **并发**: 100并发用户

### 11.2 性能目标
| 操作类型 | 目标响应时间 | 实际测试结果 |
|----------|--------------|--------------|
| 单条批次查询 | < 10ms | 待测试 |
| 批次列表查询 | < 50ms | 待测试 |
| 批次创建 | < 100ms | 待测试 |
| 批次更新 | < 50ms | 待测试 |
| 复杂查询 | < 200ms | 待测试 |

## 十二、后续优化计划

### 12.1 短期优化（1-2周）
1. 完善剩余Mapper接口
2. 添加数据库连接池优化
3. 实现查询缓存机制
4. 完善监控告警配置

### 12.2 中期优化（1-2月）
1. 实施数据库分区
2. 优化查询执行计划
3. 实现读写分离
4. 添加数据归档策略

### 12.3 长期优化（3-6月）
1. 实施数据库集群
2. 实现异地多活
3. 优化数据迁移策略
4. 实施AI驱动的性能优化

## 十三、总结

### 13.1 已完成工作
1. ✅ 完整的数据库表设计（9个表）
2. ✅ 实体类实现（9个实体类）
3. ✅ 数据访问层实现（5个Mapper接口）
4. ✅ 服务层实现（2个Service）
5. ✅ 数据库初始化脚本
6. ✅ 性能优化设计
7. ✅ 安全策略设计

### 13.2 技术特点
1. **架构完整**: 从数据库到应用层的完整实现
2. **性能优化**: 充分考虑查询性能和扩展性
3. **安全可靠**: 完善的安全策略和备份机制
4. **易于维护**: 清晰的文档和监控体系

### 13.3 交付物清单
1. 数据库设计文档（本文档）
2. 数据库DDL脚本（batch_sn_schema.sql）
3. 实体类代码（9个Java文件）
4. Mapper接口代码（5个Java文件）
5. Service接口和实现（4个Java文件）

---

**文档版本历史**
| 版本 | 日期 | 作者 | 修改说明 |
|------|------|------|----------|
| v1.0 | 2026-05-05 | devops-engineer | 初始版本创建 |
