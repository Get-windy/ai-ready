-- ============================================================
-- V10.1.0 零售单生产级扩展
-- 对标 Odoo/SAP/金蝶/用友 生产级系统标准
-- 主表扩展：~77个新字段（客户快照/组织/银行税务/金额/预收/信用/积分/POS/自定义等）
-- 明细表扩展：~76个新字段（产品快照/换算/折扣/成本/8价格等级/积分/自定义等）
-- 新增：支付明细表（支持组合支付）
-- ============================================================

-- ═══════════════════════════════════════════════════════════════
-- 1. 零售单主表 erp_retail_order 扩展
-- ═══════════════════════════════════════════════════════════════

-- ── 单据基本信息 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS generation_method VARCHAR(50);
COMMENT ON COLUMN erp_retail_order.generation_method IS '产生方式（手工/订单生成/复制/POS）';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
COMMENT ON COLUMN erp_retail_order.summary IS '摘要';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS source_bill_no VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.source_bill_no IS '来源单据编号';

-- ── 客户快照 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS customer_code VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.customer_code IS '客户编号（快照）';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS customer_level VARCHAR(50);
COMMENT ON COLUMN erp_retail_order.customer_level IS '客户级别（快照）';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS customer_remark VARCHAR(500);
COMMENT ON COLUMN erp_retail_order.customer_remark IS '客户备注（快照）';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS contact_id BIGINT;
COMMENT ON COLUMN erp_retail_order.contact_id IS '联系人ID';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.contact_name IS '联系人姓名';

-- ── 组织信息 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS department_id BIGINT;
COMMENT ON COLUMN erp_retail_order.department_id IS '部门ID';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS department_name VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.department_name IS '部门名称';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS region VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.region IS '区域';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS location VARCHAR(200);
COMMENT ON COLUMN erp_retail_order.location IS '点位';

-- ── 银行/税务 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS bank_name VARCHAR(200);
COMMENT ON COLUMN erp_retail_order.bank_name IS '开户银行';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.bank_account IS '银行账号';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS tax_no VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.tax_no IS '税号';

-- ── 金额扩展 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS other_fee NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.other_fee IS '其他费用';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS rounding_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.rounding_amount IS '抹零金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS total_weight NUMERIC(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.total_weight IS '总重量(kg)';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS total_volume NUMERIC(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.total_volume IS '总体积(m3)';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS total_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.total_quantity IS '总数量';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS return_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.return_quantity IS '退货数量';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS return_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.return_amount IS '退货金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS box_count INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.box_count IS '商品行数/箱数';

-- ── 预收款 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS advance_payment_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.advance_payment_amount IS '使用预收款金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prev_advance_payment NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.prev_advance_payment IS '此前预收';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS used_advance_payment NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.used_advance_payment IS '使用预收款';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS available_advance_payment NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.available_advance_payment IS '可用预收';

-- ── 信用 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS credit_limit NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.credit_limit IS '信用额度';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS available_credit NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.available_credit IS '可用信用额度';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prev_arrears NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.prev_arrears IS '此前欠款';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS current_arrears NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.current_arrears IS '本次欠款';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS arrears_balance NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.arrears_balance IS '欠款余额';

-- ── 结算 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS settlement_method VARCHAR(50);
COMMENT ON COLUMN erp_retail_order.settlement_method IS '结算方式';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS settled_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.settled_amount IS '结算金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS settlement_status VARCHAR(20) DEFAULT 'UNSETTLED';
COMMENT ON COLUMN erp_retail_order.settlement_status IS '结算状态：UNSETTLED/SETTLED/PARTIAL';

-- ── 积分扩展 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS member_generated_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.member_generated_points IS '产生积分';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS member_used_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.member_used_points IS '使用积分';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS member_exchange_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.member_exchange_points IS '兑换积分';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS current_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.current_points IS '当前积分';

-- ── 收款账户（4个） ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS payment_account1 VARCHAR(128);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS payment_account2 VARCHAR(128);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS payment_account3 VARCHAR(128);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS payment_account4 VARCHAR(128);

-- ── 扩展收款方式 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS alipay_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.alipay_amount IS '支付宝支付金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS wechat_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.wechat_amount IS '微信支付金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS aggregate_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.aggregate_amount IS '聚合支付金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS abc_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.abc_amount IS '中国农业银行支付金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ccb_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.ccb_amount IS '中国建设银行支付金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS jd_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.jd_amount IS '京东支付金额';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS total_received NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.total_received IS '收款合计';

-- ── 流程时间/人员 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.bookkeeper_name IS '记账人';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.creator_name IS '制单人';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.auditor_name IS '审核人';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.print_count IS '打印次数';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
COMMENT ON COLUMN erp_retail_order.bookkeeping_time IS '记账时间';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS print_time TIMESTAMP;
COMMENT ON COLUMN erp_retail_order.print_time IS '打印时间';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS approved_time TIMESTAMP;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS completed_by BIGINT;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS completed_time TIMESTAMP;

-- ── 收银员 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS cashier_id BIGINT;
COMMENT ON COLUMN erp_retail_order.cashier_id IS '收银员ID';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS cashier_name VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.cashier_name IS '收银员姓名';

-- ── 备注扩展 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS internal_note VARCHAR(500);
COMMENT ON COLUMN erp_retail_order.internal_note IS '内部备注';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(500);
COMMENT ON COLUMN erp_retail_order.buyer_remark IS '买家备注';

-- ── 表头自定义字段（数字 1-5） ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_num1 NUMERIC(18,2);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_num2 NUMERIC(18,2);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_num3 NUMERIC(18,2);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_num4 NUMERIC(18,2);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_num5 NUMERIC(18,2);

-- ── 表头自定义字段（文本 1-5） ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(200);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(200);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(200);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(200);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(200);

-- ── 表头自定义字段（往来单位/职员/部门） ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_partner BIGINT;
COMMENT ON COLUMN erp_retail_order.ext_partner IS '自定义-往来单位';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_staff BIGINT;
COMMENT ON COLUMN erp_retail_order.ext_staff IS '自定义-职员';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS ext_dept BIGINT;
COMMENT ON COLUMN erp_retail_order.ext_dept IS '自定义-部门';

-- ── 表尾自定义字段 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS footer_ext_text1 VARCHAR(200);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS footer_ext_text2 VARCHAR(200);

-- ── POS专用 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS pos_mode BOOLEAN DEFAULT false;
COMMENT ON COLUMN erp_retail_order.pos_mode IS 'POS模式';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS pos_session_id VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.pos_session_id IS 'POS班次ID';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS pos_terminal VARCHAR(100);
COMMENT ON COLUMN erp_retail_order.pos_terminal IS 'POS终端';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS hold_order_flag BOOLEAN DEFAULT false;
COMMENT ON COLUMN erp_retail_order.hold_order_flag IS '挂单标记';

-- ── 收款码 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS payment_qr_code VARCHAR(500);
COMMENT ON COLUMN erp_retail_order.payment_qr_code IS '收款码';

-- ── 乐观锁 ──
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 0;

-- ── 将id改为BIGINT主键（兼容雪花ID） ──
-- 注意：PostgreSQL的BIGSERIAL本身就是BIGINT，雪花ID可以直接存入

-- ── 状态字段扩展注释更新 ──
COMMENT ON COLUMN erp_retail_order.status IS '状态：0=草稿 1=已完成 2=挂单 3=已作废';

-- ═══════════════════════════════════════════════════════════════
-- 2. 零售单明细表 erp_retail_order_item 扩展
-- ═══════════════════════════════════════════════════════════════

-- ── 行号 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS line_no INTEGER;
COMMENT ON COLUMN erp_retail_order_item.line_no IS '行序号';

-- ── 产品快照 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS product_code VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.product_code IS '货号（快照）';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
COMMENT ON COLUMN erp_retail_order_item.image_url IS '商品图片URL';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS small_unit_barcode VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.small_unit_barcode IS '小单位条码';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS specification VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.specification IS '规格';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS model VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.model IS '型号';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS origin VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.origin IS '产地';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS brand VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.brand IS '品牌';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS product_attribute VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.product_attribute IS '商品行属性';

-- ── 货位/区域 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS location VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.location IS '货位';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS region VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.region IS '区域';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS warehouse_location_id INTEGER;
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS warehouse_location_code VARCHAR(100);

-- ── 数量扩展 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS outbound_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.outbound_quantity IS '出库数量';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS pending_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.pending_quantity IS '待处理数量';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS piece_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.piece_quantity IS '件散数量';

-- ── 单位换算 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS conversion_relation VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.conversion_relation IS '换算关系';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS conversion_result NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.conversion_result IS '换算结果';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(20);
COMMENT ON COLUMN erp_retail_order_item.small_unit IS '小单位';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS small_unit_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.small_unit_quantity IS '小单位数量';

-- ── 价格扩展 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS original_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.original_price IS '折单原价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS small_unit_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.small_unit_price IS '小单位单价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(8,4) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.tax_rate IS '税率';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS line_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.line_amount IS '行金额';

-- ── 折扣 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS discount_rate NUMERIC(8,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.discount_rate IS '折扣(%)';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS discounted_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.discounted_amount IS '折后金额';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS discounted_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.discounted_price IS '折后单价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS favorable_discount_rate NUMERIC(8,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.favorable_discount_rate IS '优惠折扣(%)';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS favorable_unit_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.favorable_unit_price IS '惠后单价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS favorable_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.favorable_amount IS '优惠后金额';

-- ── 成本/毛利 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS cost_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.cost_price IS '参考成本单价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS cost_amount NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.cost_amount IS '参考成本金额';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS gross_profit NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.gross_profit IS '参考毛利';

-- ── 市场价格快照 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS retail_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.retail_price IS '零售价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS wholesale_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.wholesale_price IS '批发价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS min_sale_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.min_sale_price IS '最低售价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS last_sale_price NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.last_sale_price IS '最近售价';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS last_sale_date DATE;
COMMENT ON COLUMN erp_retail_order_item.last_sale_date IS '最近销售日期';

-- ── 8个标准化价格等级 ──
-- 对应：餐饮店/食堂团餐/自助VIP/大团餐/特价客户/外围餐饮店/重点VIP01/连锁VIP
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_restaurant NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_restaurant IS '价格等级-餐饮店';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_canteen NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_canteen IS '价格等级-食堂团餐';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_vip_self NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_vip_self IS '价格等级-自助VIP';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_large_group NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_large_group IS '价格等级-大团餐';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_special_customer NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_special_customer IS '价格等级-特价客户';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_out_restaurant NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_out_restaurant IS '价格等级-外围餐饮店';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_vip_level1 NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_vip_level1 IS '价格等级-重点VIP01';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS price_vip_level2 NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.price_vip_level2 IS '价格等级-连锁VIP';

-- ── 库存扩展 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS available_stock_converted NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.available_stock_converted IS '可用库存换算结果';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS book_stock NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.book_stock IS '账面库存';

-- ── 批次扩展 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS batch_no VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.batch_no IS '批次条码';

-- ── 体积/重量 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS volume NUMERIC(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.volume IS '体积(m3)';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS weight NUMERIC(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.weight IS '重量(kg)';

-- ── 赠品/积分 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS gift BOOLEAN DEFAULT false;
COMMENT ON COLUMN erp_retail_order_item.gift IS '赠品标记';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS gift_item VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.gift_item IS '兑换礼品';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS exchange_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.exchange_points IS '兑换积分';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS used_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.used_points IS '使用积分';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS generated_points NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.generated_points IS '产生积分';

-- ── 箱号 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS box_no VARCHAR(100);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS customer_ticket VARCHAR(200);
COMMENT ON COLUMN erp_retail_order_item.customer_ticket IS '客户一票通';

-- ── 已收数量 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS received_quantity NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.received_quantity IS '已收数量';

-- ── 表体自定义字段（数字 1-7） ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num1 NUMERIC(18,2);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num2 NUMERIC(18,2);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num3 NUMERIC(18,2);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num4 NUMERIC(18,2);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num5 NUMERIC(18,2);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num6 NUMERIC(18,2);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_num7 NUMERIC(18,2);

-- ── 表体自定义字段（文本 1-2） ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(200);
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(200);

-- ── 表体自定义字段（往来单位/职员/部门） ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_partner BIGINT;
COMMENT ON COLUMN erp_retail_order_item.ext_partner IS '自定义-往来单位';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_staff BIGINT;
COMMENT ON COLUMN erp_retail_order_item.ext_staff IS '自定义-职员';
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS ext_dept BIGINT;
COMMENT ON COLUMN erp_retail_order_item.ext_dept IS '自定义-部门';

-- ── 系统字段 ──
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS create_by BIGINT;

-- ═══════════════════════════════════════════════════════════════
-- 3. 新建零售单支付明细表
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS erp_retail_order_payment (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,
    order_id            BIGINT          NOT NULL,
    payment_method      VARCHAR(32)     NOT NULL,
    payment_amount      NUMERIC(18,2)   NOT NULL DEFAULT 0,
    payment_account     VARCHAR(128),
    transaction_no      VARCHAR(128),
    payment_time        TIMESTAMP,
    remark              VARCHAR(255),
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
COMMENT ON TABLE erp_retail_order_payment IS '零售单支付明细表（组合支付时每种支付方式一行）';
COMMENT ON COLUMN erp_retail_order_payment.order_id IS '所属零售单ID';
COMMENT ON COLUMN erp_retail_order_payment.payment_method IS '支付方式：CASH/CARD/PREPAID/TRANSFER/ALIPAY/WECHAT/AGGREGATE/ABC/CCB/JD';
COMMENT ON COLUMN erp_retail_order_payment.payment_amount IS '支付金额';
COMMENT ON COLUMN erp_retail_order_payment.payment_account IS '收款账户';
COMMENT ON COLUMN erp_retail_order_payment.transaction_no IS '交易流水号';

CREATE INDEX IF NOT EXISTS idx_retail_payment_order ON erp_retail_order_payment(order_id);
CREATE INDEX IF NOT EXISTS idx_retail_payment_method ON erp_retail_order_payment(payment_method);

-- ═══════════════════════════════════════════════════════════════
-- 4. 索引优化
-- ═══════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_retail_order_status ON erp_retail_order(status);
CREATE INDEX IF NOT EXISTS idx_retail_order_warehouse ON erp_retail_order(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_retail_order_handler ON erp_retail_order(handler_id);
CREATE INDEX IF NOT EXISTS idx_retail_order_dept ON erp_retail_order(department_id);
CREATE INDEX IF NOT EXISTS idx_retail_order_order_date ON erp_retail_order(order_date);
CREATE INDEX IF NOT EXISTS idx_retail_order_sale_type ON erp_retail_order(sale_type);
CREATE INDEX IF NOT EXISTS idx_retail_order_hold ON erp_retail_order(hold_order_flag) WHERE hold_order_flag = true;

CREATE INDEX IF NOT EXISTS idx_retail_order_item_product ON erp_retail_order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_retail_order_item_barcode ON erp_retail_order_item(barcode);
CREATE INDEX IF NOT EXISTS idx_retail_order_item_line ON erp_retail_order_item(order_id, line_no);
