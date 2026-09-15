-- V9.13.0 MD Party系统跟进记录表 + 索引优化
-- 1. 创建 biz_party_follow 跟进记录表
-- 2. 为 biz_party 表补充缺失索引
-- 3. 为 biz_party_category 补充缺失索引

-- ============================================================
-- 1. 创建跟进记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_party_follow (
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT NOT NULL,
    follow_type     INTEGER DEFAULT 1,
    content         TEXT,
    follow_result   INTEGER DEFAULT 3,
    next_follow_time TIMESTAMP,
    follow_by       BIGINT,
    follow_by_name  VARCHAR(100),
    remark          VARCHAR(500),
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_biz_party_follow_party ON biz_party_follow(party_id);

-- ============================================================
-- 2. biz_party 表补充索引
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_biz_party_party_code') THEN
        CREATE INDEX idx_biz_party_party_code ON biz_party(party_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_biz_party_party_name') THEN
        CREATE INDEX idx_biz_party_party_name ON biz_party(party_name);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_biz_party_party_type') THEN
        CREATE INDEX idx_biz_party_party_type ON biz_party(party_type);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_biz_party_category_id') THEN
        CREATE INDEX idx_biz_party_category_id ON biz_party(category_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_biz_party_status') THEN
        CREATE INDEX idx_biz_party_status ON biz_party(party_status);
    END IF;
END $$;

-- ============================================================
-- 3. biz_party_category 补充索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_biz_party_category_code ON biz_party_category(category_code);
CREATE INDEX IF NOT EXISTS idx_biz_party_category_sort ON biz_party_category(sort_order);
