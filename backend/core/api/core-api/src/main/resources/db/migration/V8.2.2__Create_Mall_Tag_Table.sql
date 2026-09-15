-- =====================================================
-- V8.2.0: 商城标签表（用户自定义，非硬编码）
-- =====================================================

CREATE TABLE IF NOT EXISTS erp_mall_tag (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    tag_name             VARCHAR(50) NOT NULL,
    sort_order           INTEGER NOT NULL DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_by            BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_mall_tag IS '商城标签（用户自定义）';
COMMENT ON COLUMN erp_mall_tag.tag_name IS '标签名称';
COMMENT ON COLUMN erp_mall_tag.sort_order IS '排序序号';

CREATE INDEX IF NOT EXISTS idx_mall_tag_tenant ON erp_mall_tag(tenant_id);
