-- =============================================================================
-- 「系统任务」（设置 → 账套操作 → 系统任务，菜单 70561 / set:system-task）数据源收口
-- 2026-09-18
--
-- 背景（《系统任务开发文档》§1.1 / §7.3 / §12-P0）：
--   ql361 对标页是「**异步任务执行台账**」（9 列：任务ID·任务类型·任务名称·任务状态·创建人·
--   任务创建时间·任务开始时间·任务结束时间·结果查看，行内「下载处理结果」，实测 235 条）；
--   本系统该页**后端零控制器、库零表、表格恒空**（给 BillTableList 传了不存在的 prop）。
--
-- 数据源裁定 —— **复用既有执行台账，不新建表**（依据写在此处，便于后续审计）：
--   ① `scheduled_task_log`（实测 15 行真实执行记录，2026-09-14 演练产生）是本库**唯一**
--      带「开始/结束时间 + 执行状态 + 执行结果」语义的任务执行表，开发文档 §5.4 判它
--      「语义最贴近执行台账」；
--   ② 同族候选表全部为空且领域专用 —— `sys_scheduled_task`(18列/0行，仅有 cron 的**定义**)、
--      `sys_task_execute_log`(12列/0行)、`sys_sync_task`(0行，数据同步专用)、
--      `sys_print_task`/`erp_print_task`(0行，打印专用)、`dms_task`/`wms_*_task`(配送/WMS 作业专用)；
--      新建一张 `sys_async_task` 只会与既有执行表**并存两套执行模型**（开发文档 §7.3 明确「勿并存」）。
--   ③ 因此本迁移按开发文档 §7.3「路线 B（扩展既有执行表）」补齐 9 列契约缺的字段。
--
-- 列清单（对齐对标 9 列；**全部可空**，不加 NOT NULL）：
--   tenant_id          租户归属。**0 = 平台级**（定时任务由平台调度线程触发，不属任何租户，
--                      与 MyBatisPlusConfig 注释里 scheduled_task / scheduled_task_log 的
--                      「平台级调度配置，与租户无关」口径一致）；将来由租户发起的任务
--                      （导出 / 系统重建…）由写入方落自己的 tenant_id，页面对非平台行只展示本租户。
--                      ⚠️ DEFAULT 0 是**保持现状可见性**（现 /api/scheduler/task/log/page 无租户过滤），
--                      不是伪造值：既有 15 行的真实归属就是「平台调度」。
--   task_type          任务类型（对标值如「导出」）。既有行**全部**是 scheduled_task 的执行 →
--                      回填「定时任务」是事实陈述，非造数。
--   created_by_name    创建人**姓名**（对标显示「杨生淮」/「高晓丽」→ 必须存姓名而非 id）。
--                      既有行与自动调度行的发起人**未被写入侧记录**，一律留 NULL，页面如实显示「-」。
--   result_url         处理结果地址（相对 `storage.local.base-path` 的存储相对路径）。
--                      预留字段：当前无写入方，本页「下载处理结果」优先取 execute_result 文本。
--   result_expire_time 产物过期时刻（超过则下载端点返回 410，防磁盘/对象存储无限增长）。
--
-- 结构放开：`task_id` 原为 NOT NULL（每条日志必属某个定时任务定义）。系统任务是**跨领域台账**，
--   将来的导出 / 系统重建等异步任务没有「定时任务定义」可挂，故放开非空约束（放宽，不影响既有写入）。
--
-- 权限码种子（2 个，实测 devdb 2026-09-18：`set:%` 前缀此前仅 set:company-info:save 一行）：
--   set:system-task:view     任务台账查看   GET /api/set/system-task/page
--   set:system-task:download 处理结果下载   GET /api/set/system-task/{id}/result（**独立鉴权**，
--                            产物可能含敏感数据，不能只校验任务存在）
--   授权沿用 V11.394.0 / V11.396.0 口径：只授超级管理员角色（role_id = 1，另有 `*` 通配，
--   此处只为让权限清单完整可审计）；普通租户角色请由租户管理员在「系统 → 角色管理」按需勾选。
--   id 取 91310 / 91311（已核：91290–91399 区间内 91310–91320 未被占用；
--   ⚠️ 91301–91309 已被 payment:config:* / payment:channel:* / set:company-info:save 占用）。
--
-- 幂等：列用 ADD COLUMN IF NOT EXISTS、权限用 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

-- ── ① 台账列（对齐对标 9 列契约）───────────────────────────────────────────────
ALTER TABLE scheduled_task_log ADD COLUMN IF NOT EXISTS tenant_id          BIGINT DEFAULT 0;
ALTER TABLE scheduled_task_log ADD COLUMN IF NOT EXISTS task_type          VARCHAR(50);
ALTER TABLE scheduled_task_log ADD COLUMN IF NOT EXISTS created_by_name    VARCHAR(64);
ALTER TABLE scheduled_task_log ADD COLUMN IF NOT EXISTS result_url         VARCHAR(512);
ALTER TABLE scheduled_task_log ADD COLUMN IF NOT EXISTS result_expire_time TIMESTAMP;

COMMENT ON COLUMN scheduled_task_log.tenant_id          IS '租户归属：0=平台级（定时任务自动执行，全租户可见）；非0=发起方租户（仅本租户可见）';
COMMENT ON COLUMN scheduled_task_log.task_type          IS '任务类型（对标「导出」等；既有定时任务执行记录为「定时任务」）';
COMMENT ON COLUMN scheduled_task_log.created_by_name    IS '创建人姓名（发起方上报；未记录时为 NULL）';
COMMENT ON COLUMN scheduled_task_log.result_url         IS '处理结果地址（相对 storage.local.base-path 的存储相对路径；当前无写入方）';
COMMENT ON COLUMN scheduled_task_log.result_expire_time IS '处理结果过期时刻（过期后下载端点返回 410）';

-- ── ② 放开 task_id 非空：跨领域异步任务（导出/重建…）没有定时任务定义可挂 ──────
ALTER TABLE scheduled_task_log ALTER COLUMN task_id DROP NOT NULL;

-- ── ③ 回填任务类型：既有 15 行的真实来源就是 scheduled_task 的执行（事实，非造数）──
UPDATE scheduled_task_log SET task_type = '定时任务' WHERE task_type IS NULL;

-- ── ④ 分页索引：台账按「任务创建时间倒序」翻页 ────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_scheduled_task_log_tenant_create
    ON scheduled_task_log (tenant_id, create_time DESC);

-- ── ⑤ 权限码种子（只补码，不新造词根）────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91310::BIGINT, '系统任务查看', 'set:system-task:view',     '/api/set/system-task/page',          'GET', 401),
    (91311::BIGINT, '处理结果下载', 'set:system-task:download', '/api/set/system-task/{id}/result',   'GET', 402)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9133100 + (p.sort - 400), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('set:system-task:view', 'set:system-task:download')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
