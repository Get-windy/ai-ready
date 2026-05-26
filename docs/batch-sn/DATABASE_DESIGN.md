# 批次/序列号管理系统数据库设计文档

## 1. 概述

### 1.1 文档信息
- **文档名称**: 批次/序列号管理系统数据库设计
- **创建日期**: 2026-04-27
- **版本**: v1.0
- **项目**: AI-Ready ERP系统
- **模块**:批次/序列号管理系统 (erp-batch-sn)

### 1.2 设计目标
- 实现产品批次号和序列号的全流程追溯
- 支持医药/食品行业的合规要求
- 提供强大的追溯查询和质量分析功能

### 1.3 技术栈
- 数据库: PostgreSQL 14
- ORM: MyBatis Plus
- 包命名: `cn.aiedge.erp.batchsn`

## 2. 核心实体设计

### 2.1 批次号表 (batch_number)

```sql
CREATE TABLE batch_number (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    batch_no VARCHAR(64) NOT NULL UNIQUE COMMENT '批次号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(255) NOT NULL COMMENT '产品名称',
    specification VARCHAR(255) COMMENT '产品规格',
    unit VARCHAR(20) COMMENT '单位',
    
    -- 生产信息
    production_date DATE NOT NULL COMMENT '生产日期',
    expiration_date DATE COMMENT '有效期至',
    batch_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '批次状态: ACTIVE/EXPIRED/QUARANTINED/CANCELLED',
    
    -- 数量信息
    total_quantity DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总数量',
    available_quantity DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '可用数量',
    reserved_quantity DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '预留数量',
    
    -- 来源信息
    source_type VARCHAR(20) COMMENT '来源类型: PURCHASE/PRODUCTION/SALE_RETURN',
    source_ref_id BIGINT COMMENT '来源单据ID',
    source_ref_no VARCHAR(64) COMMENT '来源单据号',
    
    -- 库存信息
    warehouse_id BIGINT COMMENT '仓库ID',
    warehouse_name VARCHAR(100) COMMENT '仓库名称',
    location_id BIGINT COMMENT '库位ID',
    
    -- 质量信息
    quality_status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '质量状态: NORMAL/QUARANTINED/DEFECTIVE',
    quality_inspector_id VARCHAR(64) COMMENT '质检员ID',
    quality_inspector_name VARCHAR(50) COMMENT '质检员姓名',
    quality_inspection_date TIMESTAMP COMMENT '质检日期',
    
    -- 批次规则
    batch_rule_id BIGINT COMMENT '批次规则ID',
    batch_rule_name VARCHAR(100) COMMENT '批次规则名称',
    
    -- 系统字段
    created_by VARCHAR(64) COMMENT '创建人ID',
    created_by_name VARCHAR(50) COMMENT '创建人姓名',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) COMMENT '更新人ID',
    updated_by_name VARCHAR(50) COMMENT '更新人姓名',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_batch_no (batch_no),
    INDEX idx_product_id (product_id),
    INDEX idx_product_code (product_code),
    INDEX idx_production_date (production_date),
    INDEX idx_expiration_date (expiration_date),
    INDEX idx_batch_status (batch_status),
    INDEX idx_source_ref (source_type, source_ref_id),
    INDEX idx_warehouse_location (warehouse_id, location_id),
    INDEX idx_quality_status (quality_status),
    INDEX idx_created_at (created_at)
) COMMENT='批次号主表';

COMMENT ON COLUMN batch_number.batch_no IS '批次号，唯一标识一个批次';
COMMENT ON COLUMN batch_number.product_id IS '关联产品ID';
COMMENT ON COLUMN batch_number.total_quantity IS '批次总数量';
COMMENT ON COLUMN batch_number.available_quantity IS '当前可用数量（可用于出库）';
COMMENT ON COLUMN batch_number.reserved_quantity IS '已预留数量（销售预留/生产占用）';
COMMENT ON COLUMN batch_number.source_type IS '来源类型：PURCHASE采购入库, PRODUCTION生产入库, SALE_RETURN销售退货';
COMMENT ON COLUMN batch_number.quality_status IS '质量状态：NORMAL正常, QUARANTAINED待检, DEFECTIVE不合格';
```

### 2.2 序列号表 (serial_number)

```sql
CREATE TABLE serial_number (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    serial_no VARCHAR(128) NOT NULL UNIQUE COMMENT '序列号（全球唯一）',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(255) NOT NULL COMMENT '产品名称',
    specification VARCHAR(255) COMMENT '产品规格',
    batch_id BIGINT COMMENT '批次ID',
    batch_no VARCHAR(64) COMMENT '批次号',
    
    -- 状态信息
    sn_status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '序列号状态: AVAILABLE/IN_USE/INSERVICE/MAINTAINED/SCRAP',
    sn_STAGE VARCHAR(20) DEFAULT 'WAREHOUSE' COMMENT '所处阶段: WAREHOUSE/IN_TRANSIT/EOF_CUSTOMER/IN_SERVICE/SCRAPPED',
    
    -- 生产信息
    manufacturer VARCHAR(255) COMMENT '制造商',
    manufacturing_date DATE COMMENT '生产日期',
    warranty_period INTEGER COMMENT '质保期(月)',
    warranty_start_date DATE COMMENT '质保开始日期',
    warranty_end_date DATE COMMENT '质保结束日期',
    
    -- 位置信息
    current_location VARCHAR(255) COMMENT '当前位置',
    location_id BIGINT COMMENT '库位ID',
    warehouse_id BIGINT COMMENT '仓库ID',
    
    -- 关联信息
    purchase_order_id BIGINT COMMENT '采购订单ID',
    purchase_order_no VARCHAR(64) COMMENT '采购订单号',
    sale_order_id BIGINT COMMENT '销售订单ID',
    sale_order_no VARCHAR(64) COMMENT '销售订单号',
    
    -- 质量信息
    quality_status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '质量状态: NORMAL/DEFECTIVE/UNDER_REPAIR',
    last_inspection_date TIMESTAMP COMMENT '最后质检日期',
    next_inspection_date TIMESTAMP COMMENT '下次质检日期',
    
    -- 维护记录
    maintenance_count INTEGER DEFAULT 0 COMMENT '维修次数',
    last_maintenance_date TIMESTAMP COMMENT '最后维修日期',
    
    -- 系统字段
    created_by VARCHAR(64) COMMENT '创建人ID',
    created_by_name VARCHAR(50) COMMENT '创建人姓名',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) COMMENT '更新人ID',
    updated_by_name VARCHAR(50) COMMENT '更新人姓名',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP COMMENT '删除时间（软删除）',
    
    -- 索引
    INDEX idx_serial_no (serial_no),
    INDEX idx_product_id (product_id),
    INDEX idx_product_code (product_code),
    INDEX idx_batch_id (batch_id),
    INDEX idx_sn_status (sn_status),
    INDEX idx_sn_stage (sn_STAGE),
    INDEX idx_sale_order_id (sale_order_id),
    INDEX idx_warranty_end_date (warranty_end_date),
    INDEX idx_next_inspection_date (next_inspection_date),
    UNIQUE INDEX uk_serial_no (serial_no)
) COMMENT='序列号主表';

COMMENT ON COLUMN serial_number.serial_no IS '序列号，全球唯一标识一个单品';
COMMENT ON COLUMN serial_number.sn_status IS '序列号状态：AVAILABLE可用, IN_USE使用中, INSERVICE服务中, MAINTAINED维修中, SCRAP报废';
COMMENT ON COLUMN serial_number.sn_STAGE IS '所处阶段：WAREHOUSE仓库, IN_TRANSIT在途, EOF_CUSTOMER客户, IN_SERVICE服务中, SCRAPPED报废';
```

### 2.3 批次流转记录表 (batch_flow_record)

```sql
CREATE TABLE batch_flow_record (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(255) NOT NULL COMMENT '产品名称',
    
    -- 流转信息
    flow_type VARCHAR(20) NOT NULL COMMENT '流转类型: INBOUND/OUTBOUND/TRANSFER/ADJUST',
    flow_no VARCHAR(64) COMMENT '流转单号',
    flow_id BIGINT COMMENT '流转单ID',
    
    -- 数量变化
    quantity_change DECIMAL(18,4) NOT NULL COMMENT '数量变化量（正数入库/减少预留，负数出库/增加预留）',
    before_quantity DECIMAL(18,4) NOT NULL COMMENT '变化前数量',
    after_quantity DECIMAL(18,4) NOT NULL COMMENT '变化后数量',
    
    -- 位置信息
    from_warehouse_id BIGINT COMMENT '源仓库ID',
    from_warehouse_name VARCHAR(100) COMMENT '源仓库名称',
    to_warehouse_id BIGINT COMMENT '目标仓库ID',
    to_warehouse_name VARCHAR(100) COMMENT '目标仓库名称',
    from_location_id BIGINT COMMENT '源库位ID',
    to_location_id BIGINT COMMENT '目标库位ID',
    
    -- 操作信息
    operator_id VARCHAR(64) COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    operator_ip VARCHAR(45) COMMENT '操作IP',
    
    -- 备注
    remark TEXT COMMENT '备注说明',
    
    -- 系统字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_batch_id (batch_id),
    INDEX idx_batch_no (batch_no),
    INDEX idx_flow_type (flow_type),
    INDEX idx_flow_no (flow_no),
    INDEX idx_created_at (created_at),
    INDEX idx_operator_id (operator_id)
) COMMENT='批次流转记录表';

COMMENT ON COLUMN batch_flow_record.flow_type IS '流转类型：INBOUND入库, OUTBOUND出库, TRANSFER调拨, ADJUST库存调整';
COMMENT ON COLUMN batch_flow_record.quantity_change IS '数量变化：正数=入库/减少预留，负数=出库/增加预留';
```

### 2.4 序列号流转记录表 (serial_flow_record)

```sql
CREATE TABLE serial_flow_record (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    serial_id BIGINT NOT NULL COMMENT '序列号ID',
    serial_no VARCHAR(128) NOT NULL COMMENT '序列号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(255) NOT NULL COMMENT '产品名称',
    
    -- 流转信息
    flow_type VARCHAR(20) NOT NULL COMMENT '流转类型: INBOUND/OUTBOUND/TRANSFER/MAINTAIN/SCRAP',
    flow_no VARCHAR(64) COMMENT '流转单号',
    flow_id BIGINT COMMENT '流转单ID',
    
    -- 状态变化
    from_status VARCHAR(20) NOT NULL COMMENT '变更前状态',
    to_status VARCHAR(20) NOT NULL COMMENT '变更后状态',
    from_STAGE VARCHAR(20) NOT NULL COMMENT '变更前阶段',
    to_STAGE VARCHAR(20) NOT NULL COMMENT '变更后阶段',
    
    -- 位置信息
    from_location VARCHAR(255) COMMENT '变更前位置',
    to_location VARCHAR(255) COMMENT '变更后位置',
    
    -- 操作信息
    operator_id VARCHAR(64) COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    
    -- 备注
    remark TEXT COMMENT '备注说明',
    
    -- 系统字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_serial_id (serial_id),
    INDEX idx_serial_no (serial_no),
    INDEX idx_flow_type (flow_type),
    INDEX idx_flow_no (flow_no),
    INDEX idx_created_at (created_at)
) COMMENT='序列号流转记录表';
```

### 2.5 批次规则配置表 (batch_rule)

```sql
CREATE TABLE batch_rule (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    rule_name VARCHAR(100) NOT NULL UNIQUE COMMENT '规则名称',
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    
    -- 编号规则
    prefix VARCHAR(20) DEFAULT 'B' COMMENT '批次号前缀',
    date_format VARCHAR(20) DEFAULT 'yyyyMMdd' COMMENT '日期格式',
    seq_length INTEGER DEFAULT 4 COMMENT '序号长度',
    seq_start INTEGER DEFAULT 1 COMMENT '序号起始值',
    
    -- 有效期规则
    default_expiry_days INTEGER COMMENT '默认有效期(天)',
    auto_expiry BOOLEAN DEFAULT false COMMENT '自动过期',
    expiry_warning_days INTEGER DEFAULT 30 COMMENT '临期提醒天数',
    
    -- 质检规则
    require_quality_check BOOLEAN DEFAULT true COMMENT '需要质检',
    quality_check_interval_days INTEGER DEFAULT 180 COMMENT '质检周期(天)',
    
    -- 状态规则
    status_flow TEXT COMMENT '状态流转JSON配置',
    
    -- 适用范围
    product_category_ids TEXT COMMENT '适用产品分类ID列表',
    enable BOOLEAN DEFAULT true COMMENT '是否启用',
    
    -- 系统字段
    created_by VARCHAR(64) COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) COMMENT '更新人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_rule_code (rule_code),
    INDEX idx_enable (enable)
) COMMENT='批次规则配置表';
```

### 2.6 追溯查询日志表 (traceability_log)

```sql
CREATE TABLE traceability_log (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    trace_type VARCHAR(20) NOT NULL COMMENT '追溯类型: BATCH/SERIAL/PRODUCT',
    trace_id BIGINT NOT NULL COMMENT '追溯对象ID',
    trace_code VARCHAR(128) NOT NULL COMMENT '追溯对象编码',
    trace_name VARCHAR(255) COMMENT '追溯对象名称',
    
    -- 查询信息
    query_type VARCHAR(20) NOT NULL COMMENT '查询类型: SIMPLE/DETAILED/FULL_TRACE',
    query_time_range_start TIMESTAMP COMMENT '查询时间范围起始',
    query_time_range_end TIMESTAMP COMMENT '查询时间范围结束',
    
    -- 结果统计
    result_count INTEGER DEFAULT 0 COMMENT '结果数量',
    query_duration_ms INTEGER COMMENT '查询耗时(毫秒)',
    
    -- 用户信息
    query_user_id VARCHAR(64) COMMENT '查询用户ID',
    query_user_name VARCHAR(50) COMMENT '查询用户姓名',
    query_ip VARCHAR(45) COMMENT '查询IP',
    
    -- 系统字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_trace_type (trace_type),
    INDEX idx_trace_id (trace_id),
    INDEX idx_query_user_id (query_user_id),
    INDEX idx_created_at (created_at)
) COMMENT='追溯查询日志表';
```

## 3. 数据字典

### 3.1 批次状态 (batch_status)
- `ACTIVE` - 活跃中
- `EXPIRED` - 已过期
- `QUARANTINED` - 隔离中
- `CANCELLED` - 已取消

### 3.2 序列号状态 (sn_status)
- `AVAILABLE` - 可用
- `IN_USE` - 使用中
- `INSERVICE` - 服务中
- `MAINTAINED` - 维修中
- `SCRAP` - 报废

### 3.3 流转类型 (flow_type)
- `INBOUND` - 入库
- `OUTBOUND` - 出库
- `TRANSFER` - 调拨
- `ADJUST` - 库存调整
- `MAINTAIN` - 维修
- `SCRAP` - 报废

### 3.4 质量状态 (quality_status)
- `NORMAL` - 正常
- `QUARANTINED` - 待检
- `DEFECTIVE` - 不合格
- `UNDER_REPAIR` - 维修中

## 4. 建表脚本

### 4.1 主要建表SQL

```sql
-- 批次号主表
CREATE TABLE batch_number (
    id BIGSERIAL PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    specification VARCHAR(255),
    unit VARCHAR(20),
    production_date DATE NOT NULL,
    expiration_date DATE,
    batch_status VARCHAR(20) DEFAULT 'ACTIVE',
    total_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    source_type VARCHAR(20),
    source_ref_id BIGINT,
    source_ref_no VARCHAR(64),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(100),
    location_id BIGINT,
    quality_status VARCHAR(20) DEFAULT 'NORMAL',
    quality_inspector_id VARCHAR(64),
    quality_inspector_name VARCHAR(50),
    quality_inspection_date TIMESTAMP,
    batch_rule_id BIGINT,
    batch_rule_name VARCHAR(100),
    created_by VARCHAR(64),
    created_by_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    updated_by_name VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_batch_no (batch_no),
    INDEX idx_product_id (product_id),
    INDEX idx_product_code (product_code),
    INDEX idx_production_date (production_date),
    INDEX idx_expiration_date (expiration_date),
    INDEX idx_batch_status (batch_status),
    INDEX idx_quality_status (quality_status)
);

COMMENT ON TABLE batch_number IS '批次号主表';
COMMENT ON COLUMN batch_number.batch_no IS '批次号，唯一标识一个批次';
COMMENT ON COLUMN batch_number.available_quantity IS '当前可用数量（可用于出库）';

-- 序列号主表
CREATE TABLE serial_number (
    id BIGSERIAL PRIMARY KEY,
    serial_no VARCHAR(128) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    specification VARCHAR(255),
    batch_id BIGINT,
    batch_no VARCHAR(64),
    sn_status VARCHAR(20) DEFAULT 'AVAILABLE',
    sn_stage VARCHAR(20) DEFAULT 'WAREHOUSE',
    manufacturer VARCHAR(255),
    manufacturing_date DATE,
    warranty_period INTEGER,
    warranty_start_date DATE,
    warranty_end_date DATE,
    current_location VARCHAR(255),
    location_id BIGINT,
    warehouse_id BIGINT,
    purchase_order_id BIGINT,
    purchase_order_no VARCHAR(64),
    sale_order_id BIGINT,
    sale_order_no VARCHAR(64),
    quality_status VARCHAR(20) DEFAULT 'NORMAL',
    last_inspection_date TIMESTAMP,
    next_inspection_date TIMESTAMP,
    maintenance_count INTEGER DEFAULT 0,
    last_maintenance_date TIMESTAMP,
    created_by VARCHAR(64),
    created_by_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    updated_by_name VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    INDEX idx_serial_no (serial_no),
    INDEX idx_product_id (product_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_sn_status (sn_status),
    INDEX idx_sn_stage (sn_stage),
    INDEX idx_sale_order_id (sale_order_id)
);

COMMENT ON TABLE serial_number IS '序列号主表';
COMMENT ON COLUMN serial_number.serial_no IS '序列号，全球唯一标识一个单品';

-- 批次流转记录表
CREATE TABLE batch_flow_record (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    flow_type VARCHAR(20) NOT NULL,
    flow_no VARCHAR(64),
    flow_id BIGINT,
    quantity_change DECIMAL(18,4) NOT NULL,
    before_quantity DECIMAL(18,4) NOT NULL,
    after_quantity DECIMAL(18,4) NOT NULL,
    from_warehouse_id BIGINT,
    from_warehouse_name VARCHAR(100),
    to_warehouse_id BIGINT,
    to_warehouse_name VARCHAR(100),
    from_location_id BIGINT,
    to_location_id BIGINT,
    operator_id VARCHAR(64),
    operator_name VARCHAR(50),
    operator_ip VARCHAR(45),
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_batch_id (batch_id),
    INDEX idx_flow_type (flow_type),
    INDEX idx_flow_no (flow_no)
);

COMMENT ON TABLE batch_flow_record IS '批次流转记录表';

-- 序列号流转记录表
CREATE TABLE serial_flow_record (
    id BIGSERIAL PRIMARY KEY,
    serial_id BIGINT NOT NULL,
    serial_no VARCHAR(128) NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    flow_type VARCHAR(20) NOT NULL,
    flow_no VARCHAR(64),
    flow_id BIGINT,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    from_stage VARCHAR(20) NOT NULL,
    to_stage VARCHAR(20) NOT NULL,
    from_location VARCHAR(255),
    to_location VARCHAR(255),
    operator_id VARCHAR(64),
    operator_name VARCHAR(50),
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_serial_id (serial_id),
    INDEX idx_flow_type (flow_type)
);

COMMENT ON TABLE serial_flow_record IS '序列号流转记录表';

-- 批次规则配置表
CREATE TABLE batch_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    prefix VARCHAR(20) DEFAULT 'B',
    date_format VARCHAR(20) DEFAULT 'yyyyMMdd',
    seq_length INTEGER DEFAULT 4,
    seq_start INTEGER DEFAULT 1,
    default_expiry_days INTEGER,
    auto_expiry BOOLEAN DEFAULT false,
    expiry_warning_days INTEGER DEFAULT 30,
    require_quality_check BOOLEAN DEFAULT true,
    quality_check_interval_days INTEGER DEFAULT 180,
    status_flow TEXT,
    product_category_ids TEXT,
    enable BOOLEAN DEFAULT true,
    created_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_rule_code (rule_code),
    INDEX idx_enable (enable)
);

COMMENT ON TABLE batch_rule IS '批次规则配置表';

-- 追溯查询日志表
CREATE TABLE traceability_log (
    id BIGSERIAL PRIMARY KEY,
    trace_type VARCHAR(20) NOT NULL,
    trace_id BIGINT NOT NULL,
    trace_code VARCHAR(128) NOT NULL,
    trace_name VARCHAR(255),
    query_type VARCHAR(20) NOT NULL,
    query_time_range_start TIMESTAMP,
    query_time_range_end TIMESTAMP,
    result_count INTEGER DEFAULT 0,
    query_duration_ms INTEGER,
    query_user_id VARCHAR(64),
    query_user_name VARCHAR(50),
    query_ip VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_trace_type (trace_type),
    INDEX idx_trace_id (trace_id),
    INDEX idx_query_user_id (query_user_id)
);

COMMENT ON TABLE traceability_log IS '追溯查询日志表';
```

### 4.2 初始化数据

```sql
-- 初始化默认批次规则
INSERT INTO batch_rule (rule_name, rule_code, prefix, date_format, seq_length, 
    seq_start, default_expiry_days, auto_expiry, expiry_warning_days, 
    require_quality_check, quality_check_interval_days, enable)
VALUES 
('默认批次规则', 'DEFAULT_BATCH', 'B', 'yyyyMMdd', 4, 1, 
 365, true, 30, true, 180, true);

-- 初始化序列号规则
INSERT INTO batch_rule (rule_name, rule_code, prefix, date_format, seq_length, 
    seq_start, require_quality_check, enable)
VALUES 
('默认序列号规则', 'DEFAULT_SERIAL', 'S', 'yyyyMMdd', 6, 1, 
 true, true);
```

## 5. 数据迁移策略

### 5.1 旧系统数据迁移

```sql
-- 批次数据迁移示例
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name, specification, unit,
    production_date, expiration_date, batch_status, total_quantity,
    available_quantity, reserved_quantity, source_type, source_ref_id,
    source_ref_no, warehouse_id, warehouse_name, quality_status,
    created_by, created_by_name, created_at
)
SELECT 
    batch_no, product_id, product_code, product_name, specification, unit,
    production_date, expiration_date, 
    CASE WHEN expired = true THEN 'EXPIRED' ELSE 'ACTIVE' END,
    total_qty, available_qty, reserved_qty,
    source_type, source_id, source_no,
    warehouse_id, warehouse_name, quality_status,
    created_by, created_by_name, created_at
FROM old_system.batch_data
WHERE migrated = false;

-- 序列号数据迁移示例
INSERT INTO serial_number (
    serial_no, product_id, product_code, product_name, specification,
    batch_id, batch_no, sn_status, sn_stage, manufacturer,
    manufacturing_date, warranty_period, current_location,
    purchase_order_id, quality_status, created_by, created_at
)
SELECT 
    serial_no, product_id, product_code, product_name, specification,
    batch_id, batch_no,
    CASE WHERE_STATUS 
        WHEN 'IN_STOCK' THEN 'AVAILABLE'
        WHEN 'IN_USE' THEN 'IN_USE'
        WHEN 'IN_SERVICE' THEN 'INSERVICE'
        WHEN 'SCRAP' THEN 'SCRAP'
    END,
    CASE WHERE_STATUS 
        WHEN 'IN_STOCK' THEN 'WAREHOUSE'
        WHEN 'IN_TRANSIT' THEN 'IN_TRANSIT'
        WHEN 'AT_CUSTOMER' THEN 'EOF_CUSTOMER'
        WHEN 'IN_SERVICE' THEN 'IN_SERVICE'
        WHEN 'SCRAP' THEN 'SCRAPPED'
    END,
    manufacturer, manufacturing_date, warranty_period,
    current_location, purchase_order_id, quality_status,
    created_by, created_at
FROM old_system.serial_data
WHERE migrated = false;
```

## 6. 权限设计

```sql
-- 批次/序列号管理权限
INSERT INTO sys_permission (code, name, description, category, created_at)
VALUES 
('batchsn:view', '批次/序列号查看', '查看批次/序列号信息', 'batchsn', now()),
('batchsn:create', '批次/序列号创建', '创建批次/序列号', 'batchsn', now()),
('batchsn:edit', '批次/序列号编辑', '编辑批次/序列号信息', 'batchsn', now()),
('batchsn:delete', '批次/序列号删除', '删除批次/序列号', 'batchsn', now()),
('batchsn:trace', '批次追溯查询', '查询产品全流程追溯信息', 'batchsn', now()),
('batchsn:reports', '批次报表生成', '生成批次相关报表', 'batchsn', now()),
('batchsn:config', '批次规则配置', '配置批次管理规则', 'batchsn', now());
```

## 7. 数据备份策略

```sql
-- 批次数据定期备份视图
CREATE VIEW batch_backup_view AS
SELECT 
    bn.id, bn.batch_no, bn.product_code, bn.product_name,
    bn.production_date, bn.expiration_date, bn.batch_status,
    bn.total_quantity, bn.available_quantity, bn.reserved_quantity,
    bn.source_type, bn.source_ref_no,
    bn.created_at, bn.updated_at,
    u.real_name AS updated_by_name
FROM batch_number bn
LEFT JOIN sys_user u ON bn.updated_by = u.user_id
WHERE bn.deleted_at IS NULL;

-- 序列号状态快照视图
CREATE VIEW serial_snapshot_view AS
SELECT 
    sn.id, sn.serial_no, sn.product_code, sn.product_name,
    sn.batch_no, sn.sn_status, sn.sn_stage,
    sn.manufacturer, sn.warranty_end_date,
    sn.last_inspection_date, sn.next_inspection_date,
    sn.created_at, sn.updated_at
FROM serial_number sn
WHERE sn.deleted_at IS NULL
ORDER BY sn.updated_at DESC;
```

## 8. 有效期管理策略

### 8.1 自动过期任务

```sql
-- 临期预警任务（每天执行）
-- 将在30天内过期的批次标记为临期
UPDATE batch_number 
SET batch_status = 'WARNING'
WHERE expiration_date <= CURRENT_DATE + INTERVAL '30 days'
  AND batch_status = 'ACTIVE';

-- 自动过期任务（每天凌晨2点执行）
UPDATE batch_number 
SET batch_status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
WHERE expiration_date < CURRENT_DATE
  AND batch_status = 'ACTIVE';
```

### 8.2 质检提醒任务

```sql
-- 质检到期提醒（每天执行）
SELECT 
    bn.batch_no, bn.product_name, bn.expiration_date,
    u.email AS inspector_email
FROM batch_number bn
JOIN batch_rule br ON bn.batch_rule_id = br.id
JOIN sys_user u ON br.quality_inspector_id = u.user_id
WHERE bn.quality_inspection_date + br.quality_check_interval_days * INTERVAL '1 day' <= CURRENT_DATE
  AND bn.batch_status = 'ACTIVE';
```

## 9. 性能优化建议

### 9.1 索引优化

```sql
-- 批次号查询高频组合索引
CREATE INDEX idx_batch_product_status ON batch_number(product_id, batch_status);

-- 序列号状态查询高频组合索引
CREATE INDEX idx_serial_product_status ON serial_number(product_id, sn_status);

-- 流转记录时间范围查询索引
CREATE INDEX idx_flow_time_range ON batch_flow_record(created_at, flow_type);
```

### 9.2 分区策略

```sql
-- 批次流转记录按月分区
CREATE TABLE batch_flow_record_2026_04 PARTITION OF batch_flow_record
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE batch_flow_record_2026_05 PARTITION OF batch_flow_record
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
```

### 9.3 缓存策略

```sql
-- 批次快照缓存表（定时刷新）
CREATE TABLE batch_snapshot_cache (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    product_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    total_quantity DECIMAL(18,4),
    available_quantity DECIMAL(18,4),
    reserved_quantity DECIMAL(18,4),
    quality_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_batch_id (batch_id),
    INDEX idx_snapshot_date (snapshot_date)
);

-- 序列号状态缓存表
CREATE TABLE serial_status_cache (
    id BIGSERIAL PRIMARY KEY,
    serial_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    serial_no VARCHAR(128) NOT NULL,
    product_id BIGINT NOT NULL,
    sn_status VARCHAR(20),
    sn_stage VARCHAR(20),
    current_location VARCHAR(255),
    warranty_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_serial_id (serial_id),
    INDEX idx_snapshot_date (snapshot_date)
);
```

## 10. 审计日志

```sql
-- 批次/序列号操作审计表
CREATE TABLE batchsn_audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id BIGINT NOT NULL,
    operation_type VARCHAR(20) NOT NULL, -- CREATE/UPDATE/DELETE
    old_data TEXT, -- JSON格式
    new_data TEXT, -- JSON格式
    operator_id VARCHAR(64),
    operator_name VARCHAR(50),
    operator_ip VARCHAR(45),
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_table_record (table_name, record_id),
    INDEX idx_operation_time (operation_time),
    INDEX idx_operator_id (operator_id)
);

COMMENT ON TABLE batchsn_audit_log IS '批次/序列号操作审计日志';
```

## 11. 数据清理策略

```sql
-- 定期清理已报废序列号记录（保留3年）
DELETE FROM serial_number 
WHERE sn_status = 'SCRAP' 
  AND deleted_at < CURRENT_DATE - INTERVAL '3 years';

-- 定期清理过期批次记录（保留10年）
DELETE FROM batch_number 
WHERE batch_status = 'EXPIRED' 
  AND expiration_date < CURRENT_DATE - INTERVAL '10 years';
```

## 12. 数据关联关系图

```
product
   │
   ├─ batch_number (1:N)
   │    ├─ batch_flow_record (1:N)
   │    └─ serial_number (1:N)
   │         └─ serial_flow_record (1:N)
   │
   └─ purchase_order (1:N) ← batch_number.source_ref_id
   └─ sale_order (1:N) ← serial_number.sale_order_id
```

## 13. 版本历史

| 版本 | 日期 | 作者 | 修改说明 |
|------|------|------|----------|
| v1.0 | 2026-04-27 | team-member | 初始版本，完成数据库表结构设计 |

## 14. 参考文档

- [AI-Ready项目数据库设计规范](docs/database designing_standards.md)
- [ERP模块开发规范](docs/erp/development guide.md)
- [批次/序列号管理业务需求](docs/batch-sn/business-requirements.md)
