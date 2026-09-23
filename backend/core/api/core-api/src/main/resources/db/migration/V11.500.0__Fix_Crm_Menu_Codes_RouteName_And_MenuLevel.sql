-- 2026-09-23 CRM 模块收尾审计修复（见 I:/AI-Ready/CRM_MODULE_AUDIT_20260923.md §2.4 / §4.3）
--
-- ══════════════════════════════════════════════════════════════════════════
-- 一、10 条 CRM 菜单的 menu_code 前缀对齐（「菜单人人可见、点进去 403」）
-- ══════════════════════════════════════════════════════════════════════════
-- menu_code 参与菜单可见性派生（SysMenuServiceImpl.getUserMegaMenus + MenuPermissionDeriver）：
--   ① 库中存在以 menu_code 为前缀的权限码、且用户一个都不持有 ⇒ 菜单隐藏；
--   ② 库中**不存在**任何以该 menu_code 为前缀的权限码 ⇒ 菜单保持可见（fail-open 过渡口径）。
-- 所以 menu_code 写错的后果不是"少一个菜单"，而是**该页对所有登录用户恒可见、无法按角色收窄**，
-- 而它调用的接口仍受 @SaCheckPermission 保护 ⇒ 点进去 403 / 空数据。
--
-- 实测（2026-09-23，SQL：NOT EXISTS(权限码 = menu_code OR 权限码 LIKE menu_code||':%')）：
--   CRM 相关共 10 条命中，占全模块 17 个页面的一半以上：
--     80201 客户跟进 / 70303 客户分级 / 70311 线索转化 / 70321 商机阶段 / 70341 合同审批
--     70360 销售漏斗 / 70361 客户分析 / 70001 拜访规划 / 70002 拜访执行 / 70003 拜访检视
--
-- 修法与依据：**只把 menu_code 改成该页真实调用的接口所需权限码的前缀**，
--   不新增任何权限码、不改 path/component、不删菜单（与 V11.498.0 采购域同一做法）。
--   逐条依据（均已核对「页面实际请求的接口」→「该接口上的 @SaCheckPermission」）：
--     80201 客户跟进   → /api/crm/followUp/page 需 crm:follow-up:list   ⇒ menu_code = crm:follow-up
--                        （原 crm:customer-follow 与后端资源名 follow-up 词根不一致，是本题根因）
--     70303 客户分级   → /api/customer/page 需 crm:customer:list、
--                        PUT /api/customer/{id} 需 crm:customer:update   ⇒ menu_code = crm:customer
--     70361 客户分析   → /api/customer/export 需 crm:customer:list       ⇒ menu_code = crm:customer
--     70311 线索转化   → /api/crm/lead/page 需 crm:lead:view、
--                        /batch-convert 需 crm:lead:batchconvert         ⇒ menu_code = crm:lead
--     70321 商机阶段   → /api/crm/opportunity/{page,advance,win,lose} 需 crm:opportunity:{view,edit}
--                                                                        ⇒ menu_code = crm:opportunity
--     70360 销售漏斗   → /api/crm/opportunity/export 需 crm:opportunity:view  ⇒ menu_code = crm:opportunity
--     70341 合同审批   → /api/crm/contract/page 需 crm:contract:view、
--                        /{id}/approve|reject 需 crm:contract:approve     ⇒ menu_code = crm:contract
--     70001/70002/70003 拜访规划|执行|检视 → /api/crm/visit/** 需 crm:visit:* ⇒ menu_code = crm:visit
--                        （原 menu_code=sales:visit-plan|exec|review 名实不符，后端与前端 API 全在 crm 域；
--                          《拜访规划/执行/检视开发文档》§12 与 CRM README §7 均建议收敛为 crm:visit-*）
--
-- ⚠️ 改 menu_code 会带来**前端路由名冲突**：dynamicRoutes.ts:966 的路由名规则是
--    `name: menu.routeName || menu.menuCode`，而本批菜单 route_name 全为空
--    ⇒ 改动后 80200/70303/70361 会同时叫 `crm:customer`（同理 lead/opportunity/contract 各有 2~3 条），
--    Vue Router 同名路由**后者覆盖前者** ⇒ 先注册的那条永远 404（症状隐蔽：URL 不变、无 4xx）。
--    故本迁移**同步为每条受影响菜单补一个唯一 route_name**（沿用库中既有风格：大驼峰），
--    用 `WHERE route_name IS NULL OR route_name = ''` 保证幂等且不覆盖已有值。
--
-- ══════════════════════════════════════════════════════════════════════════
-- 二、CRM 6 条叶子菜单 menu_level 3 → 0
-- ══════════════════════════════════════════════════════════════════════════
-- SysMenuServiceImpl 对「非系统租户且非超管」强制 `wrapper.eq(SysMenu::getMenuLevel, 0)`
-- ⇒ menu_level = 3 的菜单**租户侧普通用户根本看不到**（超管不受限，所以本地调试发现不了）。
-- CRM 的 80200/80201/80202/80210/80220/80230 六条正是 3；其余 11 条 CRM 叶子与 8 个分组均为 0。
-- menu_level 在 mega-menu-redesign.md 的字段注释与菜单管理表单里只有 0=租户级 / 1=系统级，
-- 3 是文档与前端都不承认的取值（V11.499.0 已按同一理由把分析模块 29 条 3→0，并在注释里
-- 明确把「crm 6 条」留给 CRM 审计处理 —— 即本迁移）。
--
-- 不删任何菜单、不改任何 path/component。

-- ── ① menu_code 前缀对齐（同时补唯一 route_name）────────────────────────

UPDATE sys_menu SET menu_code = 'crm:follow-up', update_time = CURRENT_TIMESTAMP
 WHERE id = 80201 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:customer', update_time = CURRENT_TIMESTAMP
 WHERE id = 70303 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:customer', update_time = CURRENT_TIMESTAMP
 WHERE id = 70361 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:lead', update_time = CURRENT_TIMESTAMP
 WHERE id = 70311 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:opportunity', update_time = CURRENT_TIMESTAMP
 WHERE id = 70321 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:opportunity', update_time = CURRENT_TIMESTAMP
 WHERE id = 70360 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:contract', update_time = CURRENT_TIMESTAMP
 WHERE id = 70341 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'crm:visit', update_time = CURRENT_TIMESTAMP
 WHERE id IN (70001, 70002, 70003) AND deleted = 0;

-- ── ② 受影响菜单补唯一 route_name（防「同名路由后者覆盖前者」）──────────
-- 覆盖范围 = 改动后 menu_code 会重复的**全部**菜单（含原本就重复的 80200/80210/80220/80230）。

UPDATE sys_menu SET route_name = 'CrmCustomer'         WHERE id = 80200 AND (route_name IS NULL OR route_name = ''); -- crm:customer
UPDATE sys_menu SET route_name = 'CrmCustomerFollow'   WHERE id = 80201 AND (route_name IS NULL OR route_name = ''); -- crm:follow-up
UPDATE sys_menu SET route_name = 'CrmCustomerGrade'    WHERE id = 70303 AND (route_name IS NULL OR route_name = ''); -- crm:customer
UPDATE sys_menu SET route_name = 'CrmCustomerAnalysis' WHERE id = 70361 AND (route_name IS NULL OR route_name = ''); -- crm:customer
UPDATE sys_menu SET route_name = 'CrmLead'             WHERE id = 80210 AND (route_name IS NULL OR route_name = ''); -- crm:lead
UPDATE sys_menu SET route_name = 'CrmLeadConvert'      WHERE id = 70311 AND (route_name IS NULL OR route_name = ''); -- crm:lead
UPDATE sys_menu SET route_name = 'CrmOpportunity'      WHERE id = 80220 AND (route_name IS NULL OR route_name = ''); -- crm:opportunity
UPDATE sys_menu SET route_name = 'CrmOpportunityStage' WHERE id = 70321 AND (route_name IS NULL OR route_name = ''); -- crm:opportunity
UPDATE sys_menu SET route_name = 'CrmFunnel'           WHERE id = 70360 AND (route_name IS NULL OR route_name = ''); -- crm:opportunity
UPDATE sys_menu SET route_name = 'CrmContract'         WHERE id = 80230 AND (route_name IS NULL OR route_name = ''); -- crm:contract
UPDATE sys_menu SET route_name = 'CrmContractApproval' WHERE id = 70341 AND (route_name IS NULL OR route_name = ''); -- crm:contract
UPDATE sys_menu SET route_name = 'CrmVisitPlan'        WHERE id = 70001 AND (route_name IS NULL OR route_name = ''); -- crm:visit
UPDATE sys_menu SET route_name = 'CrmVisitExec'        WHERE id = 70002 AND (route_name IS NULL OR route_name = ''); -- crm:visit
UPDATE sys_menu SET route_name = 'CrmVisitReview'      WHERE id = 70003 AND (route_name IS NULL OR route_name = ''); -- crm:visit

-- ── ③ menu_level 3 → 0（租户侧普通用户可见）────────────────────────────

UPDATE sys_menu SET menu_level = 0, update_time = CURRENT_TIMESTAMP
 WHERE menu_level = 3 AND deleted = 0
   AND id IN (80200, 80201, 80202, 80210, 80220, 80230);

-- ── ④ 自检（对本迁移的断言，跑完应返回 0 行）────────────────────────────
-- 迁移工具不执行断言，这里保留 SQL 供复核：
--   SELECT id, menu_name, menu_code FROM sys_menu
--    WHERE menu_type = 1 AND deleted = 0
--      AND (path LIKE 'crm/%' OR path LIKE 'sales/visit-%')
--      AND NOT EXISTS (SELECT 1 FROM sys_permission p
--                       WHERE p.status = 0 AND p.deleted = 0
--                         AND (p.permission_code = sys_menu.menu_code
--                              OR p.permission_code LIKE sys_menu.menu_code || ':%'));
