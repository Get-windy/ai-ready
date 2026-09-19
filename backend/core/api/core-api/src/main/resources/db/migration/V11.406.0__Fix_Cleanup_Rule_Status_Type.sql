-- =============================================================================
-- 清理规则（系统 → 数据管理 → 清理规则，菜单 62305）status 列类型修正
--
-- 问题（2026-09-18 实测）：`sys_data_cleanup_rule.status` 是 **integer**，
--   而实体 `cn.aiedge.datasource.model.CleanupRule.status` 是 **String**，
--   同组的 `sys_data_source.status` / `sys_sync_task.status` / `sys_backup_record.status`
--   都是 **character varying**。
--   类型不匹配的后果是**写操作全部失败**：MyBatis-Plus 生成 `status = ?` 绑 String 参数，
--   PostgreSQL 报 `操作符不存在: integer = character varying`（或 `column "status" is of type
--   integer but expression is of type character varying`）→ 新增/编辑/删除一律 500，
--   页面表现为「点不动」。读操作因走 `SELECT *` 而不报错，所以只有写路径暴露。
--
-- 修正方向：把列改成 varchar，向同组三张表看齐（实体的 String 语义不变，改动面最小）。
--   该表当前 **0 行**（devdb 实测），转换无数据风险；仍写 USING 保证通用性。
-- =============================================================================

ALTER TABLE sys_data_cleanup_rule
    ALTER COLUMN status TYPE VARCHAR(32) USING status::VARCHAR;

COMMENT ON COLUMN sys_data_cleanup_rule.status IS '规则状态（字符串枚举，与同组 sys_data_source.status / sys_sync_task.status 口径一致）';
