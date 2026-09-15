-- ============================================================
-- V11.23.0: 财务双轨合并 - 清理 core-api cn.aiedge.finance 死表
--
-- 背景: core-api 的 cn.aiedge.finance 套已废弃，erp-finance 为唯一财务轨。
--   - reconciliation / other-income-doc 已搬迁至 erp-finance 复用，
--     fin_reconciliation(+_item) / fin_other_income_doc 保留。
--   - fin_accounting_period / fin_month_closing_log 为 erp 套在用，保留。
-- 执行前已在 devdb 逐表确认以下 7 张表均为 0 行，无数据迁移负担。
-- ============================================================

-- core-api 应收/应付/收付款空表（erp-finance 使用 erp_receivable/erp_payable 等）
DROP TABLE IF EXISTS fin_receivable;
DROP TABLE IF EXISTS fin_payable;
DROP TABLE IF EXISTS fin_payment;
DROP TABLE IF EXISTS fin_receipt;

-- core-api profit/cost 空表壳（无业务数据、无前端使用）
DROP TABLE IF EXISTS erp_profit_analysis;
DROP TABLE IF EXISTS erp_product_cost_standard;
DROP TABLE IF EXISTS erp_cost_allocation_rule;
