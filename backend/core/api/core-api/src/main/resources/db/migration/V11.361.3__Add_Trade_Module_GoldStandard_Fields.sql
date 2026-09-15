-- ============================================================================
-- V11.361.3  交易模块（商城）后端字段补齐 —— 金标准页面列因后端无字段显示 '-'
--
-- 背景：前端交易模块多页已升级到金标准，但若干列因后端缺字段/缺映射显示 '-'：
--   1) 商城 → 订单处理（views/mall/order-process/index.vue；按单据 39 列 / 按明细 47 列）
--      接口 /erp/mall/admin/order/page 直接返回 Page<ErpSaleOrderMall>（erp_sale_order 表映射），
--      商城视图映射类缺列 → JSON 无该 key → 页面显示 '-'。
--      对标依据：docs/Yh-Spec/手动整理对标开发文档/交易模块/订单处理开发文档.md
--   2) 商城 → 买家申请管理（views/mall/buyer-apply/index.vue）
--      商城 → 买家账号（views/mall/buyer-account/index.vue）
--   3) 商城 → 商城设置 → 公告设置（views/mall/notice-config/index.vue）
--   4) 商城 → 基础业务 → 单位显示（views/mall/unit-display/index.vue）
--
-- 口径（重要）：erp_sale_order 中**已存在**的语义同名字段一律复用，不重复加列：
--   运费      → 复用 shipping_fee（SaleOrder.shippingFee）
--   运单号    → 复用 waybill_no  （SaleOrder.waybillNo）
--   商品数量  → 复用 total_quantity（SaleOrder.totalQuantity）
--   经手人    → 复用 salesman_name（SaleOrder.salesmanName）
--   表头自定义字段1~5 → 复用 ext_num1/ext_num2/ext_text3/ext_text4/ext_text5
--   运费承担方/配送方式/物流公司/代收金额/预计发货/已发数量/未发数量/仓库名称/
--   推广人/打印次数/已结金额/审核时间 → 复用 freight_payer/delivery_method/logistics_company/
--   cod_amount/expected_ship_time/shipped_quantity/unshipped_quantity/warehouse_name/
--   promoter_name/print_count/settled_amount/audit_time
--   对应商城视图映射字段在 ErpSaleOrderMall 中以 @TableField("<既有列>") 暴露为前端所需 JSON key。
-- ============================================================================

-- ── 1) 商城订单 erp_sale_order：仅补库中确实不存在的 3 列 ─────────────────────
-- 强制终止（前端 按单据列 34「强制终止」）
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS force_stop INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_sale_order.force_stop IS '强制终止: 0=否 1=是（商城订单处理页「强制终止」列）';

-- 记账状态（前端 按单据列 30「记账状态」）
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS bookkeeping_status INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_sale_order.bookkeeping_status IS '记账状态: 0=未记账 1=已记账（商城订单处理页「记账状态」列）';

-- 是否使用优惠券（前端 按单据列 32「是否使用优惠券」；优惠金额列 coupon_amount 已存在）
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS coupon_used INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_sale_order.coupon_used IS '是否使用优惠券: 0=否 1=是（商城订单处理页「是否使用优惠券」列）';

-- ── 2) 商城用户 shop_user：买家申请管理 / 买家账号 页所需列 ───────────────────
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS contact_name         VARCHAR(100);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS address              VARCHAR(500);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS remark               VARCHAR(500);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS business_license     VARCHAR(500);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS qq                   VARCHAR(50);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS wechat               VARCHAR(100);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS category_id          BIGINT;
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS default_handler_id   BIGINT;
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS default_handler_name VARCHAR(100);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS customer_level       VARCHAR(100);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS warehouse_id         BIGINT;
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS warehouse_name       VARCHAR(200);
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS dept_id              BIGINT;
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS dept_name            VARCHAR(200);

COMMENT ON COLUMN shop_user.contact_name         IS '联系人姓名（买家申请管理「联系人姓名」列）';
COMMENT ON COLUMN shop_user.address              IS '地址（买家申请管理「地址」列）';
COMMENT ON COLUMN shop_user.remark               IS '备注（买家申请管理「备注」列）';
COMMENT ON COLUMN shop_user.business_license     IS '营业执照图片URL（买家申请管理「营业执照」列）';
COMMENT ON COLUMN shop_user.qq                   IS 'QQ（买家申请管理「qq」列）';
COMMENT ON COLUMN shop_user.wechat               IS '微信（买家申请管理「微信」列）';
COMMENT ON COLUMN shop_user.category_id          IS '归属分类ID（biz_party_category.id，party_type=CUSTOMER）';
COMMENT ON COLUMN shop_user.default_handler_id   IS '默认经手人ID（sys_user.id）';
COMMENT ON COLUMN shop_user.default_handler_name IS '默认经手人姓名（买家账号「默认经手人」列）';
COMMENT ON COLUMN shop_user.customer_level       IS '客户级别（买家账号「客户级别」列）';
COMMENT ON COLUMN shop_user.warehouse_id         IS '所属仓库ID（erp_warehouse.id）';
COMMENT ON COLUMN shop_user.warehouse_name       IS '所属仓库名称（买家账号「所属仓库」列）';
COMMENT ON COLUMN shop_user.dept_id              IS '所属部门ID（sys_dept.id）';
COMMENT ON COLUMN shop_user.dept_name            IS '所属部门名称（买家账号「所属部门」列）';

CREATE INDEX IF NOT EXISTS idx_shop_user_category ON shop_user (category_id);

-- ── 3) 商城公告 mall_notice：补「发布人」（publish_time 已存在，不重复加）──────
ALTER TABLE mall_notice ADD COLUMN IF NOT EXISTS publisher VARCHAR(100);
COMMENT ON COLUMN mall_notice.publisher IS '发布人（公告设置「发布人」列）';

-- ── 4) 商品 erp_product：补「单位显示」开关 ──────────────────────────────────
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS unit_display INTEGER DEFAULT 1;
COMMENT ON COLUMN erp_product.unit_display IS '单位显示开关: 0=隐藏 1=显示（商城「单位显示」页）';
