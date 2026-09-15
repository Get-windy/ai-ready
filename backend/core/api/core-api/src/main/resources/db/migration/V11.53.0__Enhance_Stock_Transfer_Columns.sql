-- 调拨单：补对标"调拨单"文档的列表页/表单页字段（按单据23列 + 按明细50列）

-- ══════════ 主表 erp_stock_transfer ══════════
-- 来源订单（按单据列4）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS source_bill_no character varying(100);
-- 部门
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS department_id bigint;
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS department_name character varying(100);
-- 摘要（按单据列17 / 表单字段）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS summary character varying(500);
-- 附件（按单据列18）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS attachment integer DEFAULT 0;
-- 成本金额（按单据列11）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS total_cost_amount numeric(18,2) DEFAULT 0;
-- 调拨差额（按单据列13）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS total_transfer_diff numeric(18,2) DEFAULT 0;
-- 重量/体积（按单据列14/15）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS total_weight numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS total_volume numeric(18,4) DEFAULT 0;
-- 记账人/记账时间（按单据列19/21）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS poster_name character varying(100);
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS poster_time timestamp;
-- 打印次数（按单据列23 / 表单字段）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS print_count integer DEFAULT 0;
-- 制单人（按单据列20，冗余存名便于列表检索）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS create_by_name character varying(100);
-- 经手人（按单据列8，沿用 applicant_id 作经手人，另存名字段便于检索）
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS handler_name character varying(100);
-- 调拨方式(同价/异价) 沿用 transfer_type，此处仅保证索引
CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_bill_date ON public.erp_stock_transfer(bill_date);
CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_status ON public.erp_stock_transfer(status);

-- ══════════ 明细表 erp_stock_transfer_item ══════════
-- 图片（表单明细列1）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS image character varying(500);
-- 型号/产地/品牌/区域（表单明细列4/16/17/10）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS model character varying(100);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS origin character varying(100);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS brand character varying(100);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS region character varying(100);
-- 出库货位/入库货位（表单明细列11/12）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS location_out character varying(100);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS location_in character varying(100);
-- 可用库存/换算结果/账面库存
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS available_stock numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS available_stock_converted numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS book_stock numeric(18,4) DEFAULT 0;
-- 成本金额（表单明细列20 / 按明细列36）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS cost_amount numeric(18,2) DEFAULT 0;
-- 小单位/小单位单价/小单位数量（表单明细列33/34/35）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS small_unit character varying(50);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS small_unit_price numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS small_unit_quantity numeric(18,4) DEFAULT 0;
-- 换算结果（表单明细列14 / 按明细列28）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS conversion_result numeric(18,4) DEFAULT 0;
-- 重量/体积（按明细列40/41）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS weight numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS volume numeric(18,4) DEFAULT 0;
-- 零售价/批发价（表单明细列27/28）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS retail_price numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS wholesale_price numeric(18,4) DEFAULT 0;
-- 表体自定义1~3(数字) / 4~5(文本)（按明细列18~22）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS ext_num1 numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS ext_num2 numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS ext_num3 numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS ext_text1 character varying(200);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS ext_text2 character varying(200);
-- 单据自定义1~3(数字) / 4~5(文本)（表单明细列42~46）
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS doc_custom1 numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS doc_custom2 numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS doc_custom3 numeric(18,4) DEFAULT 0;
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS doc_custom4 character varying(200);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS doc_custom5 character varying(200);
-- 明细备注（沿用 remark）
CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_item_product ON public.erp_stock_transfer_item(product_id);

-- ═══════════════════════════════════════════════════════════════════
-- 调拨单(5011) 菜单双入口对齐：主菜单→新增表单，[历史]→列表
-- 与 其他入库单 5002 / 其他出库单 5001 保持一致
-- ═══════════════════════════════════════════════════════════════════
UPDATE sys_menu SET
  menu_name = '调拨单',
  path = 'erp/stock-transfer/form',
  component = 'erp/stock-transfer/form',
  list_path = 'erp/stock-transfer/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5011 OR menu_code = 'erp:stock-transfer';
