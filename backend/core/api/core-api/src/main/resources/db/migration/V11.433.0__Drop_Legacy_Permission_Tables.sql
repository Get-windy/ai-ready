-- =============================================================================
-- 删除权限域的历史遗留表（2026-09-20）
--
-- 【删除前的核查证据】（devdb 实测）
--   · 6 张表全部 0 行：
--       sys_data_permission / users / roles / permissions / user_roles / role_permissions
--   · 无任何视图依赖这些表（pg_views 定义检索为空）
--   · 仅僵尸表之间存在外键（user_roles→users/roles、role_permissions→roles/permissions），
--     故必须先删子表再删父表
--
-- 【为什么可以删】
--   · users / roles / permissions / user_roles / role_permissions —— 复数命名的**旧 RBAC 骨架**，
--     全库无实体、无 mapper、无查询，是单数命名体系（sys_user / sys_role / sys_role_permission …）
--     成型之前的遗留物。
--   · sys_data_permission —— 与真正在用的 sys_data_scope 功能重叠的**另一套**数据权限实现。
--     它唯一的消费方（/api/data-permission 的 Controller / Service / Mapper / 实体）已随本次
--     改动一并删除代码，前端从未调用过该接口。现行行级数据权限实现是
--     sys_data_scope（角色 × 目标表 × 规则类型），由 DataPermissionInterceptor 消费。
--
-- ⚠️ 本迁移不可逆：如需回滚，需从本文件之前的历史建表脚本重建。
-- =============================================================================

-- 先删带外键的子表，再删被引用的父表
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS sys_data_permission;
