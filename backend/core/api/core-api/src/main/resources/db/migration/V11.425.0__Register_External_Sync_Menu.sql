-- =============================================================================
-- 设置域 · 「外链同步」（孤儿页 system/data-import）挂菜单
-- V11.425.0 · 2026-09-19
--
-- 【背景】`views/system/data-import/index.vue`（1390 行）是孤儿页：无 DB 菜单、
--   无硬编码路由、无人 import。**组件路径里的 "data-import" 是命名误导**——
--   页内面包屑实为「首页 / 设置 / 外链同步」，标题「外链同步」，
--   描述「配置外部系统连接，管理字段映射规则」。
--
-- 【为什么判定为「补菜单」而非「删」】
--   与菜单 62304「同步任务」**不同源、不重复、互补**：
--     · 62304「同步任务」client_type='system-admin'，component=views/admin/data/sync（848 行）
--       → 后端 /api/data-source/sync（SyncTaskController）：管**同步任务的启停与投递执行**；
--         按《系统模块/同步任务开发文档》§9.3，它只报「已投递」、永不报「已同步」。
--     · 本页（租户侧，面包屑「设置」）
--       → 后端 /api/v1/sync-config（SyncConfigController，10 个端点）：
--         管**外部系统连接配置 + 字段映射 + 连通性测试 + 同步历史**。
--   实测本页调用：/v1/sync-config（CRUD）、/v1/sync-config/sources、
--   /v1/sync-config/{id}/test、/v1/sync-history/config/{id}。
--   两者一个管「配置」、一个管「执行」，且分属租户侧与平台侧，故保留并补入口。
--
-- 【挂载点】61201「系统配置」——设置域 60012 的子分组（tenant-admin）。
--   该组现有 6 个子菜单，sort 为 1/2/4/5/6/7，**sort=3 恰好空缺**，本菜单补入。
--
-- 【字段口径】逐列对照同父 80621「系统参数」实测值（devdb，2026-09-19）：
--   tenant_id=0 / parent_id=61201 / client_type='tenant-admin' / menu_type=1
--   visible=1 / status=1 / deleted=0 / is_external=0 / is_cache=1
--   display_group=0 / display_mode=0 / menu_level=3 / icon='' / route_name='' / redirect=''
--
-- 【id 号段】落地前实测：
--   SELECT count(*) FROM sys_menu WHERE id BETWEEN 80626 AND 80629                          → 0
--   SELECT count(*) FROM sys_menu WHERE deleted=0 AND (menu_code IN ('set:external-sync','system:data-import')
--          OR path IN ('set/external-sync','system/data-import/index'))                     → 0
--
-- 【幂等】ON CONFLICT (id) DO NOTHING
-- 【回滚】DELETE FROM sys_menu WHERE id = 80626;
-- =============================================================================

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80626, 0, 61201, 0, now(), now(),
        '外链同步', 'set:external-sync', 1, 'set/external-sync',
        'views/system/data-import/index.vue', '', '',
        '', 3, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 3)
ON CONFLICT (id) DO NOTHING;
