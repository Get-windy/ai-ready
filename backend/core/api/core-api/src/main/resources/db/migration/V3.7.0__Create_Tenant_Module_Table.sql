-- ============================================================
-- 租户模块调用权配置表
-- 记录每个租户购买/开通的模块，控制功能可见性
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_tenant_module (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL,
    module_name VARCHAR(100),
    purchase_type VARCHAR(20) NOT NULL DEFAULT 'manual'
        CONSTRAINT ck_module_purchase_type CHECK (purchase_type IN ('permanent', 'auto_renew', 'manual')),
    expire_time TIMESTAMP,
    status SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted SMALLINT DEFAULT 0,
    CONSTRAINT uk_tenant_module UNIQUE (tenant_id, module_code)
);

COMMENT ON TABLE sys_tenant_module IS '租户模块调用权配置表';
COMMENT ON COLUMN sys_tenant_module.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_tenant_module.module_code IS '模块编码（如 order:sale, warehouse, finance）';
COMMENT ON COLUMN sys_tenant_module.module_name IS '模块名称';
COMMENT ON COLUMN sys_tenant_module.purchase_type IS '购买类型：permanent-永久 auto_renew-自动续费 manual-手动延期';
COMMENT ON COLUMN sys_tenant_module.expire_time IS '到期时间（permanent 类型可为 null）';
COMMENT ON COLUMN sys_tenant_module.status IS '状态 0-正常 1-停用';

-- 插入默认模块定义（系统预置模块）
INSERT INTO sys_tenant_module (tenant_id, module_code, module_name, purchase_type, status) VALUES
    (0, 'sale', '销售管理', 'permanent', 0),
    (0, 'sale:order', '销售订单', 'permanent', 0),
    (0, 'sale:delivery', '销售发货', 'permanent', 0),
    (0, 'purchase', '采购管理', 'permanent', 0),
    (0, 'purchase:order', '采购订单', 'permanent', 0),
    (0, 'purchase:receipt', '采购入库', 'permanent', 0),
    (0, 'warehouse', '库存管理', 'permanent', 0),
    (0, 'finance', '财务管理', 'permanent', 0),
    (0, 'report', '报表分析', 'permanent', 0),
    (0, 'customer', '客户管理', 'permanent', 0),
    (0, 'supplier', '供应商管理', 'permanent', 0),
    (0, 'system', '系统设置', 'permanent', 0);
