-- ============================================================
-- V11.92.0: 修复 erp_supplier.tenant_id 类型与全局租户拦截器不匹配
--
-- 全局租户拦截器 AiReadyTenantLineInnerInterceptor 给 SQL 自动拼 `tenant_id = ?`（整型），
-- 而 erp_supplier.tenant_id 为 character varying，导致 `character varying = integer`
-- 操作符不存在 → 所有查询 erp_supplier 的接口（供应商列表/下拉 /supplier/list、/supplier/page）报错。
-- 对齐全系统（biz_party/erp_payment/sys_menu 均 BIGINT）。
-- 当前表无数据，直接类型转换安全。
-- ============================================================

ALTER TABLE erp_supplier ALTER COLUMN tenant_id TYPE BIGINT USING tenant_id::bigint;
