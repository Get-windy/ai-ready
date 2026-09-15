-- ============================================================================
-- V11.368.0 商城装修 · 分类页装修：按对标**实测**校正「商品默认排序」值域与列默认值
-- ============================================================================
-- 页面：交易模块 → 商城设置 → 商城装修 → Tab三「分类页装修」
--       frontend/apps/pc-admin/src/views/mall/shop-decoration/index.vue
--
-- ── 本轮实测依据（2026-09-14，非推测）──────────────────────────────────────
-- 来源一：ql361 自己的设置读取接口响应（登录后自动拉取）
--   tool-results/ql361/pages/_api_CC.ERP.BLL.DH.Baseinfo.B2B2CSettings.GetSettings.json
--   其内嵌的 **snake_case 设置字典（602 键）** 即真实落库键与当前值，其中：
--     · mall_product_categorytype  = "0"    → 分类展示方式（0 = 按目录分类）
--     · mall_goodsorder            = "9"    → 商品默认排序（9 = 综合排序）
--     · mall_categorypage_model    = "1"    → 分类页分类样式（1 = 纯文本模式）
--         卫星键佐证：mall_categorypage_model_url     = "desktop/images/mallcommon/textmodel.png"
--                     mall_categorypage_model_explain = "提示：纯文本模式将不会展示分类图片"
-- 来源二：ql361 前端常量表（tool-results/ql361/dl2-ConstData.js，js-widget.js 内为等价副本）
--   mallgoodsorder:[{label:"按货号升序",value:"1"},{label:"按货号降序",value:"3"},
--                   {label:"按销量降序",value:"5"},{label:"按商品名称",value:"7"},
--                   {label:"综合排序",value:"9"}]
--   ⇒ 对标「商品默认排序」**只有这 5 项**，默认「综合排序」= **9**。
-- 汇总裁决：docs/Yh-Spec/抓取结果/商城装修_存储键_补抓20260914.json
--
-- ── 为什么要改（这是一个真实缺陷，不是口径洁癖）──────────────────────────
-- V11.366.0 建列时把 `category_default_sort` 的默认值写成 `'COMPOSITE'`，前端当时用的是
-- **自造**值域 {COMPOSITE, SALES, PRICE_ASC, PRICE_DESC, NEWEST} —— 其中
-- 「销量排序 / 价格升序 / 价格降序 / 上新时间」**在标的中根本不存在**，
-- 而对标真实存在的「按货号升序 / 按货号降序 / 按商品名称」在我方**缺失**。
-- 属于「自造值域混进对标项」，按实测必须改正。
--
-- ── 处置 ───────────────────────────────────────────────────────────────────
--   ① 前端 SORT_OPTIONS 改为对标完整值域，且**直接沿用对标编码** 1/3/5/7/9，默认 '9'；
--   ② 本迁移把库内历史值 'COMPOSITE' 归一为 '9'（语义都是「综合排序」，等价改写、不丢语义）；
--   ③ 列默认值由 'COMPOSITE' 改为 '9'；
--   ④ 更新注释，把对标真实存储键写进列注释，避免后人再当成「未实测」而自造。
--
-- ⚠️ 另外两项**不改编码**（如实说明）：`category_display_mode` 仍是 CATALOG/BRAND、
--    `category_style` 仍是 TEXT/IMAGE。原因：对标「按品牌分类」「图文模式」对应的编码
--    **本轮未取到**（该 JS 分片未下载、内层字典只有当前值），不猜。其值与对标语义对应关系为
--    CATALOG ↔ 对标 0、TEXT ↔ 对标 1（已由实测当前值证实）。
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- 一、历史值归一：'COMPOSITE' → '9'（同一语义「综合排序」，对标编码为 9）
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE tenant_shop_config
   SET category_default_sort = '9'
 WHERE category_default_sort = 'COMPOSITE';


-- ─────────────────────────────────────────────────────────────────────────────
-- 二、列默认值改为对标默认（综合排序 = 9）
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE tenant_shop_config ALTER COLUMN category_default_sort SET DEFAULT '9';


-- ─────────────────────────────────────────────────────────────────────────────
-- 三、列注释：写清对标真实存储键 / 实测值域 / 本实现的取舍
-- ─────────────────────────────────────────────────────────────────────────────

COMMENT ON COLUMN tenant_shop_config.category_display_mode IS
'【分类页装修·分类展示方式】本实现编码 CATALOG=按目录分类 / BRAND=按品牌分类。对标真实存储键 = 设置字典 `mall_product_categorytype`（实测当前值 "0" = 按目录分类）；对标「按品牌分类」的编码未实测，故本列不沿用对标码。语义对应：CATALOG ↔ 对标 0。证据 docs/Yh-Spec/抓取结果/商城装修_存储键_补抓20260914.json';

COMMENT ON COLUMN tenant_shop_config.category_default_sort IS
'【分类页装修·商品默认排序】✅ 值域已完整实测，**沿用对标编码**（对标存储键 = 设置字典 `mall_goodsorder`）：1=按货号升序 / 3=按货号降序 / 5=按销量降序 / 7=按商品名称 / 9=综合排序（默认）。V11.368.0 起默认值 = 9，历史值 COMPOSITE 已归一为 9。⚠️ 旧的自造项（销量排序/价格升序/价格降序/上新时间）对标不存在，已删除';

COMMENT ON COLUMN tenant_shop_config.category_style IS
'【分类页装修·分类页分类样式】本实现编码 TEXT=纯文本模式 / IMAGE=图文模式。对标真实存储键 = 设置字典 `mall_categorypage_model`（实测当前值 "1" = 纯文本模式；卫星键 `..._url=desktop/images/mallcommon/textmodel.png`、`..._explain=提示：纯文本模式将不会展示分类图片` 佐证）；对标「图文模式」的编码未实测，故本列不沿用对标码。语义对应：TEXT ↔ 对标 1。';

COMMENT ON COLUMN tenant_shop_config.category_show_count IS
'【分类页装修·显示分类下商品数量】本实现编码 1=勾选 / 0=不勾选（对标界面默认勾选）。⚠️ 对标该项的**真实存储键本轮未取到**（内层设置字典无 mall_showcategoryqty 之类键；PascalCase 的 data.MallShowcategoryqty 属另一层原始设置），如实留缺口，不猜。';
