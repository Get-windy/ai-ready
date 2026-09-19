-- =============================================================================
-- 采购管理 · 「采购换货单」菜单组件路径修复
-- V11.427.0 · 2026-09-19
--
-- 【背景】菜单 70063「采购换货单」的 component 仍是 `views/erp/purchase-exchange/form.vue`，
--   但该文件已不存在：实现在提交 2e08cce53 之后搬迁到
--   `frontend/apps/pc-admin/src/views/purchase/exchange/form.vue`
--   （`views/erp/purchase-exchange/` 目录下如今只剩 git 历史，磁盘为空）。
--
-- 【症状】菜单本身能点开——它 display_mode=1，走 list_path='purchase/exchange/index'，
--   该键在 `dynamicRoutes.ts:405-406` 有映射。但点「新增/编辑」跳到
--   path='purchase/exchange/form' 时，`getComponent('views/erp/purchase-exchange/form.vue')`
--   归一化后为 'erp/purchase-exchange/form'，`dynamicRoutes.ts:930-955` 的三级兜底
--   （原样 / 追加 /index / 去掉 /index）全部未命中，落入最后的动态 import 兜底，
--   页面显示「页面组件未找到: erp/purchase-exchange/form」。
--   ⇒ **采购换货单无法录入**（列表可查、不能新增/编辑）。
--
-- 【修复依据】`dynamicRoutes.ts:407` 已存在
--   'purchase/exchange/form': () => import('@/views/purchase/exchange/form.vue')
--   所以只需把菜单 component 指过去。DB 存 'views/purchase/exchange/form.vue'，
--   `getComponent` 会先剥掉 `views/` 前缀与 `.vue` 后缀，与存 'purchase/exchange/form' 等价；
--   此处采用前者，与同父菜单 70062「采购退货单」的写法保持一致。
--
-- 【幂等】仅在仍为旧值时才更新——重复执行无副作用，也不会覆盖人工修正过的值。
-- 【回滚】UPDATE sys_menu SET component='views/erp/purchase-exchange/form.vue' WHERE id=70063;
-- =============================================================================

UPDATE sys_menu
   SET component   = 'views/purchase/exchange/form.vue',
       update_time = now()
 WHERE id = 70063
   AND component = 'views/erp/purchase-exchange/form.vue';
