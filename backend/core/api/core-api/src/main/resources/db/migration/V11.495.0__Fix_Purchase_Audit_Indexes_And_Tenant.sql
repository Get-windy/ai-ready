-- 采购模块审计修复（2026-09-22 审计 → 2026-09-23 落地）
--
-- 本迁移只做**结构性修复**，不含菜单与角色授权（那两类属系统配置数据，另行拍板）。
-- 覆盖三项：
--   ① 单据号唯一索引：补 tenant_id、补 WHERE deleted = 0，并给换货单补上唯一约束
--   ② 询价/报价 4 张表补 tenant_id 列（此前无该列，多租户拦截器注入后 SQL 直接报「字段不存在」）
--   ③ 采购列表页高频查询缺索引补齐
--
-- ── ① 单据号唯一索引为什么必须改 ───────────────────────────────
-- 原索引是**全局唯一且不含 deleted**：
--   · 全局唯一 ⇒ 与号段生成器的租户口径冲突。合同表建表注释（V11.439.0）已明确
--     「不同租户可以各自出现相同合同号」，即单号只在租户内唯一。三张表沿用了旧写法，
--     租户 B 的单号会被租户 A 占住 → 建档 `duplicate key` 直接 400。
--   · 不含 deleted ⇒ 单据逻辑删除后单号**永久占用**，同号补录必失败。
-- 换货单更严重：`exchange_no` 连唯一索引都没有（只有主键），并发生成/重试会静默写入重复单号。
-- 口径统一为 `(tenant_id, 单号) WHERE deleted = 0`，与 purchase_contract 的
-- `uk_purchase_contract_no_tenant(tenant_id, contract_no)` 对齐。
-- 前置已核：四张表在 devdb 上按 (tenant_id, 单号) 分组无重复行，索引可安全创建。
--
-- ── ② 为什么是「补列」而不是「加 @InterceptorIgnore」 ─────────────
-- 这四张表是租户业务单据（询价单/报价单），租户归属是其固有属性，靠忽略隔离无法做归属校验。
-- 表当前均为 0 行，补列无回填风险。列定义与兄弟表 erp_purchase_inbound.tenant_id 保持一致。
--
-- 回滚：本迁移可逆（DROP INDEX / ADD COLUMN 的反向语句），但一旦有数据写入即需按业务判断。
-- =============================================================

-- ── ① 单据号唯一索引：补 tenant_id + 限定未删除行 ─────────────
DROP INDEX IF EXISTS uk_erp_purchase_order_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_order_no_tenant
    ON erp_purchase_order (tenant_id, order_no)
    WHERE deleted = 0;

DROP INDEX IF EXISTS uk_erp_purchase_return_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_return_no_tenant
    ON erp_purchase_return (tenant_id, return_no)
    WHERE deleted = 0;

DROP INDEX IF EXISTS uk_erp_purchase_inbound_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_inbound_no_tenant
    ON erp_purchase_inbound (tenant_id, inbound_no)
    WHERE deleted = 0;

-- 换货单此前无单据号唯一约束，本次补上（先确保同名普通索引不冲突：保留 idx_purchase_exchange_no 无害）
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_exchange_no_tenant
    ON erp_purchase_exchange (tenant_id, exchange_no)
    WHERE deleted = 0;

-- ── ② 询价/报价 4 张表补 tenant_id ───────────────────────────
-- 这 4 张表不在 MyBatisPlusConfig.IGNORE_TENANT_TABLES 内，而多租户拦截器对已登录会话
-- 一律注入 tenant_id（2026-09-21 平台-BREAK-01 收紧为 fail-closed），缺列即整块 500。
ALTER TABLE purchase_inquiry        ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;
ALTER TABLE purchase_inquiry_item   ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;
ALTER TABLE purchase_supplier_quote ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;
ALTER TABLE purchase_quote_item     ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_purchase_inquiry_tenant        ON purchase_inquiry (tenant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_inquiry_item_tenant   ON purchase_inquiry_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_supplier_quote_tenant ON purchase_supplier_quote (tenant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_quote_item_tenant     ON purchase_quote_item (tenant_id);

-- ── ③ 列表页高频查询索引 ────────────────────────────────────
-- 采购订单列表固定按「租户 + 单据日期区间」和「租户 + 状态」过滤，
-- 原先只有 tenant_id / supplier_id / order_no 三个单列索引，日期区间只能全表扫。
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_tenant_date
    ON erp_purchase_order (tenant_id, order_date);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_tenant_status
    ON erp_purchase_order (tenant_id, status);

-- 入库明细按商品反查（如「某商品的历史入库」）此前无索引
CREATE INDEX IF NOT EXISTS idx_erp_purchase_inbound_item_product
    ON erp_purchase_inbound_item (product_id);

-- 退货 / 入库列表固定「租户 + 单据日期」过滤
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_tenant_date
    ON erp_purchase_return (tenant_id, return_date);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_inbound_tenant_date
    ON erp_purchase_inbound (tenant_id, inbound_date);
