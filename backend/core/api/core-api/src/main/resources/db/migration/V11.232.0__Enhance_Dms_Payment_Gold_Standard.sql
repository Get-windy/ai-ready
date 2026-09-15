-- 收款管理金标准（配送 → 结算收款 → 收款管理，菜单 80920）
--
-- 背景（《收款管理开发文档》§2.3 缺陷）：
--   ① 二维码是**示例串**（`PaymentService` 返回 `pay.example.com` 假 URL）——不可真实支付；
--   ② **无支付回调** → 扫码后无法自动置已支付；
--   ③ **无台账接口** → 看不到"今日代收多少、谁还没交款"；
--   ④ **无资金上交/稽核** → 配送员代收的现金/扫码款如何上交企业无记录（资金安全缺口）；
--   ⑤ 命名歧义：接口 `paymentType` 实为**支付方式**，而实体 `paymentType` 是**收款类型**（代收货款/配送费）。
--
-- 本次落地：
--   1) 补 `pay_channel`（支付方式，与收款类型分离）、`trade_no`（平台交易号，回调/对账幂等键）、
--      `callback_time`；2) 补交款稽核列（交款状态/金额/时间/经办人/备注）；
--   3) 收款码服务地址**配置化**（未配置则不做扫码收款，杜绝假二维码）。
--
-- 幂等：IF NOT EXISTS / WHERE NOT EXISTS。

-- ─────────────────────────────────────────────
-- 1) 支付方式 / 平台流水 / 回调
-- ─────────────────────────────────────────────
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS pay_channel      INTEGER;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS pay_channel_name VARCHAR(50);
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS trade_no         VARCHAR(64);
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS callback_time    TIMESTAMP;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS rider_id         BIGINT;

COMMENT ON COLUMN dms_payment.payment_type     IS '收款类型 1-代收货款(负债) 2-配送费(收入)';
COMMENT ON COLUMN dms_payment.pay_channel      IS '支付方式 1-微信 2-支付宝 3-现金 4-POS 5-银行转账 9-其他（与收款类型区分）';
COMMENT ON COLUMN dms_payment.trade_no         IS '支付平台交易号（回调/对账幂等键，凭证留存）';
COMMENT ON COLUMN dms_payment.callback_time    IS '支付回调到达时间';

-- ─────────────────────────────────────────────
-- 2) 资金上交 / 稽核（资金安全核心）
-- ─────────────────────────────────────────────
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS handover_status  INTEGER NOT NULL DEFAULT 0;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS handover_amount  NUMERIC(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS handover_time    TIMESTAMP;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS handover_by      BIGINT;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS handover_by_name VARCHAR(100);
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS handover_remark  VARCHAR(500);

COMMENT ON COLUMN dms_payment.handover_status IS '交款状态 0-未交 1-部分交 2-已交（现金/线下收款特有）';
COMMENT ON COLUMN dms_payment.handover_amount IS '已上交金额';
COMMENT ON COLUMN dms_payment.handover_by_name IS '交款经办人姓名快照';

-- ─────────────────────────────────────────────
-- 3) 收款码服务（配置化；未配置则不做扫码收款，避免假二维码）
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 0, 'dms.payment.qrcode.base-url', '', '企业收款码服务地址（为空表示未开通扫码收款，仅支持现金/POS 等线下方式）', 'TENANT', 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM dms_config c WHERE c.config_key = 'dms.payment.qrcode.base-url' AND c.tenant_id = 0 AND c.deleted = 0
);

-- ─────────────────────────────────────────────
-- 4) 索引
-- ─────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_dms_payment_task     ON dms_payment (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_dms_payment_status   ON dms_payment (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_dms_payment_handover ON dms_payment (tenant_id, handover_status);
CREATE INDEX IF NOT EXISTS idx_dms_payment_trade_no ON dms_payment (trade_no);

-- ─────────────────────────────────────────────
-- 5) 收款码 URL 扩长（原 VARCHAR(50) 存不下真实收款码地址，插入即报「值太长」）
-- ─────────────────────────────────────────────
ALTER TABLE dms_payment ALTER COLUMN qrcode_url TYPE VARCHAR(512);
