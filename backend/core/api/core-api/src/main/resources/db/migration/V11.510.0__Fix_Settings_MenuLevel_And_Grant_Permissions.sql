-- 2026-09-26 设置模块全栈审计修复
-- 报告：SETTINGS_MODULE_AUDIT_20260924.md（§1 P0-1、§2、§3）
--
-- ══════════════════════════════════════════════════════════════════════════
-- 一、10 条设置菜单的 menu_level 由 3 改回 0（P0-1 的第一半）
-- ══════════════════════════════════════════════════════════════════════════
-- 服务端 SysMenuServiceImpl.getUserMegaMenus 对「非系统租户且非超管」强制
--     wrapper.eq(SysMenu::getMenuLevel, 0)
-- 《mega-menu-redesign.md》与菜单管理表单都只有 0=租户级 / 1=系统级 —— 3 是无人承认的取值。
--
-- 设置子树 26 个叶子里，这 10 条是 menu_level=3（其余 16 条为 0）：
--   80610 流程设计 / 80620 菜单配置 / 80621 系统参数 / 80622 审核设置 / 80623 支付配置
--   80624 企业信息 / 80625 应用中心 / 80626 外链同步 / 80630 操作日志 / 80930 打印设置
-- 后果：**普通租户（tenant_id≠1 系统租户）的非超管用户看不到这 10 页**，
--       叠加「设置权限码只授超管」⇒ 看不见 + 调不了（双阻断）。
--
-- ⚠️ dev 的 admin 账号同时命中 isSystemTenant 与 isSuperAdmin 双豁免（走早退分支取全量），
--    本地永远看不出来 —— 这也是历次 E2E 全绿的原因。
--
-- 注：全库 menu_level=3 原共 124 条，V11.499.0 处理分析模块 29 条、V11.500.0 处理 CRM、
--    V11.501.0 处理配送 20 条、V11.502.0 处理 HR 4 条；本条只处理设置模块这 10 条。
--
-- 【回滚】UPDATE sys_menu SET menu_level = 3
--         WHERE id IN (80610,80620,80621,80622,80623,80624,80625,80626,80630,80930);
UPDATE sys_menu SET menu_level = 0
 WHERE deleted = 0
   AND menu_level = 3
   AND id IN (80610, 80620, 80621, 80622, 80623, 80624, 80625, 80626, 80630, 80930);

-- ══════════════════════════════════════════════════════════════════════════
-- 二、`61203 账套操作` 内 sort 重号订正（P2-1）
-- ══════════════════════════════════════════════════════════════════════════
-- 实测 70560 系统重建 与 80630 操作日志 同为 sort=2（70561 系统任务为 3）⇒ 显示顺序不确定。
-- 按 id 顺序把操作日志放到系统任务之后（2 重建 → 3 任务 → 4 日志）。
-- 报告 §7.3 早已登记此项，§10.3 明写「未改」，本次一并订正。
--
-- 【回滚】UPDATE sys_menu SET sort = 2 WHERE id = 80630;
UPDATE sys_menu SET sort = 4
 WHERE deleted = 0 AND id = 80630 AND parent_id = 61203 AND sort = 2;

-- ══════════════════════════════════════════════════════════════════════════
-- 三、`804 我的已办` 的 component 去掉 `.vue` 后缀（P2-2）
-- ══════════════════════════════════════════════════════════════════════════
-- 同组 802/803 都不带后缀，仅 804 带（`views/workflow/task-management.vue`）。
-- 前端的归一化规则（dynamicRoutes.ts:891-917 去 `views/` 前缀与 `.vue` 后缀）本来就能兜住，
-- 故这是**一致性订正、非缺陷修复**；改成与同组一致，减少日后误判。
--
-- ⚠️ 不要动 80610 的 path（`set/workflow-designer`，无前导斜杠）：
--    全库 301 条有值 path 中只有 5 条带前导斜杠（801/802/803/804/80611），
--    无前导斜杠才是本项目主流写法；README §7.3 把它记成异常是记反了。
--
-- 【回滚】UPDATE sys_menu SET component = 'views/workflow/task-management.vue' WHERE id = 804;
UPDATE sys_menu SET component = 'views/workflow/task-management'
 WHERE deleted = 0 AND id = 804 AND component = 'views/workflow/task-management.vue';

-- ══════════════════════════════════════════════════════════════════════════
-- 四、给租户角色补授设置域权限码（P0-1 的第二半）
-- ══════════════════════════════════════════════════════════════════════════
-- 背景：设置域权限码此前**只授给了 SUPER_ADMIN**——
--   · `set:%` 18 条、`workflow:%` 19 条，`sys_role_permission` 里 role_id 全为 1；
--   · 叠加菜单可见性由权限码前缀派生（平台-AUTHZ-01，见 MenuPermissionDeriver）后，
--     26 个设置页面里非超管只剩「会计期间」1 个可用。
-- 授权口径与 V11.509.0（CRM/发票）一致，遵循两条业界惯例：
--   · 职责分离：系统管理员拿本租户的全部设置管理权；部门管理员只拿日常操作；
--   · 模块权益与角色权限分开：这里只表达「人能不能做」。
--
-- ✅ 本轮**已同时修掉**导致「非超管不可见」的另一半（见本迁移第一节：menu_level 3→0）。
--    只授码不改 menu_level，租户 2 的非超管仍然看不到那 10 页。
--
-- ⚠️ 刻意**不动** `E2E_T2_ADMIN`（租户 2 的测试账号，只持 4 条码）：
--    `tools/verify-module-authz.cjs` 等用它当「无码的非超管」探针来验证「注解已生效 ⇒ 403」，
--    给它授码会让该断言失效。
--
-- ⚠️ 刻意**不给** `set:rebuild:execute`（系统重建 = 按范围清空本租户业务数据）：
--    属危险操作，保留超管专属；若要放开，另开一条迁移并配套二次确认设计。
--
-- ⚠️ 刻意**不给** `workflow:callback-log:%`（tenant_id=0 的「平台与系统」码）：
--    那是平台侧回调日志监控（/api/workflow/callback-log/**），前端 0 引用，非租户管理面。

-- ── ① 系统管理员（SYSTEM_ADMIN，租户 1）：本租户设置域的全部管理权 ──────────
-- 覆盖 14 个页面的接口码：
--   菜单配置(set:menu-config) / 系统参数(system:config) / 审核设置(workflow:audit)
--   支付配置(payment:config) / 企业信息(set:company-info) / 应用中心(set:app-center)
--   库存期初(set:initial-stock) / 财务期初(set:initial-finance) / 会计期间(finance:period，已有)
--   辅助核算(finance:auxiliary) / 系统任务(set:system-task) / 操作日志(log:*)
--   打印设置(set:print-config) + 打印组 4 页(print:*) / 工作流 4 页(workflow:*)
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9800000 + row_number() OVER (ORDER BY p.id), r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND p.deleted = 0
  AND (
        p.permission_code LIKE 'set:%'
     OR p.permission_code LIKE 'workflow:%'
     OR p.permission_code LIKE 'system:config:%'
     OR p.permission_code LIKE 'payment:config:%'
     OR p.permission_code LIKE 'log:oper:%'
     OR p.permission_code LIKE 'log:login:%'
     OR p.permission_code LIKE 'log:audit:%'
     OR p.permission_code LIKE 'finance:auxiliary:%'
     OR p.permission_code LIKE 'print:%'
  )
  -- 危险操作保留超管专属
  AND p.permission_code <> 'set:rebuild:execute'
  -- 平台侧回调日志监控码不下发租户
  AND p.permission_code NOT LIKE 'workflow:callback-log:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ── ② 部门管理员（DEPT_ADMIN，租户 1）：只给日常操作 ────────────────────────
-- 逐条给出理由；**不含**任何设置项的管理（菜单/参数/企业信息/期初/重建/支付/打印模板链路客户端）。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9850000 + row_number() OVER (ORDER BY p.id), r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'DEPT_ADMIN'
  AND p.deleted = 0
  AND p.permission_code IN (
      -- 应用中心：查看本租户已开通模块（只读，无写入口）
      'set:app-center:view',
      -- 工作流：处理自己的待办与已办（审批是最典型的部门日常动作）
      'workflow:task:view', 'workflow:task:approve', 'workflow:task:transfer',
      -- 工作流：查看/发起/撤回自己发起的流程实例（不含 intervene 干预、不含 definition 定义维护）
      'workflow:instance:view', 'workflow:instance:diagram',
      'workflow:instance:start', 'workflow:instance:withdraw',
      -- 操作日志：只读查看与详情（不含 delete 清理、不含 export 导出）
      'log:oper:list', 'log:oper:detail',
      -- 打印任务：查看 + 执行打印链（不含任务取消 cancel、不含模板/链路/客户端管理）
      'print:task:list', 'print:task:detail', 'print:task:execute'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ══════════════════════════════════════════════════════════════════════════
-- 五、自检（迁移后手工执行核对，期望值见注释）
-- ══════════════════════════════════════════════════════════════════════════
-- ① 设置子树 menu_level 应全为 0：
--    SELECT menu_level, count(*) FROM sys_menu
--     WHERE deleted = 0 AND parent_id IN (61201,61202,61203,61204,61205,61206,61207,61208)
--     GROUP BY 1;                                        -- 期望：只有 menu_level=0
-- ② 各角色的设置域码数：
--    SELECT r.role_code, count(*) FROM sys_role_permission rp
--      JOIN sys_role r ON r.id = rp.role_id
--      JOIN sys_permission p ON p.id = rp.permission_id
--     WHERE p.permission_code ~ '^(set:|workflow:|system:config:|payment:config:|log:(oper|login|audit):|finance:auxiliary:|print:)'
--     GROUP BY r.role_code ORDER BY 1;
--    -- 期望：SUPER_ADMIN 全量、SYSTEM_ADMIN 全量（少 set:rebuild:execute 与 callback-log）、
--    --       DEPT_ADMIN 14 条、E2E_T2_ADMIN 0 条
-- ③ sort 重号应消除：
--    SELECT parent_id, sort, count(*) FROM sys_menu WHERE deleted = 0
--     GROUP BY 1,2 HAVING count(*) > 1;                   -- 期望：61203 那组不再出现
