-- ============================================================================
-- V11.160.0 商品辅助资料「单位组管理」对齐对标实测（2026-09-11 重抓）
--
-- 对标实测（22stable.ql361.com，截图与 JSON 证据 tool-results/ql361/pages/aux-probe2|4|5|6/）：
--   1) 「单位组管理」是商品单位页工具栏按钮，打开的是**弹窗**（不是独立页面）：
--        工具栏 新增单位组 | 打印(F8) | 导出 | 刷新
--        查询   单位(输入) | 显示状态(下拉) | 查询
--        列     操作(修改/更多) | 单位 | 单位关系        ← 仅 2 个可配置列，均默认显示
--        数据   袋,提,箱 | 1:12:48   袋,箱 | 1:32   个,件 | 1:12   个,提 | 1:12   盒,箱 | 1:48
--      分页    共 5 条记录 / 每页 20 行
--   2) 「单位组新增编辑」表单（内部视图 GoodsUnitTemplateEditor）为**固定 3 行**网格：
--        列：类型 | 单位名称 | 换算关系
--        行：小单位(默认 1) / 中单位 / 大单位
--        按钮：保存(Enter) | 关闭(Esc)
--   3) 结论：单位组 = **商品多单位换算模板**（小/中/大 三个单位 + 相对小单位的换算关系），
--      不是原实现里的「命名单位集合（组名/助记码/备注）」——对标既无组名列、也无备注列，
--      表单亦无名称输入项。故此处按实测收敛表结构。
--
-- P0 口径：单位组成员仍通过 unit_id 引用 erp_product_unit_dict（不复制单位字典）。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 主表：去掉对标不存在的业务列（组名/助记码/备注/排序）
--    对标列表只展示「单位」「单位关系」，二者由明细聚合得到。
-- ------------------------------------------------------------
DROP INDEX IF EXISTS uk_unit_group_tenant;

ALTER TABLE erp_product_unit_group DROP COLUMN IF EXISTS group_name;
ALTER TABLE erp_product_unit_group DROP COLUMN IF EXISTS mnemonic_code;
ALTER TABLE erp_product_unit_group DROP COLUMN IF EXISTS remark;
ALTER TABLE erp_product_unit_group DROP COLUMN IF EXISTS sort_order;

COMMENT ON TABLE erp_product_unit_group IS '商品单位组（商品多单位换算模板：小/中/大单位及换算关系）';
COMMENT ON COLUMN erp_product_unit_group.status IS '状态: 1启用 0停用';

-- ------------------------------------------------------------
-- 2. 明细表：补「单位类型」（对标表单固定 3 行：小单位/中单位/大单位）
--    conversion_rate 语义 = 对标「换算关系」（相对小单位=1 的倍数）
-- ------------------------------------------------------------
ALTER TABLE erp_product_unit_group_item ADD COLUMN IF NOT EXISTS unit_type VARCHAR(10);

COMMENT ON COLUMN erp_product_unit_group_item.unit_type IS '单位类型: SMALL小单位 MEDIUM中单位 LARGE大单位';
COMMENT ON COLUMN erp_product_unit_group_item.conversion_rate IS '换算关系（相对小单位=1 的倍数）';

-- 一个单位组内每个类型只允许一行
CREATE UNIQUE INDEX IF NOT EXISTS uk_unit_group_item_type
    ON erp_product_unit_group_item (tenant_id, group_id, unit_type) WHERE deleted = 0;
