-- =====================================================
-- V11.11.0: 商城搜索关键词库 (erp-mall)
--
-- mall_keyword 搜索关键词表
--   keyword_type: 1=热门 2=置顶 3=屏蔽
--   status:       1=启用 0=禁用
--   唯一约束: (tenant_id, keyword)
-- =====================================================

CREATE TABLE IF NOT EXISTS mall_keyword (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    keyword         VARCHAR(100)    NOT NULL,
    -- 1=热门 2=置顶 3=屏蔽
    keyword_type    INT             NOT NULL DEFAULT 1,
    sort            INT             DEFAULT 0,
    -- 1=启用 0=禁用
    status          INT             NOT NULL DEFAULT 1,

    -- 审计列
    create_by       BIGINT,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT             NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mall_keyword_tenant_kw ON mall_keyword(tenant_id, keyword);
CREATE INDEX IF NOT EXISTS idx_mall_keyword_type   ON mall_keyword(keyword_type);
CREATE INDEX IF NOT EXISTS idx_mall_keyword_status ON mall_keyword(status);

COMMENT ON TABLE  mall_keyword IS '商城搜索关键词库';
COMMENT ON COLUMN mall_keyword.keyword_type IS '关键词类型: 1=热门 2=置顶 3=屏蔽';
COMMENT ON COLUMN mall_keyword.status IS '状态: 1=启用 0=禁用';

-- ═══════════════════════════════════════════════════════
-- 示例数据 (tenant_id=1, 固定ID幂等, 可安全删除)
-- ═══════════════════════════════════════════════════════
INSERT INTO mall_keyword (id, tenant_id, keyword, keyword_type, sort, status)
VALUES (1, 1, '矿泉水', 1, 1, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO mall_keyword (id, tenant_id, keyword, keyword_type, sort, status)
VALUES (2, 1, '办公用品', 1, 2, 1)
ON CONFLICT (id) DO NOTHING;
