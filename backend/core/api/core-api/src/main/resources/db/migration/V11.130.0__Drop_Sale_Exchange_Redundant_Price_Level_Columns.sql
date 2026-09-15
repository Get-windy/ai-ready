-- ============================================================================
-- 清理销售换货单明细的价格等级冗余列
--
-- 背景：
--   V11.103.0 初次以 price_level_1 … price_level_8 命名新增了价格等级列，
--   但 MyBatis-Plus 的驼峰→下划线转换规则（priceLevel8 → price_level8）
--   对应的是 price_level1 … price_level8，两套列同时存在造成冗余（违反
--   「禁止冗余字段」开发规范），且实体只会读写后者，前者恒为默认值。
--   V11.103.0 已改为按 MP 约定命名，此迁移负责删除已产生的冗余列。
--
-- 安全前提：price_level_1 … price_level_8 由本次改动引入，实体从未写入，
--   历史数据全为默认值 0，删除不丢数据。
-- ============================================================================

ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_1;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_2;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_3;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_4;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_5;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_6;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_7;
ALTER TABLE erp_sale_exchange_item DROP COLUMN IF EXISTS price_level_8;
