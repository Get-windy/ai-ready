-- =============================================================================
-- 补 HR 候选人删除权限码（2026-09-18）
--
-- 背景：V11.380.0 为 HR 模块补了 42 条权限码，但**漏了 `hr:candidate:delete`** ——
--       当时后端确实没有候选人删除端点，所以看不出缺。
--       本轮补齐该端点（`DELETE /api/hr/candidate/{id}`，已入职候选人不可删）后发现：
--       前端《招聘管理》候选人 Tab 的行内「删除」按钮本来就在（`views/hr/recruitment/list.vue`
--       调 `hrCandidateApi.remove`），而该权限码不存在 → 除超管（走 `*` 通配）外
--       所有角色点「删除」都会 403。
--
-- 幂等：已存在则不重复插入。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 91185, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       'HR候选人删除', 'hr:candidate:delete', 3, '/api/hr/candidate/*', 'DELETE', 385, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'hr:candidate:delete');
