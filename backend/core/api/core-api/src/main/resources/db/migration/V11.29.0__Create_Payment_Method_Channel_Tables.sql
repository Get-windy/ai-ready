-- ============================================================
-- V11.29.0: 创建支付方式/支付渠道主数据表
--
-- 背景：支付方式/渠道为收付款单的核心数据来源，
-- 此前仅支付流水有记录无主数据，收付款单收款方式使用硬编码下拉。
-- 本表启用后收付款单收款方式改为读支付方式档案。
--
-- 幂等：IF NOT EXISTS + DO $$ ... END $$ 守卫。
-- ============================================================

-- ═══════════════════════════════════════════════════════════════
-- 1. 支付方式主表 md_payment_method
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS md_payment_method (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,

    -- 支付方式基本信息
    method_code         VARCHAR(50)     NOT NULL,
    method_name         VARCHAR(100)    NOT NULL,
    method_type         VARCHAR(30)     NOT NULL DEFAULT 'OTHER',
    -- method_type: CASH=现金 BANK=银行转账 WECHAT=微信 ALIPAY=支付宝 CHECK=支票 OTHER=其他

    -- 默认入账关联
    account_id          BIGINT,
    -- account_id → finance_account.id，默认入账账户

    -- 费率控制
    fee_rate            NUMERIC(8,4)    DEFAULT 0,

    -- 显示/排序
    is_default          INTEGER         DEFAULT 0,
    sort                INTEGER         DEFAULT 0,
    status              INTEGER         DEFAULT 1,

    -- 系统字段（审计五字段 + 租户）
    deleted_flag        INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE md_payment_method IS '支付方式主数据';
COMMENT ON COLUMN md_payment_method.method_code IS '支付方式编码（唯一，如 CASH/BANK/WECHAT/ALIPAY）';
COMMENT ON COLUMN md_payment_method.method_name IS '支付方式名称（如 现金/银行转账/微信支付/支付宝）';
COMMENT ON COLUMN md_payment_method.method_type IS '支付方式类型：CASH=现金 BANK=银行转账 WECHAT=微信 ALIPAY=支付宝 CHECK=支票 OTHER=其他';
COMMENT ON COLUMN md_payment_method.account_id IS '默认入账账户ID → finance_account.id';
COMMENT ON COLUMN md_payment_method.fee_rate IS '手续费率（如 0.006=0.6%）';
COMMENT ON COLUMN md_payment_method.is_default IS '是否默认：0-否 1-是';
COMMENT ON COLUMN md_payment_method.sort IS '排序号（升序）';
COMMENT ON COLUMN md_payment_method.status IS '状态：0-停用 1-启用';
COMMENT ON COLUMN md_payment_method.deleted_flag IS '删除标志：0-正常 1-已删除';
COMMENT ON COLUMN md_payment_method.created_at IS '创建时间';
COMMENT ON COLUMN md_payment_method.updated_at IS '更新时间';
COMMENT ON COLUMN md_payment_method.created_by IS '创建者ID';
COMMENT ON COLUMN md_payment_method.updated_by IS '更新者ID';

CREATE UNIQUE INDEX IF NOT EXISTS idx_md_payment_method_code ON md_payment_method(tenant_id, method_code);

-- ═══════════════════════════════════════════════════════════════
-- 2. 支付渠道主数据表 md_payment_channel
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS md_payment_channel (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,

    -- 渠道基本信息
    channel_code        VARCHAR(50)     NOT NULL,
    channel_name        VARCHAR(100)    NOT NULL,
    method_id           BIGINT          NOT NULL,
    -- method_id → md_payment_method.id，关联的支付方式

    -- 商户配置
    merchant_no         VARCHAR(200),
    config_json         TEXT,
    -- config_json 存储渠道特定配置（如 app_id, api_key, 回调地址等），JSON 格式

    -- 显示/排序
    sort                INTEGER         DEFAULT 0,
    status              INTEGER         DEFAULT 1,

    -- 系统字段（审计五字段 + 租户）
    deleted_flag        INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE md_payment_channel IS '支付渠道主数据（支付方式的具体实例）';
COMMENT ON COLUMN md_payment_channel.channel_code IS '渠道编码（唯一，如 WECHAT_MP / ALIPAY_WEB）';
COMMENT ON COLUMN md_payment_channel.channel_name IS '渠道名称（如 微信-公众号支付 / 支付宝-网页支付）';
COMMENT ON COLUMN md_payment_channel.method_id IS '关联支付方式ID → md_payment_method.id';
COMMENT ON COLUMN md_payment_channel.merchant_no IS '商户号（微信商户号/支付宝PID等）';
COMMENT ON COLUMN md_payment_channel.config_json IS '渠道配置JSON（app_id/api_key/回调地址等）';
COMMENT ON COLUMN md_payment_channel.sort IS '排序号（升序）';
COMMENT ON COLUMN md_payment_channel.status IS '状态：0-停用 1-启用';
COMMENT ON COLUMN md_payment_channel.deleted_flag IS '删除标志：0-正常 1-已删除';
COMMENT ON COLUMN md_payment_channel.created_at IS '创建时间';
COMMENT ON COLUMN md_payment_channel.updated_at IS '更新时间';
COMMENT ON COLUMN md_payment_channel.created_by IS '创建者ID';
COMMENT ON COLUMN md_payment_channel.updated_by IS '更新者ID';

CREATE UNIQUE INDEX IF NOT EXISTS idx_md_payment_channel_code ON md_payment_channel(tenant_id, channel_code);
CREATE INDEX IF NOT EXISTS idx_md_payment_channel_method ON md_payment_channel(method_id);

-- ═══════════════════════════════════════════════════════════════
-- 3. 预置示例数据（tenants_id=1 系统租户）
-- ═══════════════════════════════════════════════════════════════

INSERT INTO md_payment_method (id, tenant_id, method_code, method_name, method_type, account_id, fee_rate, is_default, sort, status)
SELECT * FROM (VALUES
    (1, 1, 'CASH',     '现金',       'CASH',   NULL::bigint, 0.0000, 1, 1, 1),
    (2, 1, 'BANK',     '银行转账',   'BANK',   NULL::bigint, 0.0000, 0, 2, 1),
    (3, 1, 'WECHAT',   '微信支付',   'WECHAT', NULL::bigint, 0.0060, 0, 3, 1),
    (4, 1, 'ALIPAY',   '支付宝',     'ALIPAY', NULL::bigint, 0.0060, 0, 4, 1),
    (5, 1, 'CHECK',    '支票',       'CHECK',  NULL::bigint, 0.0000, 0, 5, 1),
    (6, 1, 'OTHER',    '其他',       'OTHER',  NULL::bigint, 0.0000, 0, 9, 1)
) AS seed(id, tenant_id, method_code, method_name, method_type, account_id, fee_rate, is_default, sort, status)
WHERE NOT EXISTS (
    SELECT 1 FROM md_payment_method t WHERE t.tenant_id = seed.tenant_id AND t.method_code = seed.method_code
);
