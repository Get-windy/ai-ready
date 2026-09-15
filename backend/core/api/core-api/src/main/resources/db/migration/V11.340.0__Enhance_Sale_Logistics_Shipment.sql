-- =============================================================================
-- 物流发货能力增强（P0/P1/P2）· PostgreSQL
--
-- 背景（《物流发货-业界做法调研.md》）：
--   ql361 把「承运商 / 运单号」退化成订单表头上的两个文本字段；业界成熟模型是
--   「发货单 → 出库单 → 运单/包裹（承运商 + 运单号 + 件数重量 + 运费）」。
--   本系统其实**已经存在** 1:N 物流子表 erp_sale_order_logistics（后端已实现、前端未接线、0 行），
--   本次把它接起来并补齐业界要素。
--
--   P0-1 接线：物流备注/包裹读写子表，主表扁平列降级为「列表展示快照」
--   P0-2 承运商档案引用：子表补 logistics_company_id（biz_party.id，partnerType=LOGISTICS）
--   P1   一单多包：子表按「包裹」语义使用，补 包裹号/件数/重量/体积/状态
--   P2-4 运费规则：新建 erp_freight_rule（承运商×区域×重量区间 首重续重）
--   P2-6 ASN：新建 erp_shipment_notify（发货通知台账，配置化回调 + 幂等 + 重试）
--
-- 幂等：ADD COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS
-- =============================================================================

-- ═══ P0-2 承运商档案引用 ═══
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS logistics_company_id BIGINT;
COMMENT ON COLUMN erp_sale_order_logistics.logistics_company_id IS '物流公司档案ID（biz_party.id，partnerType=LOGISTICS）；名称仍存 logistics_company 作快照';

-- ═══ P1 包裹/运单层 ═══
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS package_no     VARCHAR(64);
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS package_count  INTEGER;
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS package_weight NUMERIC(18,4);
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS package_volume NUMERIC(18,4);
-- 包裹状态：0-待发货 1-已发货 2-已签收 9-异常
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS package_status INTEGER DEFAULT 0;
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS remark         VARCHAR(500);
-- 子表此前只有 create_time；补 update_time 以支持 MyBatis-Plus INSERT_UPDATE 填充
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS update_time    TIMESTAMP;
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS tenant_id      BIGINT;

COMMENT ON COLUMN erp_sale_order_logistics.package_no     IS '包裹号/箱号（同一订单内序号，如 P1/P2；为空视为默认单包裹）';
COMMENT ON COLUMN erp_sale_order_logistics.package_count  IS '件数（包裹内件数）';
COMMENT ON COLUMN erp_sale_order_logistics.package_weight IS '包裹重量(kg)';
COMMENT ON COLUMN erp_sale_order_logistics.package_volume IS '包裹体积(m³)';
COMMENT ON COLUMN erp_sale_order_logistics.package_status IS '包裹状态：0-待发货 1-已发货 2-已签收 9-异常';
COMMENT ON COLUMN erp_sale_order_logistics.remark         IS '物流/包裹备注';

-- ═══ P2-4 运费对账（承运商账单金额 vs 我方计费）═══
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS freight_bill_amount NUMERIC(18,4);
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS freight_diff        NUMERIC(18,4);
ALTER TABLE erp_sale_order_logistics ADD COLUMN IF NOT EXISTS freight_reconciled  INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_sale_order_logistics.freight_bill_amount IS '承运商账单金额（对账用，人工/导入录入）';
COMMENT ON COLUMN erp_sale_order_logistics.freight_diff        IS '运费差异 = 账单金额 − 我方计费(shipping_fee)';
COMMENT ON COLUMN erp_sale_order_logistics.freight_reconciled  IS '对账状态：0-未对账 1-已对账';

CREATE INDEX IF NOT EXISTS idx_sale_order_logistics_order ON erp_sale_order_logistics (order_id);
CREATE INDEX IF NOT EXISTS idx_sale_order_logistics_waybill ON erp_sale_order_logistics (tenant_id, waybill_no)
    WHERE waybill_no IS NOT NULL AND waybill_no <> '';

-- ═══ P2-4 承运商运费规则 ═══
CREATE TABLE IF NOT EXISTS erp_freight_rule (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT,
    carrier_id    BIGINT,
    carrier_name  VARCHAR(128),
    area          VARCHAR(128),
    min_weight    NUMERIC(18,4),
    max_weight    NUMERIC(18,4),
    first_weight  NUMERIC(18,4),
    first_price   NUMERIC(18,4),
    add_step      NUMERIC(18,4),
    add_price     NUMERIC(18,4),
    base_fee      NUMERIC(18,4),
    enabled       INTEGER DEFAULT 1,
    priority      INTEGER DEFAULT 0,
    remark        VARCHAR(255),
    deleted       INTEGER DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    version       INTEGER DEFAULT 0
);
COMMENT ON TABLE  erp_freight_rule IS '承运商运费规则：按 承运商 × 区域 × 重量区间 取首重/续重（用于发货时试算运费与对账）';
COMMENT ON COLUMN erp_freight_rule.area        IS '区域/线路（为空=通用规则）';
COMMENT ON COLUMN erp_freight_rule.first_weight IS '首重(kg)';
COMMENT ON COLUMN erp_freight_rule.first_price  IS '首重价(元)';
COMMENT ON COLUMN erp_freight_rule.add_step     IS '续重步长(kg)';
COMMENT ON COLUMN erp_freight_rule.add_price    IS '每续重步长单价(元)';
COMMENT ON COLUMN erp_freight_rule.base_fee     IS '基础费(元)，在首重价之上叠加';
CREATE INDEX IF NOT EXISTS idx_freight_rule_carrier ON erp_freight_rule (tenant_id, carrier_id) WHERE deleted = 0;

-- ═══ P2-6 ASN 发货通知台账 ═══
CREATE TABLE IF NOT EXISTS erp_shipment_notify (
    id             BIGINT PRIMARY KEY,
    tenant_id      BIGINT,
    order_id       BIGINT,
    order_no       VARCHAR(64),
    carrier_id     BIGINT,
    carrier_name   VARCHAR(128),
    waybill_no     VARCHAR(64),
    notify_type    VARCHAR(32),
    payload        TEXT,
    target_url     VARCHAR(500),
    status         INTEGER DEFAULT 0,
    retry_count    INTEGER DEFAULT 0,
    last_error     VARCHAR(500),
    sent_time      TIMESTAMP,
    idempotent_key VARCHAR(128),
    deleted        INTEGER DEFAULT 0,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    create_by      BIGINT,
    update_by      BIGINT,
    version        INTEGER DEFAULT 0
);
COMMENT ON TABLE  erp_shipment_notify IS '发货通知（ASN）外发台账：发货后生成，按配置的回调地址推送，幂等 + 重试';
COMMENT ON COLUMN erp_shipment_notify.notify_type IS '通知类型：ASN-发货通知';
COMMENT ON COLUMN erp_shipment_notify.status      IS '0-待发送 1-已发送 2-发送失败';
COMMENT ON COLUMN erp_shipment_notify.idempotent_key IS '幂等键（订单+运单号），同键不重复生成';
CREATE INDEX IF NOT EXISTS idx_shipment_notify_order ON erp_shipment_notify (order_id) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_shipment_notify_idem ON erp_shipment_notify (tenant_id, idempotent_key)
    WHERE deleted = 0 AND idempotent_key IS NOT NULL;
