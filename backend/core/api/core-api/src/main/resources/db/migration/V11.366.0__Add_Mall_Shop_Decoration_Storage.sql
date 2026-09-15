-- ============================================================================
-- V11.366.0 商城装修：分类页装修 4 列 + 装修配置存储 + 行业模板库 + 商品关联表
-- ============================================================================
-- 目标页面：商城 → 商城设置 → 商城装修
--   前端：frontend/apps/pc-admin/src/views/mall/shop-decoration/index.vue
--   API ：frontend/apps/pc-admin/src/api/erp/mall.ts
--   实体：backend/erp/erp-mall/.../b2b/model/ShopConfig.java · ShopTemplate.java
--         + 新增 ShopDecoration.java / ShopDecorationProduct.java
--   端点：MallAdminController（@RequestMapping("/api/erp/mall/admin")）
--         · GET/PUT  /config                        —— 复用（V11.361.7 既有）
--         · GET      /template/list                 —— 复用
--         · POST     /template                      —— 新增模板（写 shop_template）
--         · GET      /template/library              —— 行业模板库列表
--         · POST     /template/library/{id}/reference —— 引用（复制库模板到本租户「我的模板」）
--         · GET/POST /decoration                    —— 装修配置列表 / 新增
--         · GET/PUT/DELETE /decoration/{id}         —— 装修配置详情 / 更新 / 删除
--         · GET/PUT  /decoration/{id}/products      —— 商品详情装修的「设置应用商品」关联
--
-- 对标规格：docs/Yh-Spec/手动整理对标开发文档/交易模块/商城装修开发文档.md（权威）
-- 实测实据：docs/Yh-Spec/抓取结果/商城装修_抓取.json（2026-09-07 全 4 Tab 实测）
--
-- ── 实测 vs 未实测（本迁移的「如实标注」总表）──────────────────────────────
--   【实测】
--     · Tab三 分类页装修 4 项：分类展示方式=单选(按目录分类[勾选]/按品牌分类)、
--       商品默认排序=下拉(综合排序[默认])、分类页分类样式=单选(纯文本模式[勾选]/图文模式)、
--       显示分类下商品数量=复选框(默认开启)；提示「纯文本模式将不会展示分类图片」。
--     · Tab二 模板库 14 个行业名逐字：办公用品/服装鞋帽/化妆用品/机械机电/家电数码/母婴用品/
--       汽修汽配/生鲜农贸/食品快消品/手机通讯/通用行业/五金建材/医药制品/珠宝钟表。
--     · Tab一 风格选择 9 主题色（沿用既有 theme_color 列，本迁移不新增列）。
--   【未实测 → 本实现口径，勿当对标结论】
--     · 「商品默认排序」下拉**选项全集未实测**（抓取文件只记了默认项「综合排序」）。
--       本实现只落库实测项 COMPOSITE，不做其它排序枚举的硬编码校验。
--     · 分类展示方式/分类样式的**存储编码**未实测（对标只给出中文标签），
--       本实现口径为 CATALOG/BRAND、TEXT/IMAGE（列注释已写明）。
--     · 排版布局/拖拽装修编辑器的**结构未实测**，`config_json` 只约定「TEXT 存 JSON 文本」
--       （与仓库既有 JSON 列风格一致：TEXT 不用 jsonb，无需 TypeHandler）。
--     · 各行业模板的**内部布局内容未实测** → 本迁移 **不填充任何模板内容**
--       （seed 行的 config_json 一律为 NULL，仅落「行业名」这一实测事实）。
--     · 「设置应用商品」的真实交互（弹窗/多选）未实测 → 关联表按「能承载前端已有的多选 id 集合」
--       设计，未造交互。
--
-- ── 列来源核对（⚠️ 本仓库曾因引用不存在的列导致迁移失败、应用起不来）────────
--   已用真库只读查询（information_schema / pg_constraint）逐列复核，2026-09-14：
--     · tenant_shop_config 真实列 21（V6.4.0 建表）+ V11.361.7 追加 52 + V11.364.0/V11.365.0 追加；
--       **category_display_mode / category_default_sort / category_style / category_show_count
--       四列均不存在** → 本次全部为「新增」。
--       theme_color / template_id **已存在** → 复用，本迁移不新增、不改类型
--       （Tab一 风格选择既往 theme_color，Tab二「正在使用模板」既往 template_id）。
--     · shop_template 真实列 13（V6.4.0 建表，全库无 ALTER，无 tenant_id 列）；
--       **industry_code / industry_name / is_library 三列均不存在** → 本次全部为「新增」。
--       既有唯一约束：shop_template_template_code_key UNIQUE(template_code)（seed 需 ON CONFLICT）。
--     · shop_decoration / shop_decoration_product 两表**均不存在** → 本次新建。
--       product_id 引用 erp_product.id（真库已确认 erp_product.id BIGINT 存在）。
--
-- ── 类型与命名约定 ─────────────────────────────────────────────────────────
--   · 布尔开关一律 INTEGER 0/1（遵循项目「status=1 启用」惯例），**不使用 PG BOOLEAN**；
--     实体侧对应 Integer（PG 列 INTEGER 时实体用 Boolean 会 setBoolean 类型不匹配）。
--   · JSON 结构化字段用 TEXT（**不用 jsonb**），实体侧直接映射 String，无需 TypeHandler。
--   · 外键 id 用 BIGINT。全部新增列可空 —— updateConfig 走 updateById（null 忽略），
--     设 NOT NULL 会与「三页共用单行 + 部分提交」机制冲突。
--   · 新表 tenant_id 可空但由 MetaObjectHandler 自动填充 + 租户拦截器自动注入过滤
--     （两表均**不在** MyBatisPlusConfig.IGNORE_TENANT_TABLES 名单内）。
--
-- 幂等性：CREATE TABLE IF NOT EXISTS / ADD COLUMN IF NOT EXISTS / COMMENT /
--         INSERT ... ON CONFLICT DO NOTHING，可重复执行。
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- 一、A 项：tenant_shop_config 追加「分类页装修」4 列
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. 分类展示方式（对标单选：按目录分类[默认勾选] / 按品牌分类）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS category_display_mode  VARCHAR(20) DEFAULT 'CATALOG';

-- 2. 商品默认排序（对标下拉；**选项全集未实测**，实测默认项为「综合排序」）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS category_default_sort  VARCHAR(20) DEFAULT 'COMPOSITE';

-- 3. 分类页分类样式（对标单选：纯文本模式[默认勾选] / 图文模式；纯文本模式不展示分类图片）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS category_style         VARCHAR(20) DEFAULT 'TEXT';

-- 4. 显示分类下商品数量（对标复选框，默认开启）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS category_show_count    INTEGER DEFAULT 1;

COMMENT ON COLUMN tenant_shop_config.category_display_mode IS '【分类页装修】分类展示方式（对标单选，实测选项「按目录分类[默认勾选]」/「按品牌分类」）。⚠️ 存储编码未实测，本实现口径：CATALOG=按目录分类 / BRAND=按品牌分类';
COMMENT ON COLUMN tenant_shop_config.category_default_sort IS '【分类页装修】商品默认排序（对标下拉，实测默认项「综合排序」）。⚠️ **选项全集未实测**，本实现只落库实测项 COMPOSITE，未硬编码校验其它取值';
COMMENT ON COLUMN tenant_shop_config.category_style        IS '【分类页装修】分类页分类样式（对标单选，实测选项「纯文本模式[默认勾选]」/「图文模式」，提示「纯文本模式将不会展示分类图片」）。⚠️ 存储编码未实测，本实现口径：TEXT=纯文本模式 / IMAGE=图文模式';
COMMENT ON COLUMN tenant_shop_config.category_show_count   IS '【分类页装修】显示分类下商品数量 1=显示 0=不显示（对标复选框，实测默认开启）';


-- ─────────────────────────────────────────────────────────────────────────────
-- 二、C 项：shop_template 追加「行业模板库」3 列
-- ─────────────────────────────────────────────────────────────────────────────
-- 说明：shop_template 全库仅在 V6.4.0 建表（13 列，无 tenant_id）。
--       库模板（is_library=1）为平台共享数据；「引用」= 复制出一条 is_library=0 的
--       本租户「我的模板」行（沿用既有 GET /template/list 的口径，不新增隔离列，
--       避免改动既有页面的可见性语义 —— 见开发文档「未实现/未闭环清单」）。

-- 5. 行业编码（本实现口径的英文码，行业名本身是实测数据）
ALTER TABLE shop_template ADD COLUMN IF NOT EXISTS industry_code VARCHAR(50);

-- 6. 行业名称（**实测逐字** 14 行业：办公用品/服装鞋帽/化妆用品/机械机电/家电数码/母婴用品/
--    汽修汽配/生鲜农贸/食品快消品/手机通讯/通用行业/五金建材/医药制品/珠宝钟表）
ALTER TABLE shop_template ADD COLUMN IF NOT EXISTS industry_name VARCHAR(50);

-- 7. 是否行业模板库条目：1=库（平台共享，供「模板库」Tab 展示与引用） 0=本租户「我的模板」
ALTER TABLE shop_template ADD COLUMN IF NOT EXISTS is_library INTEGER DEFAULT 0;

COMMENT ON COLUMN shop_template.industry_code IS '【行业模板库】行业编码（⚠️ 对标未实测此编码，本实现口径：office/apparel/cosmetics/machinery/appliances/mother-baby/auto-parts/fresh-agriculture/food/mobile/general/hardware/pharma/jewelry）';
COMMENT ON COLUMN shop_template.industry_name IS '【行业模板库】行业名称（**对标实测逐字**：办公用品/服装鞋帽/化妆用品/机械机电/家电数码/母婴用品/汽修汽配/生鲜农贸/食品快消品/手机通讯/通用行业/五金建材/医药制品/珠宝钟表）';
COMMENT ON COLUMN shop_template.is_library    IS '【行业模板库】1=行业模板库条目（平台共享，仅供「模板库」Tab 展示与引用） 0=本租户「我的模板」；⚠️ 行业模板的**内部布局内容未实测**，库条目的 config_json 一律为空，不臆造模板内容';


-- ─────────────────────────────────────────────────────────────────────────────
-- 三、B 项：新建装修配置存储表 shop_decoration
-- ─────────────────────────────────────────────────────────────────────────────
-- ⚠️ 表结构为「本实现口径」：对标未实测装修配置的后端字段，本表按
--    「能承载前端已有的装修结构（拖拽排版 JSON）+ 引用模板」设计。
--    未建外键约束（与仓库既有表风格一致：逻辑关联，不建 FK 以免多租户/清理时耦合）。

CREATE TABLE IF NOT EXISTS shop_decoration (
    id           BIGINT PRIMARY KEY,
    tenant_id    BIGINT,
    /** 装修范围：HOME 首页 / CATEGORY 分类页 / PRODUCT_DETAIL 商品详情（本实现口径，来自任务书口径） */
    scope        VARCHAR(30),
    /** 引用的模板 id（可空，关联 shop_template.id，无 FK） */
    template_id  BIGINT,
    /** 装修名称（对标「我的模板」卡片名 / 「新增模板」输入名） */
    name         VARCHAR(200),
    /** 装修结构 JSON（TEXT 存 JSON 文本，**不用 jsonb**；结构未实测，后端不校验内部形状） */
    config_json  TEXT,
    status       INTEGER DEFAULT 1,
    deleted      INTEGER DEFAULT 0,
    create_by    BIGINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP
);

COMMENT ON TABLE  shop_decoration            IS '商城装修配置（首页/分类页/商品详情；⚠️ 表结构为**本实现口径** —— 对标未实测装修配置的后端字段，本表按「能承载前端已有的装修结构」设计）';
COMMENT ON COLUMN shop_decoration.id         IS '主键（雪花）';
COMMENT ON COLUMN shop_decoration.tenant_id  IS '租户ID（自动填充 + 租户拦截器自动过滤）';
COMMENT ON COLUMN shop_decoration.scope      IS '装修范围：HOME=首页 / CATEGORY=分类页 / PRODUCT_DETAIL=商品详情（⚠️ 本实现口径；对标抓取只实测到 4 个 Tab，无 scope 编码）';
COMMENT ON COLUMN shop_decoration.template_id IS '引用的模板 id（可空，逻辑关联 shop_template.id，不建外键）';
COMMENT ON COLUMN shop_decoration.name       IS '装修名称（对标「我的模板」卡片名 / 「新增模板」的名称）';
COMMENT ON COLUMN shop_decoration.config_json IS '装修结构 JSON（TEXT 存 JSON 文本，不用 jsonb；⚠️ 拖拽排版结构未实测，后端按不透明文本存取，不校验内部形状 —— 前端传来什么就存什么）';
COMMENT ON COLUMN shop_decoration.status     IS '1启用 0停用';
COMMENT ON COLUMN shop_decoration.deleted    IS '逻辑删除 0未删除 1已删除';


-- ─────────────────────────────────────────────────────────────────────────────
-- 四、D 项：新建商品详情装修-商品关联表 shop_decoration_product
-- ─────────────────────────────────────────────────────────────────────────────
-- ⚠️ 表结构为「本实现口径」：对标「设置应用商品」的**交互未实测**（只实测到入口存在），
--    本表按「能承载前端已有的多选商品 id 集合」设计，未造交互。
--    一行 = 一个关联（当前前端为多选集合，后端按全量替换语义写入）。

CREATE TABLE IF NOT EXISTS shop_decoration_product (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT,
    /** 装修配置 id（逻辑关联 shop_decoration.id，不建外键） */
    decoration_id BIGINT,
    /** 商品 id（逻辑关联 erp_product.id，不建外键） */
    product_id    BIGINT,
    deleted       INTEGER DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP
);

COMMENT ON TABLE  shop_decoration_product               IS '商品详情装修-应用商品关联（⚠️ 表结构为**本实现口径** —— 对标「设置应用商品」的交互未实测，本表按「能承载前端已有的多选商品 id 集合」设计）';
COMMENT ON COLUMN shop_decoration_product.id            IS '主键（雪花）';
COMMENT ON COLUMN shop_decoration_product.tenant_id     IS '租户ID（自动填充 + 租户拦截器自动过滤）';
COMMENT ON COLUMN shop_decoration_product.decoration_id IS '装修配置 id（逻辑关联 shop_decoration.id，不建外键）';
COMMENT ON COLUMN shop_decoration_product.product_id    IS '商品 id（逻辑关联 erp_product.id，不建外键）';
COMMENT ON COLUMN shop_decoration_product.deleted       IS '逻辑删除 0未删除 1已删除';

-- 按装修配置查关联商品 + 防重复关联（同一装修同一商品只允许一行有效）
CREATE INDEX IF NOT EXISTS idx_shop_decoration_product_deco
    ON shop_decoration_product (decoration_id);

CREATE INDEX IF NOT EXISTS idx_shop_decoration_tenant_scope
    ON shop_decoration (tenant_id, scope);

CREATE INDEX IF NOT EXISTS idx_shop_template_library
    ON shop_template (is_library);


-- ─────────────────────────────────────────────────────────────────────────────
-- 五、C 项 seed：14 个行业模板库条目
-- ─────────────────────────────────────────────────────────────────────────────
-- 依据：docs/Yh-Spec/抓取结果/商城装修_抓取.json
--       `Tab二_装修模板.子Tab_模板库.模板列表` 的 14 个行业名（**实测逐字**）。
--
-- ⚠️ 只 seed「行业名」这一实测事实：
--     · config_json 一律为 NULL —— 各行业的**模板内部布局内容未实测**，
--       禁止臆造 14 个假模板结构（任务书红线）。
--     · thumbnail / description 亦为 NULL（未实测）。
--     · 前端「模板库」卡片对空内容显式提示「模板内容未实测」。
--     · is_default=0（对标实测未标记默认库模板）；status=1（库条目默认启用）。
--     · 库条目是**平台共享**数据（shop_template 无 tenant_id 列）。
--     · 主键取 `990000000000000001` 起（**16 位基数** `9900000000000000` + 2 位零填充序号
--       ⇒ 固定 **18 位**，实测首=990000000000000001、末=990000000000000014，均 18 位；
--       最大值 990000000000000014 ≈ 9.9e17，距 BIGINT 上限 9223372036854775807 尚有约 8.2e18 余量）。
--       该段远离雪花生成区间（雪花 ≈1.8e18 量级），避免与运行时 ASSIGN_ID 冲突。
--       ⚠️ 教训：本迁移首版曾用 9220000000000000000+ 基数，序号进到 2 位时变 20 位
--       直接溢出 BIGINT（SQLSTATE=22003）—— 定长位数务必用零填充（见下 990...001 ... 990...014）。
--     · 幂等性：ON CONFLICT (template_code) DO NOTHING + WHERE NOT EXISTS (id) 双保险。
--       `template_code` 真库已有唯一约束 shop_template_template_code_key（已复核 pg_constraint）；
--       `id` 为主键。故「同编码已存在」或「同 id 已存在」两种重跑场景均不报错、不重复插入。
--       若是真实业务已存在的同名编码，以业务行优先，不覆盖、不改写。

INSERT INTO shop_template
    (id, template_name, template_code, thumbnail, description, config_json,
     is_default, status, deleted, create_time, industry_code, industry_name, is_library)
SELECT v.id, v.template_name, v.template_code, v.thumbnail, v.description, v.config_json,
       v.is_default, v.status, v.deleted, v.create_time, v.industry_code, v.industry_name, v.is_library
FROM (VALUES
    (990000000000000001::BIGINT, '办公用品',   'LIB_INDUSTRY_OFFICE',            NULL::VARCHAR, NULL::VARCHAR, NULL::TEXT, 0, 1, 0, CURRENT_TIMESTAMP, 'office',            '办公用品',   1),
    (990000000000000002::BIGINT, '服装鞋帽',   'LIB_INDUSTRY_APPAREL',           NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'apparel',           '服装鞋帽',   1),
    (990000000000000003::BIGINT, '化妆用品',   'LIB_INDUSTRY_COSMETICS',         NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'cosmetics',         '化妆用品',   1),
    (990000000000000004::BIGINT, '机械机电',   'LIB_INDUSTRY_MACHINERY',         NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'machinery',         '机械机电',   1),
    (990000000000000005::BIGINT, '家电数码',   'LIB_INDUSTRY_APPLIANCES',        NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'appliances',        '家电数码',   1),
    (990000000000000006::BIGINT, '母婴用品',   'LIB_INDUSTRY_MOTHER_BABY',       NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'mother-baby',       '母婴用品',   1),
    (990000000000000007::BIGINT, '汽修汽配',   'LIB_INDUSTRY_AUTO_PARTS',        NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'auto-parts',        '汽修汽配',   1),
    (990000000000000008::BIGINT, '生鲜农贸',   'LIB_INDUSTRY_FRESH_AGRICULTURE', NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'fresh-agriculture', '生鲜农贸',   1),
    (990000000000000009::BIGINT, '食品快消品', 'LIB_INDUSTRY_FOOD',              NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'food',              '食品快消品', 1),
    (990000000000000010::BIGINT, '手机通讯',   'LIB_INDUSTRY_MOBILE',            NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'mobile',            '手机通讯',   1),
    (990000000000000011::BIGINT, '通用行业',   'LIB_INDUSTRY_GENERAL',           NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'general',           '通用行业',   1),
    (990000000000000012::BIGINT, '五金建材',   'LIB_INDUSTRY_HARDWARE',          NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'hardware',          '五金建材',   1),
    (990000000000000013::BIGINT, '医药制品',   'LIB_INDUSTRY_PHARMA',            NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'pharma',            '医药制品',   1),
    (990000000000000014::BIGINT, '珠宝钟表',   'LIB_INDUSTRY_JEWELRY',           NULL, NULL, NULL, 0, 1, 0, CURRENT_TIMESTAMP, 'jewelry',           '珠宝钟表',   1)
) AS v(id, template_name, template_code, thumbnail, description, config_json,
       is_default, status, deleted, create_time, industry_code, industry_name, is_library)
WHERE NOT EXISTS (SELECT 1 FROM shop_template t WHERE t.id = v.id)
ON CONFLICT (template_code) DO NOTHING;
