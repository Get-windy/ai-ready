-- 2026-09-23 分析模块审计修复
--
-- 一、menu_code 重复致前端路由互相覆盖（页面静默 404）
--
--   背景：前端 frontend/apps/pc-admin/src/router/dynamicRoutes.ts:966 的路由名规则是
--         `name: menu.routeName || menu.menuCode`
--         两条菜单若 route_name 为空、menu_code 相同，会生成**同名路由**；
--         Vue Router 对同名路由是「后者覆盖前者」（同文件 :1216-1222 逐条 addRoute）
--         ⇒ path 不同的那一条**永远没有路由**，落到 Layout 下的 catch-all 404 页。
--         症状隐蔽：URL 不变、无 4xx、无 console 报错，只渲染 404 文案。
--
--   实测受影响页面（2026-09-23，tools/verify-menu-route-collision.cjs）：
--         80421 采购分析          /analytics/purchase-analysis   → 404（被 80422 覆盖）
--         70051 库存预警补货       /purchase/alert-replenish      → 404（被 70053 覆盖）
--         70052 缺货补货          /purchase/shortage-replenish   → 404（被 70053 覆盖）
--         70060 采购订单          /purchase/order/form           → 路由同样被 70071 覆盖，
--                                                                恰好被参数路由 purchase/order/:id 兜住而未暴露
--
--   修法：给这些菜单补上**唯一**的 route_name（沿用库中既有风格：大驼峰，如 SystemMenu）。
--         **刻意不改 menu_code** —— 它参与权限码前缀派生（如 purchase:analytics 能命中
--         purchase:analytics:list），改动会连带改变菜单可见性；且 80091/70011 这类
--         「真双入口」（path 相同、display_mode=1）本来就**有意共用** menu_code。

UPDATE sys_menu SET route_name = 'AnalyticsPurchaseAnalysis'  WHERE id = 80421 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'AnalyticsPurchasePrep'      WHERE id = 80422 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'PurchaseAlertReplenish'     WHERE id = 70051 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'PurchaseShortageReplenish'  WHERE id = 70052 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'PurchaseSmartReplenish'     WHERE id = 70053 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'PurchaseOrderForm'          WHERE id = 70060 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'PurchaseDetailQuery'        WHERE id = 70071 AND (route_name IS NULL OR route_name = '');

-- 二、分析模块 29 条叶子菜单的 menu_level 由 3 改回 0（非系统租户看不到整个分析模块）
--
--   服务端 SysMenuServiceImpl.getUserMegaMenus:276-279 对「非系统租户且非超管」强制
--         wrapper.eq(SysMenu::getMenuLevel, 0)
--   而这 29 条被配成 menu_level = 3 —— 一个**文档与前端菜单管理页都不承认**的取值
--   （mega-menu-redesign.md 的字段注释与菜单表单都只有 0=租户级 / 1=系统级）。
--   实测：tenant 2 用户请求菜单接口（带真实 tenantId=2）返回 188 个节点，其中
--         analytics/* 菜单 **0 条**，分析目录下 5 个分组子节点数全为 0。
--
--   注：全库 menu_level=3 另有 dms 19 / marketing 19 / finance 17 / mall 13 / set 10 /
--       md 8 / crm 6 等共 95 条，属各自模块的审计范围，本迁移只处理分析模块。

UPDATE sys_menu SET menu_level = 0 WHERE menu_level = 3 AND path LIKE 'analytics/%' AND deleted = 0;
