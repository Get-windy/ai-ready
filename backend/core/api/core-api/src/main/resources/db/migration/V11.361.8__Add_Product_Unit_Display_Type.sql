-- ============================================================================
-- V11.361.8  商城「单位显示」（交易模块 → 商城 → 基础业务 → 单位显示）单位级显示类型
--
-- 页面：frontend/apps/pc-admin/src/views/mall/unit-display/index.vue
--      接口：GET /erp/product/page（列表）、PUT /erp/product/batch-unit-display（行内开关 / 批量显示隐藏）
-- 对标：docs/Yh-Spec/手动整理对标开发文档/交易模块/单位显示开发文档.md
--      抓取：docs/Yh-Spec/抓取结果/单位显示_抓取.json
--
-- 背景（已核实的缺口）：
--   对标「单位显示」是**按单位粒度**的商城显示开关，查询区固定项含「单位显示」与
--   「单位显示类型」两个条件；而本系统此前只有商品级 erp_product.unit_display
--   （V11.361.3 补列），没有任何单位维度的显示字段，页面被迫用
--   erp_product.mall_shelf_status（商城上架状态）承载「单位显示」勾选。
--
-- 落笔前核实（避免给不存在的列写 SQL 导致整个迁移失败、应用起不来）：
--   1) erp_product_unit 的**真实生效建表**是 V3.0.0:88 `CREATE TABLE IF NOT EXISTS erp_product_unit`
--      （V8.1.0:51 是同名 IF NOT EXISTS，实际不会重建）；其后叠加的 ALTER 见
--      V8.1.0:75（定价列，含 preset_purchase_price）、V9.23.0、V9.29.0（删 grade_price_*）、
--      V11.139.0（恢复 grade_price_1..8）、V11.154.0（weight/volume）。
--      → deleted 列存在（V3.0.0:97），preset_purchase_price 列存在（V8.1.0:77/92，不重复添加）。
--   2) 全库检索确认：`unit_display_type` 在迁移与源码中**均不存在**，本次为首次新增。
--
-- 口径（本实现口径，非对标实测枚举）：
--   对标 ql361「单位显示类型」下拉的**取值未实测**（抓取记录仅证明该控件存在，
--   未取到选项列表；页面对标文档亦未给出取值口径），故不编造枚举：
--   本期先落地与页面 √/× 开关一致的二元口径 —— SHOW / HIDE。
--   字段可空：NULL = 未显式设置，读取时按商品级 erp_product.unit_display 兜底。
-- ============================================================================

ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS unit_display_type VARCHAR(20);

COMMENT ON COLUMN erp_product_unit.unit_display_type IS '单位显示类型（单位级商城显示开关）: SHOW=在商城显示该单位 HIDE=在商城隐藏该单位；NULL=未显式设置，按商品级 erp_product.unit_display 兜底。⚠️ 本实现口径：对标 ql361「单位显示类型」下拉取值未实测（抓取仅记录控件存在），故先落地 显示/隐藏 二元口径，待对标枚举实测后校准；勿据此反推对标语义。';

-- 「单位显示类型」查询条件按本列反查商品ID集合（deleted=0），加索引避免全表扫
CREATE INDEX IF NOT EXISTS idx_erp_product_unit_display_type
    ON erp_product_unit (unit_display_type);
