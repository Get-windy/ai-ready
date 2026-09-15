-- 定时任务执行目标改为「白名单处理器」（job_key）+ 登记「开发工具 → 定时任务」权限码
--
-- 背景（2026-09-14 复核，两个真实缺陷）：
--   ① 执行器用 Class.forName(execute_class).getDeclaredConstructor().newInstance()
--      + getMethod(execute_method, String.class) 反射调用：
--      · 只能跑「无参构造 + 单 String 参数」的普通类 → 无法注入 Spring Bean，定时任务实际跑不起来
--        （现存种子任务 execute_class/execute_method 全为 NULL、执行日志 0 条）；
--      · 类名/方法名来自请求体 → 任意登录用户可让后台线程反射调用类路径上的方法（越权 + 危险）。
--   ② 接口只有 @SaCheckLogin，缺权限码。
--
-- 处置：新增 job_key（白名单处理器键，由 JobHandlerRegistry 解析）；不再读取类名/方法名并删除这两个死字段；
--       历史演示任务（无 job_key）停用；登记权限码并把「配送·超时任务升级扫描」任务行**默认停用**插入。

ALTER TABLE scheduled_task ADD COLUMN IF NOT EXISTS job_key varchar(128);
COMMENT ON COLUMN scheduled_task.job_key IS '执行目标处理器键（JobHandler#key()，白名单）：取代原 execute_class/execute_method 反射调用';

-- 历史演示任务（无 job_key）停用：否则启用后每轮都在「执行日志」里留一条「未注册的任务处理器」失败
UPDATE scheduled_task
SET enabled = 0,
    status = 'STOPPED',
    task_desc = LEFT(COALESCE(task_desc, '') || '（历史演示数据：未绑定 job_key，已停用）', 500)
WHERE deleted = 0
  AND (job_key IS NULL OR job_key = '')
  AND COALESCE(task_desc, '') NOT LIKE '%未绑定 job_key%';

-- 不再使用的反射字段（执行目标已由 job_key 白名单解析）
ALTER TABLE scheduled_task DROP COLUMN IF EXISTS execute_class;
ALTER TABLE scheduled_task DROP COLUMN IF EXISTS execute_method;

-- 配送·超时任务升级扫描（《调度任务开发文档》§3.6 约束 2 的定时化）
-- 默认 enabled = 0（需人工确认后启用）；参数默认「演练」模式，落库前可先看命中量
INSERT INTO scheduled_task (task_name, task_desc, task_type, cron_expression, execute_params, job_key,
                            status, retry_count, retry_interval, timeout, enabled,
                            execute_count, success_count, fail_count, tenant_id, create_time, update_time, deleted)
SELECT '配送·超时任务升级扫描',
       '在途任务超时未接单 → 按派单策略换人重派（确无他人可派时退回原配送员并重新计时）；配送中/取货中只告警审计。'
       || '安全闸门：默认停用 + Redis 锁多实例互斥 + execute_params 支持 {"dryRun": true} 演练',
       'CRON', '0 */10 * * * ?', '{"dryRun": true}', 'dms.dispatch.escalateOverdue',
       'STOPPED', 0, 60, 300, 0,
       0, 0, 0, 0, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM scheduled_task WHERE job_key = 'dms.dispatch.escalateOverdue' AND deleted = 0);

-- 权限码登记（开发工具 → 定时任务，菜单 62405 `system:dev:scheduler`）
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90211, 0, 0, 0, now(), now(), '定时任务查看', 'system:dev:scheduler:list', 3,
       '/api/scheduler/task/**', 'GET', 211, 1, 0, '任务/日志/统计/处理器清单'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:dev:scheduler:list');

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90212, 0, 0, 0, now(), now(), '定时任务新增', 'system:dev:scheduler:create', 3,
       '/api/scheduler/task', 'POST', 212, 1, 0, '创建定时任务'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:dev:scheduler:create');

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90213, 0, 0, 0, now(), now(), '定时任务修改', 'system:dev:scheduler:update', 3,
       '/api/scheduler/task/{id}', 'PUT', 213, 1, 0, '编辑/启用/停用/暂停/恢复'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:dev:scheduler:update');

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90214, 0, 0, 0, now(), now(), '定时任务删除', 'system:dev:scheduler:delete', 3,
       '/api/scheduler/task/{id}', 'DELETE', 214, 1, 0, '删除定时任务'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:dev:scheduler:delete');

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90215, 0, 0, 0, now(), now(), '定时任务执行', 'system:dev:scheduler:execute', 3,
       '/api/scheduler/task/{id}/execute', 'POST', 215, 1, 0, '立即执行/批量执行/重试'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:dev:scheduler:execute');

-- 授权：超级管理员角色（role_id = 1；超管另有 "*" 通配，这里保证「权限清单」完整可审计）
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9021101, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'system:dev:scheduler:list'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9021102, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'system:dev:scheduler:create'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9021103, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'system:dev:scheduler:update'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9021104, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'system:dev:scheduler:delete'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9021105, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'system:dev:scheduler:execute'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);
