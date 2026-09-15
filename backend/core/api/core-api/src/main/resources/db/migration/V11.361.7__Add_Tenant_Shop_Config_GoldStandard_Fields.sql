-- ============================================================================
-- V11.361.7 交易模块金标准补齐：tenant_shop_config 追加「配置三页」对标列
-- ============================================================================
-- 目标页面：
--   1) 商城设置 → 基础设置  views/mall/basic-config/index.vue
--   2) 商城设置 → 店铺设置  views/mall/shop-config/index.vue
--   3) 商城设置 → 运费设置  views/mall/freight-config/index.vue
--   三页共用同一张物理表 tenant_shop_config 与同一对端点
--   GET/PUT /api/erp/mall/admin/config（MallAdminController → MallAdminServiceImpl）。
--
-- 列来源核对（⚠️ 本仓库曾因「只看第一个 CREATE TABLE IF NOT EXISTS」而写错列名，
-- 导致整个迁移失败、应用起不来。本次逐个复核了**真正生效**的那次建表）：
--   · tenant_shop_config **全库仅在一处创建**：V6.4.0__Create_B2B_Mall_Tables.sql:6
--     （CREATE TABLE IF NOT EXISTS），建表即 21 列：
--       id, tenant_id, shop_name, shop_logo, shop_desc, theme_color, banner_ids,
--       template_id, payment_methods, enable_register, enable_auto_audit,
--       min_order_amount, free_shipping_amount, freight_amount, status, deleted,
--       create_by, create_time, update_by, update_time
--   · 全库对 tenant_shop_config 再无任何 ALTER TABLE（已 grep 全部 *.sql 确认），
--     故本迁移的 ADD COLUMN IF NOT EXISTS 全部是「新增」，不存在与既有列重名。
--   · 因此本次**不需要**新增 free_shipping_amount / freight_amount（已在库，运费设置复用），
--     也不新增 shop_name / shop_logo / shop_desc / status / enable_register /
--     enable_auto_audit / payment_methods（均已在库）。
--
-- 类型与命名约定：
--   · 布尔开关一律 INTEGER 0/1（遵循项目「status=1 启用」惯例），**不使用 PG BOOLEAN**。
--     实体侧对应 Integer：PG 列是 INTEGER，若实体用 Boolean 则 setBoolean 写入会类型不匹配。
--   · 字符串枚举用 VARCHAR；素材 URL 用 VARCHAR；长文本用 TEXT。
--   · JSON 结构化字段统一用 TEXT（**不用 jsonb**）：
--       理由 ① 与既有 VARCHAR/TEXT 风格一致，实体侧直接映射 String，无需 TypeHandler；
--            ② 三页是「整对象 PUT 一次保存」的模型，JSON 列与该模型天然一致，
--               无需为每个子结构再开 CRUD 端点；
--            ③ 运费设置的物流方式/运费模板**对标字段未实测**（《运费设置开发文档》明确写明
--               「不得视为实测结论」），建独立表等于为未实测结构固化 schema；
--            ④ 对标文档本身也给了「建议独立表 **或 JSON 存储**」两个选项。
--       → 这是**本实现的存储口径**，后续如需按子结构做查询/统计，可平滑迁移为独立表。
--
-- 幂等性：ADD COLUMN IF NOT EXISTS，可重复执行。
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- 一、基础设置页（views/mall/basic-config/index.vue）
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. 基础设置 / 基础信息
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS official_qr_code      VARCHAR(500);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS main_category         VARCHAR(100);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS qualifications        TEXT;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS contacts              TEXT;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS return_consignee      VARCHAR(50);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS return_phone          VARCHAR(30);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS return_address        VARCHAR(500);

-- 2. 基础设置 / 显示设置
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS display_detail_fields TEXT;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS display_list_fields   TEXT;

-- 3. 基础设置 / 图片水印
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS watermark_type        VARCHAR(20) DEFAULT 'none';
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS watermark_image       VARCHAR(500);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS watermark_text        VARCHAR(200);

-- 4. 基础设置 / 访问设置
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS mall_qr_code          VARCHAR(500);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS wechat_link           VARCHAR(500);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS auth_domain           VARCHAR(200);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS default_warehouse_id  BIGINT;

-- 5. 基础设置 / 绑定小程序 + 门店助手 B To B 支付
--    ⚠️ 本期为**明文存储**；「凭据加密 + 脱敏返回」仍未闭环（见基础设置开发文档「剩余缺口」）。
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS miniapp_appid         VARCHAR(100);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS miniapp_appsecret     VARCHAR(200);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS miniapp_pay_mch_id    VARCHAR(100);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS miniapp_pay_mch_key   VARCHAR(200);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS miniapp_mall_url      VARCHAR(500);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS b2b_pay_mch_id        VARCHAR(100);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS b2b_pay_mch_key       VARCHAR(200);

-- 6. 基础设置 / 消息设置（16 项订阅开关，JSON）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS message_subscribe     TEXT;


-- ─────────────────────────────────────────────────────────────────────────────
-- 二、店铺设置页（views/mall/shop-config/index.vue）
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. 店铺参数 / 基础设置
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS shop_enabled          INTEGER DEFAULT 1;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS open_time             VARCHAR(10);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS close_time            VARCHAR(10);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS sms_signature         VARCHAR(50);
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS buyer_hide_level      INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS allow_guest           VARCHAR(20) DEFAULT 'NOT_ALLOW';
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS guest_show_price      VARCHAR(20) DEFAULT 'HIDE';
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS quantity_scale        INTEGER DEFAULT 1;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS wechat_only_login     INTEGER DEFAULT 0;

-- 2. 店铺参数 / 价格设置
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS price_track           INTEGER DEFAULT 0;
-- ⚠️ 页面对应字段为 priceTrackWithUnit（「跟踪价格随单位联动」）；
--    部分旧文档写作 price_track_unit，**以页面源码为准**，此处列名为 price_track_with_unit。
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS price_track_with_unit INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS enable_retail_price   INTEGER DEFAULT 0;

-- 3. 店铺参数 / 商品设置
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS product_auth_manage   INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS enable_split_unit     INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS show_sales            INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS stock_display         VARCHAR(20) DEFAULT 'AVAILABLE';
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS enable_mall_category  INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS out_of_stock_display  VARCHAR(20) DEFAULT 'RESTOCKING';

-- 4. 店铺参数 / 订单设置（「启用」复选框 + N 天自动收货 = 两列）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS auto_receive_enabled  INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS auto_receive_days     INTEGER DEFAULT 7;

-- 5. 注册设置 / 注册协议
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS register_agreement    TEXT;

-- 6. 支付设置 / 支付场景矩阵（在线支付 / 欠款 / 货到付款 的启停 + 排序 + ruleValue，JSON）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS payment_scenes        TEXT;


-- ─────────────────────────────────────────────────────────────────────────────
-- 三、运费设置页（views/mall/freight-config/index.vue）
--    ⚠️ 不含 free_shipping_amount / freight_amount —— 两列 V6.4.0 建表时已在库，本页复用。
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. 物流 / 物流设置
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS self_delivery         INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS enable_logistics      INTEGER DEFAULT 0;

-- 2. 物流 / 物流方式（JSON：[{type,name,enabled}]）
--    ⚠️ 对标「物流」子项数据区因对标系统网络资源请求失败**未实测到字段**，
--       当前结构按开发文档的计价维度结构性补齐，不得视为实测结论。
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS logistics_methods    TEXT;

-- 3. 物流 / 运费模板（JSON：{templateName,chargeType,freeEnabled,freeThreshold,tiers:[{from,to,fee}]}）
--    ⚠️ 同上：对标未实测到字段，结构性补齐。
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS freight_template     TEXT;

-- 4. 到店自提（开关 + 提货地址 JSON：[{contact,phone,address}]）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS enable_pickup         INTEGER DEFAULT 0;
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS pickup_addresses      TEXT;


-- ─────────────────────────────────────────────────────────────────────────────
-- 四、列注释
-- ─────────────────────────────────────────────────────────────────────────────

-- 基础设置页
COMMENT ON COLUMN tenant_shop_config.official_qr_code      IS '【基础设置】公众号二维码图片URL';
COMMENT ON COLUMN tenant_shop_config.main_category         IS '【基础设置】主营类目（对标下拉 10 项，存中文名称）';
COMMENT ON COLUMN tenant_shop_config.qualifications        IS '【基础设置】营业资质 JSON：{"license":"","foodPermit":"","other1".."other4":""}（6 槽位图片URL）';
COMMENT ON COLUMN tenant_shop_config.contacts              IS '【基础设置】联系方式 JSON 数组：[{"type":"微信","number":""}]（类型：微信/手机/QQ/电话）';
COMMENT ON COLUMN tenant_shop_config.return_consignee      IS '【基础设置】退货地址-收件人';
COMMENT ON COLUMN tenant_shop_config.return_phone          IS '【基础设置】退货地址-收件电话';
COMMENT ON COLUMN tenant_shop_config.return_address        IS '【基础设置】退货地址-收件地址';
COMMENT ON COLUMN tenant_shop_config.display_detail_fields IS '【基础设置】商品详情页显示字段 JSON 数组（spec/model/productCode/origin/brand/barcode/shelfLife/produceDate/weight/volume）';
COMMENT ON COLUMN tenant_shop_config.display_list_fields   IS '【基础设置】商品列表页显示字段 JSON 数组（batchNo）';
COMMENT ON COLUMN tenant_shop_config.watermark_type        IS '【基础设置】水印方式：none 不使用 / image 图片水印 / text 文字水印';
COMMENT ON COLUMN tenant_shop_config.watermark_image       IS '【基础设置】图片水印图片URL（watermark_type=image 时有效）';
COMMENT ON COLUMN tenant_shop_config.watermark_text        IS '【基础设置】文字水印文字（watermark_type=text 时有效）';
COMMENT ON COLUMN tenant_shop_config.mall_qr_code          IS '【基础设置】微商城二维码图片URL';
COMMENT ON COLUMN tenant_shop_config.wechat_link           IS '【基础设置】微信版链接地址（页面只读展示）';
COMMENT ON COLUMN tenant_shop_config.auth_domain           IS '【基础设置】微信公众号网页授权域名';
COMMENT ON COLUMN tenant_shop_config.default_warehouse_id  IS '【基础设置】网店仓库（默认仓库ID，关联 erp_warehouse.id）';
COMMENT ON COLUMN tenant_shop_config.miniapp_appid         IS '【基础设置】小程序 appid（⚠️ 当前明文存储，加密+脱敏未闭环）';
COMMENT ON COLUMN tenant_shop_config.miniapp_appsecret     IS '【基础设置】小程序 appsecret（⚠️ 当前明文存储）';
COMMENT ON COLUMN tenant_shop_config.miniapp_pay_mch_id    IS '【基础设置】小程序支付商户号（⚠️ 当前明文存储）';
COMMENT ON COLUMN tenant_shop_config.miniapp_pay_mch_key   IS '【基础设置】小程序支付商户密钥（⚠️ 当前明文存储）';
COMMENT ON COLUMN tenant_shop_config.miniapp_mall_url      IS '【基础设置】小程序商城地址（页面只读展示）';
COMMENT ON COLUMN tenant_shop_config.b2b_pay_mch_id        IS '【基础设置】门店助手B To B支付-支付商户号（⚠️ 当前明文存储）';
COMMENT ON COLUMN tenant_shop_config.b2b_pay_mch_key       IS '【基础设置】门店助手B To B支付-支付商户密钥（⚠️ 当前明文存储）';
COMMENT ON COLUMN tenant_shop_config.message_subscribe     IS '【基础设置】消息设置 JSON：16 项订阅开关 {registerSuccess:true, ...}';

-- 店铺设置页
COMMENT ON COLUMN tenant_shop_config.shop_enabled          IS '【店铺设置】商城开关 1=开 0=关（注意：与既有的 status 语义并存，status 为商城状态全局位）';
COMMENT ON COLUMN tenant_shop_config.open_time             IS '【店铺设置】营业时间-起（HH:mm）';
COMMENT ON COLUMN tenant_shop_config.close_time            IS '【店铺设置】营业时间-止（HH:mm）';
COMMENT ON COLUMN tenant_shop_config.sms_signature         IS '【店铺设置】短信签名（页面必填）';
COMMENT ON COLUMN tenant_shop_config.buyer_hide_level      IS '【店铺设置】启用买家不显示客户级别 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.allow_guest           IS '【店铺设置】是否允许游客访问：NOT_ALLOW 不允许 / ALLOW 允许';
COMMENT ON COLUMN tenant_shop_config.guest_show_price      IS '【店铺设置】游客显示价格：HIDE 不显示 / SHOW 显示';
COMMENT ON COLUMN tenant_shop_config.quantity_scale        IS '【店铺设置】商城数量小数位数 0-3';
COMMENT ON COLUMN tenant_shop_config.wechat_only_login     IS '【店铺设置】只允许微信登录 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.price_track           IS '【店铺设置】价格跟踪 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.price_track_with_unit IS '【店铺设置】跟踪价格随单位联动 1=是 0=否（页面对应 priceTrackWithUnit）';
COMMENT ON COLUMN tenant_shop_config.enable_retail_price   IS '【店铺设置】启用建议零售价 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.product_auth_manage   IS '【店铺设置】商品授权管理 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.enable_split_unit     IS '【店铺设置】启用分单位显示 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.show_sales            IS '【店铺设置】启用商城显示销量 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.stock_display         IS '【店铺设置】库存显示方式：AVAILABLE 显示可用库存 / QUANTITY 显示库存数量 / HIDE 不显示库存';
COMMENT ON COLUMN tenant_shop_config.enable_mall_category  IS '【店铺设置】启用商城分类 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.out_of_stock_display  IS '【店铺设置】无货商品显示方式：RESTOCKING 显示补货中 / HIDE 不显示商品';
COMMENT ON COLUMN tenant_shop_config.auto_receive_enabled  IS '【店铺设置】启用 N 天自动收货 1=是 0=否（页面对应 autoReceiveEnabled）';
COMMENT ON COLUMN tenant_shop_config.auto_receive_days     IS '【店铺设置】N 天自动收货天数（1-90）';
COMMENT ON COLUMN tenant_shop_config.register_agreement    IS '【店铺设置】注册协议内容（对标该子项字段标签缺失，本列为本系统实现口径）';
COMMENT ON COLUMN tenant_shop_config.payment_scenes        IS '【店铺设置】支付场景矩阵 JSON 数组：[{code:ONLINE|CREDIT|COD,enabled,ruleEnabled,ruleValue,sort}] —— 本实现存储口径；在线支付渠道明细仍由 payment_methods 承载';

-- 运费设置页
COMMENT ON COLUMN tenant_shop_config.self_delivery         IS '【运费设置】自有配送 1=开 0=关';
COMMENT ON COLUMN tenant_shop_config.enable_logistics      IS '【运费设置】启用物流 1=开 0=关';
COMMENT ON COLUMN tenant_shop_config.logistics_methods     IS '【运费设置】物流方式 JSON 数组：[{type:CITY|EXPRESS|SELF,name,enabled}]（⚠️ 对标字段未实测，结构为本实现口径）';
COMMENT ON COLUMN tenant_shop_config.freight_template      IS '【运费设置】运费模板 JSON：{templateName,chargeType:WEIGHT|PIECE|AMOUNT,freeEnabled,freeThreshold,tiers:[{from,to,fee}]}（⚠️ 对标字段未实测）';
COMMENT ON COLUMN tenant_shop_config.enable_pickup         IS '【运费设置】到店自提 1=开 0=关';
COMMENT ON COLUMN tenant_shop_config.pickup_addresses      IS '【运费设置】提货地址 JSON 数组：[{contact,phone,address}]';
