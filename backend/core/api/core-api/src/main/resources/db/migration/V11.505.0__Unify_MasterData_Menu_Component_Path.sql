-- V11.504.0: 资料模块菜单的 component 写法统一为全式「views/xxx.vue」
--
-- 背景（2026-09-24 资料模块审计 · D-17b）：
--   同一模块内混用两种写法 —— 19 个叶子写 `views/md/xxx/index.vue`（全式），
--   5 个叶子写 `md/xxx/index` / `erp/product/index`（短式）。
--   前端 getComponent() 会剥 `views/` 前缀与 `.vue` 后缀再查 componentMap，
--   两种写法都能解析（因此无运行时影响），但混用让「菜单配置该怎么写」失去唯一答案，
--   新增菜单时容易跟错样例。此处统一为全式。
--
-- 范围：仅资料模块子树（60011）下的 5 条。全库另有 25 条短式 component
--       属仓储/销售/采购/CRM/财务/质量模块，不在本次收敛范围。
--
-- 另：顺带清理 70504「商品辅助资料」的残留 list_path（D-17 同源的菜单脏数据）——
--     该菜单 display_mode=0（单入口），但 list_path 仍留着 'md/product-supplement'。
--     前端仅在 displayMode===1 时才使用 list_path（dynamicRoutes.ts:1005），故无运行时
--     影响，属残留脏数据；清空与 mega-menu-redesign 的字段语义保持一致。

UPDATE sys_menu SET component = 'views/erp/product/index.vue' WHERE id = 70501 AND deleted = 0;
UPDATE sys_menu SET component = 'views/md/customer/index.vue'   WHERE id = 80510 AND deleted = 0;
UPDATE sys_menu SET component = 'views/md/supplier/index.vue'   WHERE id = 80511 AND deleted = 0;
UPDATE sys_menu SET component = 'views/md/logistics/index.vue'  WHERE id = 80512 AND deleted = 0;
UPDATE sys_menu SET component = 'views/md/partner/index.vue'    WHERE id = 80513 AND deleted = 0;

UPDATE sys_menu SET list_path = NULL, tag_label = NULL
 WHERE id = 70504 AND deleted = 0 AND display_mode = 0;
