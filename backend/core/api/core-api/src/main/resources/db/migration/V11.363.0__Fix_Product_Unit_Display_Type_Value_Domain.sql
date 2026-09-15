-- ============================================================================
-- V11.363.0  改正 erp_product_unit.unit_display_type 的取值口径（按对标实测）
--
-- ⚠️ 版本号说明：本迁移最初取号为 `V11.361.9`，后**改号为 `V11.363.0`**。原因：devdb 的
--   flyway_schema_history 中 `11.362.0`/`11.362.1` 已先于它被应用，而本迁移**尚未在任何环境
--   应用过**；本仓库只有 `application-dev.yml` 配了 `flyway.out-of-order: true`，非 dev profile
--   下 Flyway 会拒应用/校验告警一个"版本低于库中最大值"的新迁移。改为高于库中最大版本的号，
--   即可在所有 profile 下正常应用（本迁移只对 V11.361.8 新增的列做 COMMENT/UPDATE，不依赖
--   执行顺序，故改号安全）。除版本号外内容未改动。
--
-- 页面：frontend/apps/pc-admin/src/views/mall/unit-display/index.vue
--      接口：GET /erp/product/page（列表查询条件「单位显示类型」）
--            PUT /erp/product/batch-unit-display（批量写单位粒度显示类型）
-- 对标：docs/Yh-Spec/手动整理对标开发文档/交易模块/单位显示开发文档.md
--
-- 背景（改正原因）：
--   V11.361.8 给本列落的 SHOW/HIDE 二元口径是**猜测**（当时对标下拉取值未实测），**是错的**。
--   2026-09-14 对标 ql361「单位显示」页查询区「单位显示类型」下拉 DOM 实测共 4 项（逐字）：
--       全部              = -1
--       只显示常用单位     =  0
--       只显示小单位       =  1
--       只显示中/大单位    =  2
--   故本列语义是「**单位粒度显示类型**」，不是「显示/隐藏该单位」的布尔开关。
--   （「是否在商城显示」是另一个概念，落在商品级 erp_product.unit_display 1/0，
--     即对标查询区的「单位显示」条件；关联实测：「单位显示」= 全部(-1)/是(1)/否(2)。）
--
-- 落笔前核实（避免给不存在的列写 SQL 导致整个迁移失败、应用起不来）：
--   erp_product_unit 的**真实生效建表**是 V3.0.0:88 `CREATE TABLE IF NOT EXISTS erp_product_unit`
--   （V8.1.0:51 是同名 IF NOT EXISTS，实际不重建）；unit_display_type 列由 V11.361.8 新增，
--   **列已存在**，本迁移不重复 ADD COLUMN，也不 ALTER COLUMN TYPE（保持 VARCHAR(20)，
--   与 ProductUnit.unitDisplayType（String）映射一致，避免类型冲突）。
--
-- 幂等性：COMMENT / UPDATE 均可重复执行（第二次执行 UPDATE 匹配 0 行）。
-- ============================================================================

COMMENT ON COLUMN erp_product_unit.unit_display_type IS '单位显示类型（单位粒度显示类型）: -1=全部 0=只显示常用单位 1=只显示小单位 2=只显示中/大单位；NULL=未显式设置（读取时按商品级 erp_product.unit_display 兜底「该商品是否在商城显示」）。取值依据 2026-09-14 对标 ql361「单位显示」页查询区「单位显示类型」下拉 DOM 实测；V11.361.8 曾落的 SHOW/HIDE 二元猜测口径已废止。注：本列是「单位粒度」显示类型，与「是否显示该单位」的布尔开关（商品级 erp_product.unit_display）是两个概念。';

-- 历史猜测值清理（该列无生产积累，可直接改值域）：
--   1) SHOW（=在商城显示该单位）在 4 值域中最接近「-1=全部」，按 -1 归一，保留"显示"语义；
UPDATE erp_product_unit
   SET unit_display_type = '-1'
 WHERE unit_display_type = 'SHOW';

--   2) 其余不属于 -1/0/1/2 的历史值（含 HIDE：4 值域中**没有**"隐藏"这个粒度，
--      不编造枚举值）统一置 NULL = 未显式设置，由商品级 erp_product.unit_display 兜底。
UPDATE erp_product_unit
   SET unit_display_type = NULL
 WHERE unit_display_type IS NOT NULL
   AND unit_display_type NOT IN ('-1', '0', '1', '2');

-- 索引已在 V11.361.8 建好（idx_erp_product_unit_display_type），本迁移不重复创建。
