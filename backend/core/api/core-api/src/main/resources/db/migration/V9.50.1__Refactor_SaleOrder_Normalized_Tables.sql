-- V9.50.0: 销售订单规范化重构
-- 对标 Odoo/SAP/金蝶/用友 生产级 ERP 架构
-- 将 130+ 列上帝表拆分为 25 列主表 + 8 个子表
-- 客户来源: ERP 往来单位 (biz_party)，非 CRM 公海客户
-- 执行时间: 2026-07-13

-- ═══════════════════════════════════════════
-- 1. 往来单位快照表 (1:1)
-- 快照下单时刻的 ERP 往来单位 (biz_party) 信息
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_partner_snapshot (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,

    -- 往来单位基本信息快照
    customer_name   VARCHAR(200),
    customer_code   VARCHAR(100),
    customer_level  VARCHAR(100),
    customer_grade_code VARCHAR(100),
    customer_grade_name VARCHAR(100),
    customer_ticket VARCHAR(20),
    customer_remark VARCHAR(1000),

    -- 银行/税务信息快照 (来源: biz_party)
    bank_name       VARCHAR(200),
    bank_account    VARCHAR(100),
    tax_no          VARCHAR(100),

    -- 区域
    region          VARCHAR(100),

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_partner_snapshot_order ON erp_sale_order_partner_snapshot(order_id);

COMMENT ON TABLE erp_sale_order_partner_snapshot IS '销售订单往来单位快照 - 快照下单时刻的 biz_party 信息';

-- ═══════════════════════════════════════════
-- 2. 收货地址表 (1:N)
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_delivery_address (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    address_type    VARCHAR(20) NOT NULL DEFAULT 'RECEIVER',  -- RECEIVER / PICKUP

    contact_name    VARCHAR(100),
    phone           VARCHAR(50),
    address         VARCHAR(500),

    is_default      BOOLEAN DEFAULT FALSE,
    sequence        INTEGER DEFAULT 0,

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_delivery_address_order ON erp_sale_order_delivery_address(order_id);

COMMENT ON TABLE erp_sale_order_delivery_address IS '销售订单收货地址 - 支持收货/提货多种地址类型';

-- ═══════════════════════════════════════════
-- 3. 结算信息表 (1:1)
-- 结款方式、信用额度、收款日/对账日
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_settlement (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,

    -- 结款方式
    settlement_method VARCHAR(50),

    -- 信用额度快照 (来源: biz_party.credit_limit)
    credit_limit    DECIMAL(18,2) DEFAULT 0,
    available_credit DECIMAL(18,2) DEFAULT 0,
    prev_debt       DECIMAL(18,2) DEFAULT 0,

    -- 收款日/对账日
    payment_date    DATE,
    reconciliation_date DATE,

    -- 支付信息
    payment_account_id BIGINT,
    payment_method  VARCHAR(50),
    payment_status  INTEGER,

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_settlement_order ON erp_sale_order_settlement(order_id);

COMMENT ON TABLE erp_sale_order_settlement IS '销售订单结算信息 - 结款方式、信用额度、支付信息';

-- ═══════════════════════════════════════════
-- 4. 物流信息表 (1:N)
-- 配送/快递/自提，支持多包裹
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_logistics (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,

    logistics_type  VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',  -- DELIVERY / EXPRESS / PICKUP

    -- 配送信息
    delivery_method VARCHAR(50),
    delivery_route  VARCHAR(200),
    delivery_route_id BIGINT,
    driver_id       BIGINT,
    driver_name     VARCHAR(100),
    delivery_vehicle VARCHAR(100),

    -- 快递信息
    logistics_company VARCHAR(100),
    logistics_no    VARCHAR(200),
    waybill_no      VARCHAR(200),

    -- 费用
    freight_payer   VARCHAR(50),
    shipping_fee    DECIMAL(18,2) DEFAULT 0,
    cod_amount      DECIMAL(18,2) DEFAULT 0,

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_logistics_order ON erp_sale_order_logistics(order_id);

COMMENT ON TABLE erp_sale_order_logistics IS '销售订单物流信息 - 支持配送/快递/自提多种方式';

-- ═══════════════════════════════════════════
-- 5. 订金账户表 (1:N)
-- 替代原 depositAccount, depositAccount1-4 重复组
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_deposit (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,

    account_name    VARCHAR(200),
    amount          DECIMAL(18,2) DEFAULT 0,
    sequence        INTEGER DEFAULT 0,

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_deposit_order ON erp_sale_order_deposit(order_id);

COMMENT ON TABLE erp_sale_order_deposit IS '销售订单订金账户 - 替代重复组字段';

-- ═══════════════════════════════════════════
-- 6. 会员积分流水表 (1:1)
-- 记录本单引起的积分变动
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_points_journal (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,

    member_card_no  VARCHAR(64),
    member_name     VARCHAR(100),
    member_discount INTEGER,

    -- 积分变动
    prev_points     DECIMAL(18,2) DEFAULT 0,
    sale_points     DECIMAL(18,2) DEFAULT 0,
    return_points   DECIMAL(18,2) DEFAULT 0,
    exchange_points DECIMAL(18,2) DEFAULT 0,
    used_points     DECIMAL(18,2) DEFAULT 0,
    current_points  DECIMAL(18,2) DEFAULT 0,

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_points_journal_order ON erp_sale_order_points_journal(order_id);

COMMENT ON TABLE erp_sale_order_points_journal IS '销售订单会员积分流水 - 记录本单积分变动';

-- ═══════════════════════════════════════════
-- 7. 审核流水表 (1:N)
-- 记录提交/审核/反审核等操作流水
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_audit_trail (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,

    action          VARCHAR(20) NOT NULL,  -- SUBMIT / APPROVE / REJECT / CANCEL
    operator_id     BIGINT NOT NULL,
    operator_name   VARCHAR(100),
    action_time     TIMESTAMP NOT NULL,
    remark          VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_audit_trail_order ON erp_sale_order_audit_trail(order_id);

COMMENT ON TABLE erp_sale_order_audit_trail IS '销售订单审核流水 - 记录审批操作历史';

-- ═══════════════════════════════════════════
-- 8. 扩展信息表 (1:1)
-- 自定义字段、附件、摘要、表尾等
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_order_ext_info (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,

    -- 摘要/附件
    summary         VARCHAR(2000),
    attachment      TEXT,

    -- 自定义字段(表头)
    ext_num1        DECIMAL(18,2),
    ext_num2        DECIMAL(18,2),
    ext_text1       VARCHAR(500),
    ext_text2       VARCHAR(500),
    ext_text3       VARCHAR(500),

    -- 表尾自定义字段
    footer_ext_text1 VARCHAR(500),
    footer_ext_text2 VARCHAR(500),

    -- 扩展JSON
    ext_json        TEXT,

    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ext_info_order ON erp_sale_order_ext_info(order_id);

COMMENT ON TABLE erp_sale_order_ext_info IS '销售订单扩展信息 - 自定义字段、附件等';

-- ═══════════════════════════════════════════
-- 数据迁移: 从宽表迁移到子表
-- ═══════════════════════════════════════════

-- 迁移往来单位快照
INSERT INTO erp_sale_order_partner_snapshot (id, order_id, customer_name, customer_code, customer_level,
    customer_grade_code, customer_grade_name, customer_ticket, customer_remark, bank_name, bank_account,
    tax_no, region, create_time)
SELECT id, id, customer_name, customer_code, customer_level,
    customer_grade_code, customer_grade_name, customer_ticket, customer_remark, bank_name, bank_account,
    tax_no, region, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (customer_name IS NOT NULL OR customer_code IS NOT NULL)
ON CONFLICT (order_id) DO NOTHING;

-- 迁移收货地址
INSERT INTO erp_sale_order_delivery_address (id, order_id, address_type, contact_name, phone, address, is_default, sequence, create_time)
SELECT id + 1000000, id, 'RECEIVER', receiver_name, receiver_phone, shipping_address, true, 0, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (receiver_name IS NOT NULL OR shipping_address IS NOT NULL)
ON CONFLICT DO NOTHING;

-- 迁移提货地址 (如果有)
INSERT INTO erp_sale_order_delivery_address (id, order_id, address_type, contact_name, phone, address, is_default, sequence, create_time)
SELECT id + 2000000, id, 'PICKUP', contact_name, contact_phone, pickup_address, false, 1, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (contact_name IS NOT NULL OR pickup_address IS NOT NULL)
ON CONFLICT DO NOTHING;

-- 迁移结算信息 (源表无 payment_account_id，默认 NULL)
INSERT INTO erp_sale_order_settlement (id, order_id, settlement_method, credit_limit, available_credit, prev_debt,
    payment_date, reconciliation_date, payment_method, payment_status, create_time)
SELECT id, id, settlement_method, credit_limit, available_credit, prev_debt,
    payment_date, reconciliation_date, payment_method, payment_status, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (settlement_method IS NOT NULL OR credit_limit IS NOT NULL OR payment_method IS NOT NULL)
ON CONFLICT (order_id) DO NOTHING;

-- 迁移物流信息 (源表无 logistics_company/logistics_no/shipping_fee，跳过这些列)
INSERT INTO erp_sale_order_logistics (id, order_id, logistics_type, delivery_method, delivery_route, delivery_route_id,
    driver_id, driver_name, delivery_vehicle, waybill_no,
    freight_payer, cod_amount, create_time)
SELECT id, id, 'DELIVERY', delivery_method, delivery_route, delivery_route_id,
    driver_id, driver_name, delivery_vehicle, waybill_no,
    freight_payer, cod_amount, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (delivery_method IS NOT NULL OR waybill_no IS NOT NULL)
ON CONFLICT DO NOTHING;

-- 迁移订金账户
INSERT INTO erp_sale_order_deposit (id, order_id, account_name, amount, sequence, create_time)
SELECT id + 3000000, id, deposit_account, deposit_amount, 0, create_time
FROM erp_sale_order
WHERE deleted = 0 AND deposit_account IS NOT NULL
ON CONFLICT DO NOTHING;

-- 迁移会员积分流水 (源表无 member_card_no/member_name/member_discount，跳过这些列)
INSERT INTO erp_sale_order_points_journal (id, order_id,
    prev_points, sale_points, return_points, exchange_points, used_points, current_points, create_time)
SELECT id, id,
    prev_points, sale_points, return_points, exchange_points, used_points, current_points, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (sale_points IS NOT NULL)
ON CONFLICT (order_id) DO NOTHING;

-- 迁移审核流水 (提交记录)
INSERT INTO erp_sale_order_audit_trail (id, order_id, action, operator_id, operator_name, action_time, remark)
SELECT id + 4000000, id, 'SUBMIT', submitter_id, submitter_name, submit_time, NULL
FROM erp_sale_order
WHERE deleted = 0 AND submitter_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- 迁移审核流水 (审核记录)
INSERT INTO erp_sale_order_audit_trail (id, order_id, action, operator_id, operator_name, action_time, remark)
SELECT id + 5000000, id, 'APPROVE', auditor_id, auditor_name, audit_time, NULL
FROM erp_sale_order
WHERE deleted = 0 AND auditor_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- 迁移扩展信息
INSERT INTO erp_sale_order_ext_info (id, order_id, summary, attachment,
    ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
    footer_ext_text1, footer_ext_text2, ext_json, create_time)
SELECT id, id, summary, attachment,
    ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
    footer_ext_text1, footer_ext_text2, ext_info, create_time
FROM erp_sale_order
WHERE deleted = 0
  AND (summary IS NOT NULL OR ext_num1 IS NOT NULL OR ext_text1 IS NOT NULL OR ext_info IS NOT NULL)
ON CONFLICT (order_id) DO NOTHING;
