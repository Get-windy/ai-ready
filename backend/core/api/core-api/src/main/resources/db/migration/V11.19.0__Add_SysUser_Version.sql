-- ============================================================
-- V11.19.0: sys_user 补 version 乐观锁列
--
-- 背景：
--   cn.aiedge.base.entity.User 继承含 @Version 的 BaseEntity，
--   但 sys_user 表无 version 列，导致 UserService.getById /
--   /api/v2/user/{id} 全线 500（工作流合并验证中实测暴露）。
-- ============================================================

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
