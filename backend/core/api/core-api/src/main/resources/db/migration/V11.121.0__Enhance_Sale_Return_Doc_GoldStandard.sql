-- ============================================================================
-- V11.121.0 销售退货单金标准升级
-- 1) erp_sale_return_doc 补审批落库列（实体 SaleReturnDoc 已有字段但建表遗漏）
-- 2) 补记账人列（列表「记账人」列 / 过账后记录操作员）
-- 说明：全部使用 IF NOT EXISTS，可重复执行。
-- 版本号说明：初版按当时最大号写成 V11.100.0，与并行开发的
--   V11.100.0__Add_Sale_Return_Apply_Missing_Columns.sql 撞号，Flyway 报
--   「Found more than one migration with version 11.100.0」；本文件已后移为 11.121.0。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 审批落库列
-- ------------------------------------------------------------
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS approved_time TIMESTAMP;
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS approved_note VARCHAR(500);
-- auditor 为历史字符串列（实体 SaleReturnDoc.auditor 是 String），与 auditor_name 同源
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS auditor VARCHAR(100);
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS auditor_id BIGINT;
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100);
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS audit_time TIMESTAMP;
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS submit_by BIGINT;
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS submit_time TIMESTAMP;

-- ------------------------------------------------------------
-- 2. 记账人（过账生成凭证时写入）
-- ------------------------------------------------------------
ALTER TABLE erp_sale_return_doc ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);

COMMENT ON COLUMN erp_sale_return_doc.approved_by IS '审批人ID';
COMMENT ON COLUMN erp_sale_return_doc.approved_time IS '审批时间';
COMMENT ON COLUMN erp_sale_return_doc.approved_note IS '审批意见';
COMMENT ON COLUMN erp_sale_return_doc.auditor IS '审核人（历史字符串口径，与 auditor_name 同源）';
COMMENT ON COLUMN erp_sale_return_doc.bookkeeper_name IS '记账人（审核过账生成凭证时的操作员）';

-- ------------------------------------------------------------
-- 3. 号段检索与列表过滤索引
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sale_return_doc_no ON erp_sale_return_doc (return_doc_no);
CREATE INDEX IF NOT EXISTS idx_sale_return_doc_order_date ON erp_sale_return_doc (order_date);
CREATE INDEX IF NOT EXISTS idx_sale_return_doc_status ON erp_sale_return_doc (status);
CREATE INDEX IF NOT EXISTS idx_sale_return_doc_item_doc_id ON erp_sale_return_doc_item (return_doc_id);
