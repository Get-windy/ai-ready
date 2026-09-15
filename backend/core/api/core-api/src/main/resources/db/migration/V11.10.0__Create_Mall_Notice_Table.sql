-- =====================================================
-- V11.10.0: 商城公告 (erp-mall)
--
-- mall_notice 商城公告表
--   notice_type: 1=公告 2=活动 3=系统
--   status:      0=草稿 1=已发布 2=已下线
-- =====================================================

CREATE TABLE IF NOT EXISTS mall_notice (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT,
    -- 1=公告 2=活动 3=系统
    notice_type     INT             NOT NULL DEFAULT 1,
    -- 0=草稿 1=已发布 2=已下线
    status          INT             NOT NULL DEFAULT 0,
    publish_time    TIMESTAMP,
    sort            INT             DEFAULT 0,

    -- 审计列
    create_by       BIGINT,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT             NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mall_notice_tenant ON mall_notice(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mall_notice_status ON mall_notice(status);
CREATE INDEX IF NOT EXISTS idx_mall_notice_type   ON mall_notice(notice_type);

COMMENT ON TABLE  mall_notice IS '商城公告';
COMMENT ON COLUMN mall_notice.notice_type IS '公告类型: 1=公告 2=活动 3=系统';
COMMENT ON COLUMN mall_notice.status IS '状态: 0=草稿 1=已发布 2=已下线';
COMMENT ON COLUMN mall_notice.publish_time IS '发布时间';

-- ═══════════════════════════════════════════════════════
-- 示例数据 (tenant_id=1, 固定ID幂等, 可安全删除)
-- ═══════════════════════════════════════════════════════
INSERT INTO mall_notice (id, tenant_id, title, content, notice_type, status, publish_time, sort)
VALUES (1, 1, '【示例】商城上线公告', '欢迎光临企智连商城，新店开业全场优惠！', 1, 1, CURRENT_TIMESTAMP, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO mall_notice (id, tenant_id, title, content, notice_type, status, publish_time, sort)
VALUES (2, 1, '【示例】系统维护通知(草稿)', '系统将于本周日凌晨进行升级维护，请提前保存数据。', 3, 0, NULL, 2)
ON CONFLICT (id) DO NOTHING;
