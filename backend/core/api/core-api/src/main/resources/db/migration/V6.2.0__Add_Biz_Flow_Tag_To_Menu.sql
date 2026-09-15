-- ============================================================
-- V6.2.0: 为菜单表添加业务流向标签
--
-- 变更说明：
--   1. 新增 biz_flow_tag 字段，标记菜单所属业务流
--      （sales, purchase, warehouse, wms, delivery, customer,
--        finance, expense, asset, budget, mall, orders,
--        workflow, product, printing, system）
--   2. 新增 display_group 字段，标记是否为纯展示分组
--      （0=正常路由目录，1=仅用于sidebar分组的虚拟节点）
--   3. 新增外链图标字段 link_icon，供外链菜单使用
--
-- 影响范围：
--   - sys_menu: 新增 3 个字段
-- ============================================================

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS biz_flow_tag VARCHAR(50) DEFAULT NULL;

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS display_group INTEGER DEFAULT 0;

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS link_icon VARCHAR(200) DEFAULT NULL;

COMMENT ON COLUMN sys_menu.biz_flow_tag IS '业务流向标签(sales/purchase/warehouse/wms/delivery/customer/finance/expense/asset/budget/mall/orders/workflow/product/printing/system)';
COMMENT ON COLUMN sys_menu.display_group IS '是否纯展示分组(0=正常路由目录,1=仅sidebar分组,不生成路由嵌套)';
COMMENT ON COLUMN sys_menu.link_icon IS '外链/快捷方式图标';
