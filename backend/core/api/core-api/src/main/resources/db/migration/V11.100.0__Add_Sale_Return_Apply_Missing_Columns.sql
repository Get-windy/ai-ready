-- ============================================================
-- V11.100.0: 销售退货申请单补齐实体已声明但库中缺失的列
--
-- 背景：SaleReturn 实体声明 attachment，devdb 表无该列，
-- 导致 create/查询走 BaseMapper 全字段 SELECT 时抛
-- `字段 "attachment" 不存在`（创建单据 500）。
-- 口径：对照实体全字段逐一比对，缺列即补（幂等守卫）。
-- ============================================================

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS attachment VARCHAR(500);

COMMENT ON COLUMN erp_sale_return.attachment IS '附件（文件路径/URL，来源：单据附件）';
