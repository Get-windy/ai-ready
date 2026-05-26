# 批次/序列号管理技术方案设计文档

## 1. 系统架构设计

### 1.1 微服务架构设计
- **独立微服务**：`erp-batch-sn` 作为独立的批次/序列号管理微服务
- **服务边界**：专注于批次和序列号的全生命周期管理
- **接口协议**：RESTful API + 消息队列事件通知
- **服务发现**：通过Nacos进行服务注册与发现
- **负载均衡**：Spring Cloud LoadBalancer实现客户端负载均衡

### 1.2 数据存储架构
- **主数据库**：MySQL 8.0，专用Schema `erp_batch_sn`
- **读写分离**：主库写入，从库读取（针对查询密集型操作）
- **分库分表**：按业务线或时间维度进行数据分片
- **备份策略**：每日全量备份 + Binlog增量备份
- **灾备方案**：异地多活部署，RPO < 5分钟，RTO < 30分钟

### 1.3 缓存策略设计
- **Redis缓存**：
  - 批次基本信息缓存（TTL: 30分钟）
  - 序列号状态缓存（TTL: 15分钟）
  - 热点查询结果缓存（如批次追溯路径）
- **本地缓存**：Caffeine用于高频访问的配置信息
- **缓存一致性**：通过消息队列保证缓存与数据库的一致性
- **缓存穿透防护**：布隆过滤器防止无效ID查询

### 1.4 消息队列设计
- **消息中间件**：RocketMQ
- **核心Topic**：
  - `BATCH_SN_STATUS_CHANGE`：批次/序列号状态变更事件
  - `BATCH_SN_TRACE_UPDATE`：追溯关系更新事件
  - `BATCH_SN_EXPIRY_WARNING`：临期预警事件
- **消费组**：
  - 库存服务消费状态变更事件
  - 通知服务消费预警事件
  - 审计服务消费所有变更事件

## 2. 数据模型设计

### 2.1 批次信息数据模型

#### 核心表结构
```sql
-- 批次主表
CREATE TABLE batch_number (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_code VARCHAR(64) NOT NULL UNIQUE COMMENT '批次编码',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_sku VARCHAR(64) NOT NULL COMMENT '产品SKU',
    quantity DECIMAL(18,6) NOT NULL COMMENT '批次数量',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    manufacture_date DATE NOT NULL COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '批次状态',
    created_by BIGINT NOT NULL COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_batch (product_id, batch_code),
    INDEX idx_status_expiry (status, expiry_date)
);

-- 批次流转记录表
CREATE TABLE batch_flow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    from_location VARCHAR(100) COMMENT '来源位置',
    to_location VARCHAR(100) NOT NULL COMMENT '目标位置',
    quantity DECIMAL(18,6) NOT NULL COMMENT '流转数量',
    flow_type VARCHAR(20) NOT NULL COMMENT '流转类型',
    related_document_id VARCHAR(64) COMMENT '关联单据ID',
    operator_id BIGINT NOT NULL COMMENT '操作人',
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_flow (batch_id, flow_type),
    INDEX idx_document (related_document_id)
);
```

### 2.2 序列号数据模型

#### 核心表结构
```sql
-- 序列号主表
CREATE TABLE serial_number (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    serial_code VARCHAR(128) NOT NULL UNIQUE COMMENT '序列号',
    batch_id BIGINT NOT NULL COMMENT '所属批次ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '序列号状态',
    quality_status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '质量状态',
    manufacture_date DATE NOT NULL COMMENT '生产日期',
    warranty_start_date DATE COMMENT '保修开始日期',
    warranty_end_date DATE COMMENT '保修结束日期',
    current_location VARCHAR(100) COMMENT '当前位置',
    owner_id BIGINT COMMENT '当前所有者ID',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch_serial (batch_id, serial_code),
    INDEX idx_status_owner (status, owner_id),
    INDEX idx_product_warranty (product_id, warranty_end_date)
);

-- 序列号流转记录表
CREATE TABLE serial_flow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    serial_id BIGINT NOT NULL COMMENT '序列号ID',
    from_location VARCHAR(100) COMMENT '来源位置',
    to_location VARCHAR(100) NOT NULL COMMENT '目标位置',
    from_owner_id BIGINT COMMENT '来源所有者',
    to_owner_id BIGINT NOT NULL COMMENT '目标所有者',
    flow_type VARCHAR(20) NOT NULL COMMENT '流转类型',
    related_document_id VARCHAR(64) COMMENT '关联单据ID',
    operator_id BIGINT NOT NULL,
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_serial_flow (serial_id, flow_type),
    INDEX idx_owner_flow (from_owner_id, to_owner_id)
);
```

### 2.3 追溯关系数据模型

#### 核心表结构
```sql
-- 批次追溯关系表
CREATE TABLE batch_trace_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_batch_id BIGINT NOT NULL COMMENT '源批次ID',
    target_batch_id BIGINT NOT NULL COMMENT '目标批次ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型',
    quantity_ratio DECIMAL(10,6) COMMENT '数量比例',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source_target (source_batch_id, target_batch_id),
    INDEX idx_relation_type (relation_type)
);

-- 序列号追溯关系表
CREATE TABLE serial_trace_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_serial_id BIGINT NOT NULL COMMENT '源序列号ID',
    target_serial_id BIGINT NOT NULL COMMENT '目标序列号ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source_target (source_serial_id, target_serial_id)
);
```

### 2.4 库存关联数据模型

#### 库存关联设计
- **库存批次关联表**：`inventory_batch_association`
- **库存序列号关联表**：`inventory_serial_association`
- **实时同步机制**：通过消息队列保证库存与批次/序列号的一致性

## 3. 接口设计

### 3.1 批次管理API设计

#### 核心接口列表
- `POST /api/erp/batch-sn/batches` - 创建批次
- `GET /api/erp/batch-sn/batches/{batchCode}` - 获取批次详情
- `PUT /api/erp/batch-sn/batches/{batchCode}/status` - 更新批次状态
- `GET /api/erp/batch-sn/batches/{batchCode}/trace` - 批次追溯查询
- `POST /api/erp/batch-sn/batches/{batchCode}/flow` - 批次流转操作

#### 请求/响应示例
```json
// 创建批次请求
{
  "productSku": "PROD-001",
  "quantity": 1000,
  "unit": "PCS",
  "manufactureDate": "2026-04-29",
  "expiryDate": "2027-04-29",
  "customFields": {
    "productionLine": "LINE-A",
    "qualityInspector": "INSPECTOR-001"
  }
}

// 批次详情响应
{
  "batchCode": "BATCH-20260429-001",
  "productSku": "PROD-001",
  "quantity": 1000,
  "availableQuantity": 850,
  "status": "ACTIVE",
  "manufactureDate": "2026-04-29",
  "expiryDate": "2027-04-29",
  "currentLocations": [
    {"location": "WAREHOUSE-A", "quantity": 500},
    {"location": "WAREHOUSE-B", "quantity": 350}
  ]
}
```

### 3.2 序列号管理API设计

#### 核心接口列表
- `POST /api/erp/batch-sn/serials/generate` - 批量生成序列号
- `GET /api/erp/batch-sn/serials/{serialCode}` - 获取序列号详情
- `PUT /api/erp/batch-sn/serials/{serialCode}/status` - 更新序列号状态
- `GET /api/erp/batch-sn/serials/{serialCode}/trace` - 序列号追溯查询
- `POST /api/erp/batch-sn/serials/transfer` - 序列号转移操作

#### 请求/响应示例
```json
// 生成序列号请求
{
  "batchCode": "BATCH-20260429-001",
  "count": 100,
  "prefix": "SN-PROD001-",
  "warrantyMonths": 24
}

// 序列号详情响应
{
  "serialCode": "SN-PROD001-00001",
  "batchCode": "BATCH-20260429-001",
  "productSku": "PROD-001",
  "status": "IN_USE",
  "qualityStatus": "NORMAL",
  "currentLocation": "CUSTOMER-001",
  "ownerId": 1001,
  "warrantyEndDate": "2028-04-29",
  "flowHistory": [
    {"operation": "MANUFACTURE", "location": "FACTORY-A", "timestamp": "2026-04-29T10:00:00Z"},
    {"operation": "SHIPMENT", "location": "CUSTOMER-001", "timestamp": "2026-04-30T14:30:00Z"}
  ]
}
```

### 3.3 批量操作API设计
- `POST /api/erp/batch-sn/batches/bulk-status` - 批量更新批次状态
- `POST /api/erp/batch-sn/serials/bulk-transfer` - 批量转移序列号
- `GET /api/erp/batch-sn/batches/export` - 批次数据导出
- `POST /api/erp/batch-sn/import` - 批次/序列号数据导入

## 4. 性能优化方案

### 4.1 数据库优化
- **索引优化**：为高频查询字段建立复合索引
- **分区表**：按时间或业务线对大表进行分区
- **读写分离**：查询操作路由到从库
- **连接池**：HikariCP连接池配置优化

### 4.2 缓存优化
- **多级缓存**：本地缓存 + Redis分布式缓存
- **缓存预热**：系统启动时预加载热点数据
- **缓存更新策略**：写时删除 + 异步重建
- **缓存监控**：命中率、延迟等指标监控

### 4.3 查询优化
- **分页查询**：大数据量查询强制分页
- **异步导出**：大数据导出使用异步任务
- **聚合查询**：复杂统计使用物化视图
- **查询限流**：防止恶意查询导致系统雪崩

### 4.4 批量处理优化
- **批量插入**：使用JDBC批处理提高插入性能
- **并行处理**：大批次数据分片并行处理
- **内存控制**：防止OOM的内存使用控制
- **进度反馈**：长时间任务的进度反馈机制

## 5. 安全性和可靠性设计

### 5.1 数据安全
- **数据加密**：敏感字段AES加密存储
- **访问控制**：基于RBAC的细粒度权限控制
- **审计日志**：所有关键操作的完整审计日志
- **数据脱敏**：API响应中的敏感数据脱敏

### 5.2 系统可靠性
- **幂等设计**：所有写操作保证幂等性
- **事务管理**：分布式事务保证数据一致性
- **熔断降级**：依赖服务故障时的降级策略
- **重试机制**：网络异常时的智能重试

### 5.3 监控告警
- **健康检查**：服务健康状态实时监控
- **性能指标**：QPS、响应时间、错误率等
- **业务指标**：批次创建成功率、追溯查询成功率等
- **告警规则**：异常情况的自动告警通知

## 6. 部署和运维方案

### 6.1 部署架构
- **容器化部署**：Docker + Kubernetes
- **配置中心**：Nacos配置管理
- **服务网格**：Istio流量管理和安全控制
- **CI/CD**：GitLab CI自动化部署流水线

### 6.2 运维监控
- **日志收集**：ELK日志分析平台
- **链路追踪**：SkyWalking分布式追踪
- **指标监控**：Prometheus + Grafana
- **告警通知**：企业微信/邮件告警集成

### 6.3 容量规划
- **初始容量**：支持日均10万批次、100万序列号操作
- **扩展能力**：水平扩展支持千万级操作
- **资源配比**：CPU/Memory/Storage合理配比
- **成本控制**：云资源的弹性伸缩和成本优化