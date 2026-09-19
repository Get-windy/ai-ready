-- =============================================================================
-- 商品管理 · 「价格审批」补菜单（配套 V11.424.0 的后端补齐）
-- V11.426.0 · 2026-09-19
--
-- 【背景】V11.424.0 已按前端既有契约补齐价格审批后端（8 个端点）。
--   但 `views/erp/pricing/approval/index.vue`（1241 行）此前一直是孤儿页：
--   没有任何 DB 菜单指向它，仅靠 `dynamicRoutes.ts` 的 requiredRoutes 硬编码
--   让 URL 可直达、侧边栏无入口。后端补齐后应给它正式入口。
--
-- 【同时清理的姊妹页】同目录的 `views/erp/pricing/index.vue`（价格管理，1704 行）已删除：
--   与菜单 70503「商品价格管理」功能重叠，且其独有功能（对比等级/季节性调价/变更记录）
--   依赖 `/erp/pricing/partner-grade-prices/*` —— 该接口后端不存在（只有
--   `/api/erp/product-grade-price`）⇒ 调用必 404；其 v-permission 码
--   pricing:compare/seasonal/history 在 sys_permission 中同样不存在。既是坏功能又无入口。
--
-- 【挂载点】61101「商品管理」（tenant-admin），紧随 70503「商品价格管理」之后，sort=6。
--
-- 【字段口径】逐列对照同父 70505「图片管理」实测值（devdb，2026-09-19）：
--   tenant_id=0 / parent_id=61101 / client_type='tenant-admin' / menu_type=1
--   visible=1 / status=1 / deleted=0 / is_external=0 / is_cache=1
--   display_group=0 / display_mode=0 / **menu_level=0** / icon='' / route_name='' / redirect=''
--   ⚠️ 该组 menu_level 实测为 0（与 61405 职员管理组的 3、61201 系统配置组的 3 都不同），
--      故按同父取值，不要套用别的分组。
--
-- 【与硬编码路由的关系】`dynamicRoutes.ts` 的 requiredRoutes 里保留了同 path
--   （erp/pricing/approval）的兜底条目，**刻意不删**：菜单树有 30 分钟本地缓存，
--   菜单在旧缓存会话里点不到，但按 URL 直达仍应能打开；两者 component 指向同一文件，
--   行为一致。口径同 workflow/process-analysis 的处理（见该处注释）。
--
-- 【id 号段】落地前实测：
--   SELECT count(*) FROM sys_menu WHERE id BETWEEN 70506 AND 70509                              → 0
--   SELECT count(*) FROM sys_menu WHERE deleted=0 AND (path='erp/pricing/approval'
--          OR menu_code='md:price-approval')                                                    → 0
--
-- 【幂等】ON CONFLICT (id) DO NOTHING
-- 【回滚】DELETE FROM sys_menu WHERE id = 70506;
-- =============================================================================

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (70506, 0, 61101, 0, now(), now(),
        '价格审批', 'md:price-approval', 1, 'erp/pricing/approval',
        'views/erp/pricing/approval/index.vue', '', '',
        '', 6, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
