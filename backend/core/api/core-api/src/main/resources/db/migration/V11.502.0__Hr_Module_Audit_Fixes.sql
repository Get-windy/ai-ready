-- 2026-09-23 人力资源模块全栈审计修复
-- 报告：HR_MODULE_AUDIT_20260923.md（§6 建议的处置次序 · 第二批/第三批）
--
-- ══════════════════════════════════════════════════════════════════════════
-- 一、hr_employee 工号唯一约束补上租户维度（P0-3）
-- ══════════════════════════════════════════════════════════════════════════
-- 实测原约束：hr_employee_employee_no_key  UNIQUE (employee_no)   ← 不含 tenant_id
-- 而号段是 per-tenant 的（biz_number_sequence 唯一键 = (tenant_id, biz_type, locale)），
-- 生成规则 `{prefix}-{YYYYMMDD}-{seq}` 里**没有任何租户维度**，且按天重置。
-- 后果：两个租户同一天各建第一个员工，都会得到 `EMP-YYYYMMDD-0001` ⇒ 撞全局唯一约束报 400。
--   （本库目前 hr_employee 0 行，所以是潜伏态；多租户一开就会现。）
--
-- 顺带订正第二处不一致：旧索引**不含 deleted 条件**，而应用层的工号查重
-- （HrEmployeeServiceImpl.existsEmployeeNo）走 MyBatis-Plus，逻辑删除行**不参与**统计
-- ⇒ 删掉一个员工后，重新用同一个工号会被应用层放行、却被数据库唯一索引拒绝，
--    表现为莫名的「请求数据不完整或存在冲突」。改为**部分唯一索引**（WHERE deleted = 0）
--    与逻辑删除口径对齐 —— 写法与 hr_leave_quota 的 uk 一致（V11.380.0 §九）。
--
-- ⚠️ 若库里已有跨租户重复工号，CREATE UNIQUE INDEX 会失败并中止本迁移（这是故意的：
--    宁可启动失败也不要悄悄放过脏数据）。当前实测 hr_employee 0 行、无重复。
--
-- 【回滚】
--   DROP INDEX IF EXISTS uk_hr_employee_tenant_employee_no;
--   ALTER TABLE hr_employee ADD CONSTRAINT hr_employee_employee_no_key UNIQUE (employee_no);
ALTER TABLE hr_employee DROP CONSTRAINT IF EXISTS hr_employee_employee_no_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_hr_employee_tenant_employee_no
    ON hr_employee (tenant_id, employee_no) WHERE deleted = 0;

-- ══════════════════════════════════════════════════════════════════════════
-- 二、4 条 HR 菜单的 menu_level 由 3 改回 0（P2-2）
-- ══════════════════════════════════════════════════════════════════════════
-- 服务端 SysMenuServiceImpl.getUserMegaMenus 对「非系统租户且非超管」强制
--     wrapper.eq(SysMenu::getMenuLevel, 0)
-- 《mega-menu-redesign.md》与菜单管理表单都只有 0=租户级 / 1=系统级 —— 3 是无人承认的取值。
--
-- 这 4 条（80530/80531/80532 职员管理三页 + 907 招聘管理）是 V6.22.0 / V11.380.0 / V11.422.0
-- 补录时写的 3（V11.422.0 的注释还明确写「跟随 80530~80532 的实测值取 3」）。
-- 后果：**非系统租户的租户管理员看不到这 4 页**。
-- ⚠️ dev 的 admin 命中「系统租户 + 超管」双豁免（走早退分支取全量），本地永远看不出来。
--
-- 注：全库 menu_level=3 共 124 条，V11.499.0 处理分析模块 29 条、V11.500.0 处理 CRM、
--    V11.501.0 处理配送 20 条；本条只处理 HR 这 4 条。
--
-- 【回滚】UPDATE sys_menu SET menu_level = 3 WHERE id IN (80530, 80531, 80532, 907);
UPDATE sys_menu SET menu_level = 0, update_time = CURRENT_TIMESTAMP
 WHERE id IN (80530, 80531, 80532, 907) AND deleted = 0;

-- ══════════════════════════════════════════════════════════════════════════
-- 三、菜单 907「招聘管理」的 menu_code 对齐权限码前缀（P1-4）
-- ══════════════════════════════════════════════════════════════════════════
-- 原值 `hr-recruitment`（**连字符**），而该页接口的鉴权码是 `hr:recruitment:*`（冒号）。
-- menu_code 参与菜单可见性派生（getUserMegaMenus + MenuPermissionDeriver）：
--   ① 库中存在以 menu_code 为前缀的权限码、且用户一个都不持有 ⇒ 菜单隐藏；
--   ② 库中**不存在**任何以该 menu_code 为前缀的权限码 ⇒ 菜单保持可见（fail-open 过渡口径）。
-- `hr-recruitment` 落在 ② ⇒ 该页**对所有登录用户恒可见、无法按角色收窄**，
-- 而它调用的接口受 @RequiresPermission("hr:recruitment:*") 保护 ⇒ 点进去 403。
--
-- 修法与 V11.498.0（采购）/ V11.500.0（CRM）一致：只把 menu_code 改成该页真实接口
-- 所需权限码的前缀，**不新增权限码、不改 path/component、不删菜单**。
-- 证据：HrRecruitmentController 全部 8 个端点用 hr:recruitment:{list,create,update,delete}。
--
-- 已核：改后 `hr:recruitment` 在全库 menu_code 中唯一（无重复组），故**不需要**补 route_name。
--
-- 【回滚】UPDATE sys_menu SET menu_code = 'hr-recruitment' WHERE id = 907 AND deleted = 0;
UPDATE sys_menu SET menu_code = 'hr:recruitment', update_time = CURRENT_TIMESTAMP
 WHERE id = 907 AND deleted = 0;

-- ══════════════════════════════════════════════════════════════════════════
-- 四、软删 9071/9072/9073 —— 指向已删除权限码的孤儿按钮菜单（P1-4）
-- ══════════════════════════════════════════════════════════════════════════
-- 这三条是 menu_type=3 的按钮型菜单，menu_code 分别 = hr-recruitment:add/edit/delete。
-- 那三条权限码已由 V11.435.0__Remove_Legacy_Permission_Codes.sql 从 sys_permission 删除
-- ⇒ 按钮菜单指向的权限码在库里已不存在，勾选它不会控制任何东西（role/index.vue 的
--   权限矩阵注释里也写明了「勾进角色不会控制任何东西」）。
-- 且招聘页前端**零 v-permission**，页面按钮本来就不依赖它们。
--
-- 处置=软删（与本仓菜单清理惯例一致，见 V11.429.0）；不物理删除，便于回滚与追溯。
--
-- 【回滚】UPDATE sys_menu SET deleted = 0 WHERE id IN (9071, 9072, 9073);
UPDATE sys_menu SET deleted = 1, update_time = CURRENT_TIMESTAMP
 WHERE id IN (9071, 9072, 9073) AND deleted = 0;

-- ══════════════════════════════════════════════════════════════════════════
-- 五、补 2 条删除类权限码（P1-6：权限矩阵「删除」列失真）
-- ══════════════════════════════════════════════════════════════════════════
-- 现状：HrController 的 3 个删除类端点挂的是**修改**码 ——
--   DELETE /salary/payment/{id}    → hr:salary:update
--   DELETE /salary/structure/{id}  → hr:salary:update
--   DELETE /performance/{id}       → hr:performance:update
-- 而角色页的权限矩阵把「动作段」映射成 6 列（查看/打印/增加/删除/修改/导出，见
-- views/system/role/index.vue 的 ACTION_COLUMNS）⇒ 薪资/绩效的**「删除」列永远是空的**、
-- 「修改」列同时管增删改，勾选它等于把删除权限一起给出去了。
--
-- 修法：为两个域各补一条 `:delete` 码，Controller 侧同步改注解（见本次提交的 HrController）。
-- 授权由 PermissionInitializationConfig.assignAllPermissionsToSuperAdmin() 在启动时自动补齐；
-- 非超管角色的授权见 tools/grant-hr-permissions.py。
--
-- ⚠️ 不新增 `hr:attendance:create`：新增考勤（补卡/更正）与修改考勤同属「更正」语义，
--    合成一条 `hr:attendance:update` 是刻意的，不再是矩阵失真。本迁移只处理真正的删除动作。
--
-- 【回滚】DELETE FROM sys_permission WHERE id IN (91207, 91208);
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91207::BIGINT, 'HR薪资删除', 'hr:salary:delete',      '/api/hr/salary/payment/*',   'DELETE', 355),
    (91208,         'HR绩效删除', 'hr:performance:delete', '/api/hr/performance/*',      'DELETE', 364)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);
