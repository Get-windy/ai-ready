-- ============================================================================
-- 销售换货单金标准增强（P0 差距修复）
--
-- 背景：
--   erp_sale_exchange / erp_sale_exchange_item 两张表长期停留在初版结构
--   （手工脚本 sql/V20260717001__extend_sale_exchange_tables.sql 从未被 Flyway 收录），
--   而 SaleExchange / SaleExchangeItem 实体已声明完整字段。
--   MyBatis-Plus 按实体生成 SELECT 语句 → 缺列直接 BadSqlGrammar，
--   列表永远 0 行、单据无法落库（数据不闭环）。
--
-- 本迁移按实体全字段补齐两表结构（与手工脚本一致，另补：
--   主表 create_by/update_by 审计列、明细表 quantity 业务数量列）。
-- 全部 ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS，可重复执行。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 主表 erp_sale_exchange
-- ------------------------------------------------------------
-- 单据基本信息
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS sales_type varchar(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS settle_status varchar(20);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS print_count integer DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS attachment text;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS summary varchar(500);

-- 客户快照（来源: erp_customer / biz_party）
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS customer_code varchar(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS customer_level varchar(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS contact_name varchar(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS contact_phone varchar(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS contact_address varchar(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bank_name varchar(200);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bank_account varchar(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS tax_no varchar(100);

-- 仓库快照（来源: erp_warehouse）：换入 / 换出双仓
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS in_warehouse_id bigint;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS in_warehouse_name varchar(200);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS out_warehouse_id bigint;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS out_warehouse_name varchar(200);

-- 职员/部门快照（来源: sys_user / sys_dept）
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS handler_id bigint;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS handler_name varchar(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS dept_id bigint;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS dept_name varchar(200);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bookkeeper_name varchar(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bookkeeping_time timestamp;

-- 收款/信用
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS payment_account varchar(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS more_accounts varchar(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS received_amount numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS prev_advance numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS use_advance numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS available_advance numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS advance_balance numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS receivable_increase numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS credit_limit numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS available_credit numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS prev_debt numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS current_debt numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS debt_balance numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS collection_deadline varchar(50);

-- 金额计算链 + 结算
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS product_amount numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS discount_amount numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS settled_amount numeric(18,2) DEFAULT 0;

-- 数量/物理属性汇总
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS in_quantity_total numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS out_quantity_total numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS total_weight numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS total_volume numeric(18,4) DEFAULT 0;

-- 表头自定义字段
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num1 numeric(18,4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num2 numeric(18,4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num3 numeric(18,4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num4 numeric(18,4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num5 numeric(18,4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text1 varchar(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text2 varchar(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text3 varchar(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text4 varchar(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text5 varchar(500);

-- 标准审计列（MyBatis-Plus 实体 createBy/updateBy 映射，缺列会导致 SELECT 报错）
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS create_by bigint;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS update_by bigint;

-- ------------------------------------------------------------
-- 2. 明细表 erp_sale_exchange_item
-- ------------------------------------------------------------
-- 仓库类型标记（1=换入, 2=换出）
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS warehouse_type integer DEFAULT 1;

-- 商品快照
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS barcode varchar(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS specification varchar(200);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS model varchar(200);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS origin varchar(200);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS brand varchar(200);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS product_line_attr varchar(50);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS image varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS location varchar(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS area varchar(100);

-- 库存信息
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS available_stock numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS stock_converted numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS book_stock numeric(18,4) DEFAULT 0;

-- 批次和日期
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS batch_barcode varchar(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS production_date date;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS shelf_life integer;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS expiry_date date;

-- 数量与包装（quantity 为业务数量列，区别于初版的 exchange_quantity）
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS quantity numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS conversion_rate numeric(18,4) DEFAULT 1;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS piece_scatter_qty numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS large_package numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS medium_package numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_package numeric(18,4) DEFAULT 0;

-- 价格信息
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS recent_sale_date date;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS recent_sale_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS retail_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS wholesale_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS min_sale_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS unit_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS amount numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_unit varchar(50);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_unit_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_unit_qty numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS cost_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS cost_amount numeric(18,2) DEFAULT 0;

-- 折扣信息
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS discount numeric(5,2) DEFAULT 100;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS discount_price numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS discount_amount numeric(18,2) DEFAULT 0;

-- 物理属性
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS volume numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS weight numeric(18,4) DEFAULT 0;

-- 业务标记
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS is_gift boolean DEFAULT false;

-- 价格等级（8个，对标：餐饮店/食堂团餐/外围餐饮店/自助vip/大团餐/重点|vip01/连锁|vip/特价客户）
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level1 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level2 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level3 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level4 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level5 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level6 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level7 numeric(18,2) DEFAULT 0;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level8 numeric(18,2) DEFAULT 0;

-- 明细自定义字段
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num1 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num2 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num3 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num4 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num5 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num6 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num7 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num8 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num9 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num10 numeric(18,4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text1 varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text2 varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text3 varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text4 varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text5 varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text6 varchar(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_partner bigint;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_staff bigint;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_dept bigint;

-- ------------------------------------------------------------
-- 3. 索引
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_exchange_customer ON erp_sale_exchange(customer_id);
CREATE INDEX IF NOT EXISTS idx_exchange_date ON erp_sale_exchange(exchange_date);
CREATE INDEX IF NOT EXISTS idx_exchange_status ON erp_sale_exchange(status);
CREATE INDEX IF NOT EXISTS idx_exchange_in_warehouse ON erp_sale_exchange(in_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_exchange_out_warehouse ON erp_sale_exchange(out_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_exchange_no ON erp_sale_exchange(exchange_no);

CREATE INDEX IF NOT EXISTS idx_exchange_item_exchange ON erp_sale_exchange_item(exchange_id);
CREATE INDEX IF NOT EXISTS idx_exchange_item_product ON erp_sale_exchange_item(product_id);
CREATE INDEX IF NOT EXISTS idx_exchange_item_warehouse_type ON erp_sale_exchange_item(warehouse_type);
