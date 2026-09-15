-- =====================================================
-- V9.25.0: 外链同步增强 — 同步方向 + 字段映射配置
-- 1. sync_data_source 增加 sync_direction 字段
-- 2. 新建 sync_field_mapping 表存储字段映射规则
-- =====================================================

-- 1. 添加同步方向字段（默认 inbound：外部→系统）
ALTER TABLE sync_data_source
    ADD COLUMN IF NOT EXISTS sync_direction VARCHAR(20) DEFAULT 'inbound';

COMMENT ON COLUMN sync_data_source.sync_direction IS '同步方向: inbound=外部→系统, outbound=系统→外部, bidirectional=双向';

-- 2. 字段映射配置表
CREATE TABLE IF NOT EXISTS sync_field_mapping (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    source_config_id BIGINT NOT NULL,           -- 关联 sync_data_source.id
    bill_type       VARCHAR(50) NOT NULL,        -- 单据类型（601=客户, 604=供应商 等）
    source_field    VARCHAR(100) NOT NULL,       -- 外部系统字段名
    source_label    VARCHAR(100),                -- 外部系统字段显示名
    target_field    VARCHAR(100) NOT NULL,       -- 本地系统字段名
    target_label    VARCHAR(100),                -- 本地系统字段显示名
    transform_type  VARCHAR(50) DEFAULT 'direct',-- 转换类型: direct/enum/公式/默认值
    transform_rule  TEXT,                        -- 转换规则（JSON 或表达式）
    default_value   VARCHAR(255),                -- 默认值
    required        BOOLEAN DEFAULT FALSE,       -- 是否必填
    sort_order      INT DEFAULT 0,               -- 排序
    status          INT DEFAULT 1,               -- 状态: 1=启用, 0=禁用
    deleted         INT DEFAULT 0,
    create_time     TIMESTAMP DEFAULT NOW(),
    update_time     TIMESTAMP DEFAULT NOW(),
    create_by       BIGINT,
    update_by       BIGINT
);

CREATE INDEX IF NOT EXISTS idx_sync_field_mapping_config ON sync_field_mapping(source_config_id, bill_type);
CREATE INDEX IF NOT EXISTS idx_sync_field_mapping_tenant ON sync_field_mapping(tenant_id);

COMMENT ON TABLE sync_field_mapping IS '外链同步字段映射配置';
COMMENT ON COLUMN sync_field_mapping.source_config_id IS '关联的同步配置ID';
COMMENT ON COLUMN sync_field_mapping.bill_type IS '单据类型编码';
COMMENT ON COLUMN sync_field_mapping.transform_type IS '转换类型: direct=直接映射, enum=枚举转换, formula=公式, default=默认值';

-- 3. 补充单据类型字典数据（BILL_TYPE 已存在时插入）
-- 如果 BILL_TYPE 字典类型不存在则跳过（可通过系统字典管理界面添加）
DO $$
DECLARE
    v_dict_type_id BIGINT;
BEGIN
    SELECT id INTO v_dict_type_id FROM sys_dict_type WHERE dict_code = 'BILL_TYPE' AND deleted = 0 LIMIT 1;
    IF v_dict_type_id IS NOT NULL THEN
        -- 基础数据
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '601', '客户', 1, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '604', '供应商', 2, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '504', '商品', 10, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        -- 销售数据
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '101', '销售订单', 20, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '102', '销售出库单', 21, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '103', '销售退货单', 22, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        -- 采购数据
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '201', '采购订单', 30, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '202', '采购入库单', 31, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '203', '采购退货单', 32, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        -- 库存数据
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '801', '库存查询', 40, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '802', '入库单', 41, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '803', '出库单', 42, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        -- 财务数据
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '301', '应收账款', 50, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '302', '应付账款', 51, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '303', '收款单', 52, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
        INSERT INTO sys_dict_item (tenant_id, dict_type_id, item_value, item_text, sort_order, status, is_default, deleted, create_time)
        VALUES (1, v_dict_type_id, '304', '付款单', 53, 'enabled', 'N', 0, NOW()) ON CONFLICT DO NOTHING;
    END IF;
END $$;
