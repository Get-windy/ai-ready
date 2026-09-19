-- =============================================================================
-- 补齐「系统重建」（设置 → 账套操作 → 系统重建，菜单 70560 / set:rebuild）的权限码种子
-- 2026-09-18
--
-- 背景（《系统重建开发文档》§5.5 / §9.2-P1 / §11）：
--   本页是全系统唯一能「大片清空本租户数据」的危险操作页，但原实现：
--     ① 后端零控制器、零端点 → 无权限模型可言；
--     ② `sys_permission` 中 `set:%` 前缀 **0 行**（devdb 实测 2026-09-18）→ 无码可配。
--   本轮按对标重做（危险操作页 + 登录密码二次验证），并补上独立的高危权限码：
--       set:rebuild:execute —— 清除范围清单（GET /api/set/rebuild/options）
--                              与执行重建（POST /api/set/rebuild/execute）共用
--   （清单与执行同码：能看影响行数的人，就是有资格执行的人，不额外拆分读写码。）
--
--   写路径没有任何权限门控 = 给每个登录用户一把全库删除的钥匙，故本迁移是必修项。
--
-- 授权说明：按 V11.394.0 / V11.362.0 的既有做法，同时授权给超管角色（role_id = 1）。
--   超管另有 `*` 通配，这里只为让「权限清单」完整可审计；
--   普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
--
-- ⚠️ id 选择说明（与 9xxxx 区间的其它迁移不同，这里**刻意避开** 9xxxx）：
--   9xxxx 是多个模块迁移共用的手工号段，devdb 实测 91301–91311 / 91321–91322 / 91341–91345
--   已被 payment:* / log:oper:* / set:system-task:* / workflow:audit:* 占用，
--   并行开发时**极易撞号**（本迁移首版取 91311 即当场撞了 set:system-task:download）。
--   故本迁移改用「菜单号派生」的 id：
--       sys_permission.id        = 70560 * 100 + 1 = 7056001
--       sys_role_permission.id   = 70560 * 100 + 101 = 7056101
--   已核 devdb 7,000,000–8,000,000 区间在两表中均为 **0 行**（2026-09-18），不会与雪花 id 冲突。
--
-- 幂等：全部 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 7056001::BIGINT, 1, 0, 0, now(), now(),
       '系统重建执行', 'set:rebuild:execute', 3, '/api/set/rebuild/execute', 'POST', 397, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = 'set:rebuild:execute');

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 7056101::BIGINT, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'set:rebuild:execute'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
