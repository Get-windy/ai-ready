-- ============================================================================
-- 预订货单金标准增强（P0 差距修复）
--
-- 背景：
--   1) 表单「表尾自定义字段1/2」在实体 SalePreOrder 中已声明（footerExtText1/2），
--      但 erp_sale_pre_order 表从未建列 —— MyBatis-Plus 按非空字段插入时会带上这两列，
--      导致任何一次 create/update 都抛 BadSqlGrammar（列表长期 0 行，数据无法闭环）。
--   2) 单据编号必须来自后端号段（红线：严禁前端演示自增号）。
--      BizNumberGeneratorService.nextPreOrderNo() 已存在（prefix YDHD），
--      但 biz_number_sequence 未初始化 YDHD 序列，调用会抛「未配置编号序列」。
--
-- 全部 ADD COLUMN IF NOT EXISTS / ON CONFLICT DO NOTHING，可重复执行。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 表尾自定义字段（对标：新增预订货表单页「表尾自定义字段1~2」）
-- ------------------------------------------------------------
ALTER TABLE erp_sale_pre_order ADD COLUMN IF NOT EXISTS footer_ext_text1 varchar(200);
ALTER TABLE erp_sale_pre_order ADD COLUMN IF NOT EXISTS footer_ext_text2 varchar(200);

COMMENT ON COLUMN erp_sale_pre_order.footer_ext_text1 IS '表尾自定义字段1(文本)';
COMMENT ON COLUMN erp_sale_pre_order.footer_ext_text2 IS '表尾自定义字段2(文本)';

-- ------------------------------------------------------------
-- 2. 单据编号号段（YDHD-yyyyMMdd-NNNN）
--    与 XSDD(销售订单) 同一套 BizNumberGeneratorService 行锁号段，跨进程安全。
-- ------------------------------------------------------------
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id, max_seq)
VALUES ('YDHD', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'YDHD', 4, 1, 999999)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 明细「商品分类」下钻索引（按明细 Tab 左侧商品分类树过滤 join erp_product）
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sale_pre_order_item_product_id ON erp_sale_pre_order_item (product_id);
