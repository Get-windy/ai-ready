-- 创建CRM-ERP客户映射表
-- 用于维护CRM客户/线索与ERP实体客户(Partner)之间的映射关系
-- Author: AI-Ready System
-- Description: 实现CRM-ERP客户关系管理功能

CREATE TABLE IF NOT EXISTS crm_erp_customer_mapping (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,

    -- CRM侧信息
    crm_system VARCHAR(50) NOT NULL,
    crm_entity_type VARCHAR(50) NOT NULL,
    crm_id BIGINT NOT NULL,

    -- ERP侧信息
    erp_system VARCHAR(50) NOT NULL,
    erp_entity_type VARCHAR(50) NOT NULL,
    erp_id BIGINT NOT NULL,

    -- 匹配相关信息
    match_rule VARCHAR(50),
    match_confidence DECIMAL(5,4),

    -- 同步状态
    sync_status VARCHAR(20) DEFAULT 'PENDING',
    sync_time TIMESTAMP WITH TIME ZONE,

    -- 扩展信息
    ext_info JSONB,

    -- 标准字段
    deleted INTEGER DEFAULT 0,
    create_by BIGINT,
    create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_crm_mapping_crm_info ON crm_erp_customer_mapping(crm_system, crm_entity_type, crm_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_mapping_erp_info ON crm_erp_customer_mapping(erp_system, erp_entity_type, erp_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_mapping_sync_status ON crm_erp_customer_mapping(sync_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_mapping_tenant ON crm_erp_customer_mapping(tenant_id) WHERE deleted = 0;

-- 添加表注释
COMMENT ON TABLE crm_erp_customer_mapping IS 'CRM-ERP客户映射表 - 维护CRM客户/线索与ERP实体客户之间的映射关系';
COMMENT ON COLUMN crm_erp_customer_mapping.id IS '主键ID';
COMMENT ON COLUMN crm_erp_customer_mapping.tenant_id IS '租户ID';
COMMENT ON COLUMN crm_erp_customer_mapping.crm_system IS 'CRM系统标识';
COMMENT ON COLUMN crm_erp_customer_mapping.crm_entity_type IS 'CRM实体类型';
COMMENT ON COLUMN crm_erp_customer_mapping.crm_id IS 'CRM系统中的实体ID';
COMMENT ON COLUMN crm_erp_customer_mapping.erp_system IS 'ERP系统标识';
COMMENT ON COLUMN crm_erp_customer_mapping.erp_entity_type IS 'ERP实体类型';
COMMENT ON COLUMN crm_erp_customer_mapping.erp_id IS 'ERP系统中的实体ID';
COMMENT ON COLUMN crm_erp_customer_mapping.match_rule IS '匹配规则';
COMMENT ON COLUMN crm_erp_customer_mapping.match_confidence IS '匹配置信度';
COMMENT ON COLUMN crm_erp_customer_mapping.sync_status IS '同步状态';
COMMENT ON COLUMN crm_erp_customer_mapping.sync_time IS '同步时间';
COMMENT ON COLUMN crm_erp_customer_mapping.ext_info IS '扩展信息(JSON)';
COMMENT ON COLUMN crm_erp_customer_mapping.deleted IS '删除标记';
COMMENT ON COLUMN crm_erp_customer_mapping.create_by IS '创建人ID';
COMMENT ON COLUMN crm_erp_customer_mapping.create_time IS '创建时间';
COMMENT ON COLUMN crm_erp_customer_mapping.update_by IS '更新人ID';
COMMENT ON COLUMN crm_erp_customer_mapping.update_time IS '更新时间';