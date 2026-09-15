-- ============================================================================
-- V11.137.0 商品辅助资料（资料 → 商品管理 → 商品辅助资料）金标准升级（2026-09-11）
--
-- 背景：对标 ql361「商品辅助资料」实测为 3 子标签组合页
--      （商品品牌 / 商品单位 / 商品标签），本系统原为 4 Tab（多出「商品分类」）。
--
-- 对标系统实测抓取（2026-09-11，22stable.ql361.com）：
--   1) 商品品牌：工具栏「新增品牌 | 刷新 | 打印(F8) | 导出」，查询「筛选条件 + 查询」，
--      列「操作(修改/删除) | 品牌名称 | 助记码 | 备注」，弹窗「品牌名称* / 助记码* / 备注(限30字)」。
--   2) 商品单位：工具栏「新增单位 | 单位组管理 | 刷新 | 打印(F8) | 导出」，
--      列「操作(修改/删除) | 商品单位 | 助记码 | 计量单位备注 | 是否默认」，
--      弹窗「单位名称* / 助记码* / 备注(限30字)」。
--   3) 商品标签：无工具栏、无查询区（实测 titlebarText/queryText 均为空），
--      列「操作(修改/停用) | 标签名称 | 对应商品」，对应商品为关联商品名聚合串（超长省略）。
--
-- P0 辅助资料单一口径（红线）：品牌 / 单位 / 标签仍为全局基础资料，
--      商品主数据 / 销售 / 采购 / 仓储 / 财务统一引用，严禁另建重复字典。
--      本迁移只做「补列」与「新增单位组承载表」，不改动既有字典语义。
--
-- 处理：补列 / 建表 / 索引均幂等；sys_menu 归口对齐（菜单已存在，仅纠偏）。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 商品单位字典：补「是否默认」列（对标 Tab2 列「是否默认」）
-- ------------------------------------------------------------
ALTER TABLE erp_product_unit_dict
    ADD COLUMN IF NOT EXISTS is_default INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_product_unit_dict.is_default IS '是否默认单位: 1是 0否';

-- ------------------------------------------------------------
-- 2. 商城标签：补「状态」列（对标 Tab3 行内「停用」）
--    1启用 0停用（对齐 AGENTS.md 状态字段整型规范）
-- ------------------------------------------------------------
ALTER TABLE erp_mall_tag
    ADD COLUMN IF NOT EXISTS status INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN erp_mall_tag.status IS '状态: 1启用 0停用';

-- ------------------------------------------------------------
-- 3. 商品单位组（对标 Tab2 功能按钮「单位组管理」）
--    单位组 = 一组命名单位的集合（如「箱/包/个」），供商品多单位换算引用；
--    不是单位字典的副本，组内成员通过 unit_id 引用 erp_product_unit_dict。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_product_unit_group (
    id            BIGINT       PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL DEFAULT 0,
    group_name    VARCHAR(100) NOT NULL,
    mnemonic_code VARCHAR(50),
    remark        VARCHAR(500),
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    status        INTEGER      NOT NULL DEFAULT 1,
    deleted       INTEGER      NOT NULL DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_unit_group IS '商品单位组（命名单位集合，供商品多单位引用）';
COMMENT ON COLUMN erp_product_unit_group.status IS '状态: 1启用 0停用';

CREATE UNIQUE INDEX IF NOT EXISTS uk_unit_group_tenant
    ON erp_product_unit_group (tenant_id, group_name) WHERE deleted = 0;

-- 单位组成员
CREATE TABLE IF NOT EXISTS erp_product_unit_group_item (
    id              BIGINT       PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL DEFAULT 0,
    group_id        BIGINT       NOT NULL,
    unit_id         BIGINT,
    unit_name       VARCHAR(100) NOT NULL,
    conversion_rate NUMERIC(10, 4) NOT NULL DEFAULT 1,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_unit_group_item IS '商品单位组成员（组内单位及换算率）';

CREATE INDEX IF NOT EXISTS idx_unit_group_item_group
    ON erp_product_unit_group_item (tenant_id, group_id) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 4. 菜单归口：商品辅助资料（70504）→ views/md/product-supplement/index.vue
--    历史桩目录 views/md/product-aux 已被取代，此处幂等纠偏。
-- ------------------------------------------------------------
UPDATE sys_menu
   SET component = 'views/md/product-supplement/index.vue',
       path      = 'md/product-aux',
       list_path = 'md/product-supplement',
       menu_code = 'md:product-aux',
       display_mode = 0
 WHERE id = 70504;
