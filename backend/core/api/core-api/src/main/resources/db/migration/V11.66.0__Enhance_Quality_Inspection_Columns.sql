-- ============================================================
-- V11.66.0: 质检单表增强 + 菜单双入口对齐
--
-- 背景：quality_inspection（V8.13.0 建表）存在以下缺陷：
--   1) 前端新建表单提交「检验类型 inspectionType(INBOUND/OUTBOUND/PROCESS)」，
--      但实体与表均无该列，字段提交后丢失。
--   2) biz_id / biz_type / product_id 为 NOT NULL，但质检单允许独立录单
--      （仅来源单号 bizNo / 产品名称 productName），无来源单据/产品档案时插入失败。
--   3) inspection_result 注释未含 CONCESSION（让步接收）。
--   4) 菜单 90201（quality:inspection）为单入口，未实现主菜单→新建表单、[历史]→列表 的双入口。
-- ============================================================

-- 1. 补「检验类型」列（来料 IQC / 出库 OQC / 过程 IPQC）
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS inspection_type VARCHAR(20);
COMMENT ON COLUMN quality_inspection.inspection_type IS '检验类型: INBOUND来料, OUTBOUND出库, PROCESS过程';

-- 2. 放宽 NOT NULL：质检单可独立录单，来源单据/产品档案均可为空
ALTER TABLE quality_inspection ALTER COLUMN biz_id DROP NOT NULL;
ALTER TABLE quality_inspection ALTER COLUMN biz_type DROP NOT NULL;
ALTER TABLE quality_inspection ALTER COLUMN product_id DROP NOT NULL;

-- 3. 检验结果注释补充 CONCESSION（让步接收）
COMMENT ON COLUMN quality_inspection.inspection_result IS '检验结果: PASS合格, FAIL不合格, PENDING待检, CONCESSION让步接收';
COMMENT ON COLUMN quality_inspection.biz_type IS '业务类型: PURCHASE_ORDER采购, SALE_ORDER销售, STOCK_IN入库, STOCK_OUT出库';

-- 4. 质检单菜单 90201 双入口对齐：主菜单→新建表单，[历史]→列表
UPDATE sys_menu SET
  path = 'quality/inspection/form',
  component = 'quality/inspection/form',
  list_path = 'quality/inspection/list',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 90201;
