-- ══════════════════════════════════════════════════════════════════════════════
-- E-02 批次 4：删除 32 条「僵尸权限码」（核心域 + CRM + 财务遗留）
--
-- 本批全部是**重影/残留**，没有一条是"端点缺注解"——缺注解的那部分在同批用
-- 补 @SaCheckPermission 的方式解决（见下方 §2 说明），不在这里。
--
-- 两类出处：
--   ① **裸域 vs system: 域重影**（16 条）：E-04 按模块批量造码时，又造了一套不带
--      system: 前缀的裸域码，而真正在把门的是既有那套：
--        permission:* / role:* / user:*  ←→  system:permission:* / system:role:* / system:user:*
--        tenant:*                        ←→  system:tenant:*
--      例：RoleBillTypeController 用的是 system:role:detail / system:role:assign-permission；
--          PermissionController 用的是 system:permission:view / system:role:assign / ...，
--          裸域那套零消费方 ⇒ 删。
--   ② **同族内的多余动作码**（16 条）：
--        · erp:expense:application:approve / :statistics:refresh —— 真正在用的是
--          erp:expense:approval:process（批次 4 已挂到 POST /approval/process）
--        · erp:expense:approval:query —— 真正在用的是 erp:expense:statistics:list
--          （批次 4 已挂到查费用分析的 matrix/detail/partner）
--        · finance:balance:view —— 正主是 finance:partner-balance:view（批次 4 已挂到
--          往来余额表三个读端点）
--        · finance:receivable:{list,query,export,edit} —— ReceivableController 的读
--          全部用 finance:receivable:view，写用 create/write-off/bad-debt/delete ⇒ 这四条多余
--        · doc:date:edit / doc:unapprove —— 库里没有"改单据日期""反审核"这两个动作的端点
--        · crm 6 条（create / refresh / contract:refresh / contract:batchapprove /
--          opportunity:reset / opportunity:detailrefresh）—— CRM 走
--          CrmPermissions.require(...) 显式校验，实际只用了 crm:contract:{edit,approve}
--          与 crm:customer:{list,update,delete}、crm:lead:view、crm:opportunity:{create,view}，
--          这 6 条无任何调用点。
--
-- ⚠️ **有意保留的一条**：`finance:other-income-doc:approve` 本也在候选里（端点只有
--    confirm，用的是 finance:other-income-doc:update），但它**被「部门管理员」「系统管理员」
--    真实持有** —— 是种子有意的授权，不是随手勾的。删掉会静默回收这些角色的授权，
--    而它究竟该"删码"还是"补一个审批环节"属产品决策，故留待确认，仍会显示为「未生效（标灰）」。
--
-- 删前已核对（真库）：32 条均为叶子节点（无父子引用）；其余 31 条仅「超级管理员」持有；
-- 全仓引用只落在种子 SQL / 对标文档 / 审计 JSON（登记处本身），无一处活跃代码。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 软删 32 条僵尸码 ──
UPDATE sys_permission
SET deleted = 1
WHERE deleted = 0
  AND permission_code IN (
    -- ① 裸域 vs system: 域重影（16 条）
    'permission:create', 'permission:delete', 'permission:list', 'permission:update',
    'role:create', 'role:delete', 'role:list', 'role:update',
    'user:create', 'user:delete', 'user:list', 'user:update',
    'tenant:create', 'tenant:delete', 'tenant:list', 'tenant:update',
    -- ② 同族内的多余动作码（16 条）
    'erp:expense:application:approve', 'erp:expense:statistics:refresh', 'erp:expense:approval:query',
    'finance:balance:view',
    'finance:receivable:list', 'finance:receivable:query',
    'finance:receivable:export', 'finance:receivable:edit',
    'doc:date:edit', 'doc:unapprove',
    'crm:create', 'crm:refresh', 'crm:contract:refresh',
    'crm:contract:batchapprove', 'crm:opportunity:reset', 'crm:opportunity:detailrefresh'
  );

-- ── 2. 清掉这些码残留的角色授权行 ──
DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission
    WHERE deleted = 1
      AND permission_code IN (
        'permission:create', 'permission:delete', 'permission:list', 'permission:update',
        'role:create', 'role:delete', 'role:list', 'role:update',
        'user:create', 'user:delete', 'user:list', 'user:update',
        'tenant:create', 'tenant:delete', 'tenant:list', 'tenant:update',
        'erp:expense:application:approve', 'erp:expense:statistics:refresh', 'erp:expense:approval:query',
        'finance:balance:view',
        'finance:receivable:list', 'finance:receivable:query',
        'finance:receivable:export', 'finance:receivable:edit',
        'doc:date:edit', 'doc:unapprove',
        'crm:create', 'crm:refresh', 'crm:contract:refresh',
        'crm:contract:batchapprove', 'crm:opportunity:reset', 'crm:opportunity:detailrefresh'
      )
);

-- ── 3. 自检 ──
DO $$
DECLARE
    left_zombies int;
    live_guards  int;
    dup          int;
    kept         int;
BEGIN
    -- 3.1 32 条必须都已下架
    SELECT count(*) INTO left_zombies
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN (
        'permission:create', 'permission:delete', 'permission:list', 'permission:update',
        'role:create', 'role:delete', 'role:list', 'role:update',
        'user:create', 'user:delete', 'user:list', 'user:update',
        'tenant:create', 'tenant:delete', 'tenant:list', 'tenant:update',
        'erp:expense:application:approve', 'erp:expense:statistics:refresh', 'erp:expense:approval:query',
        'finance:balance:view',
        'finance:receivable:list', 'finance:receivable:query',
        'finance:receivable:export', 'finance:receivable:edit',
        'doc:date:edit', 'doc:unapprove',
        'crm:create', 'crm:refresh', 'crm:contract:refresh',
        'crm:contract:batchapprove', 'crm:opportunity:reset', 'crm:opportunity:detailrefresh'
      );
    IF left_zombies > 0 THEN
        RAISE EXCEPTION '批次4清理后仍有 % 条僵尸码未下架', left_zombies;
    END IF;

    -- 3.2 批次 4 接线时依赖的「正主码」必须一条不少（少一条，对应页面就没人能进）
    SELECT count(*) INTO live_guards
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN ('system:permission:view', 'system:role:assign-permission',
                              'erp:expense:approval:process', 'erp:expense:statistics:list',
                              'finance:partner-balance:view', 'finance:receivable:view',
                              'purchase:price:edit', 'sale:price:edit',
                              'crm:contract:edit', 'crm:contract:approve');
    IF live_guards <> 10 THEN
        RAISE EXCEPTION '批次4在役权限码异常：期望 10 条正主码，实际 % 条', live_guards;
    END IF;

    -- 3.3 有意保留的那一条必须还在（防止后来者"顺手"删掉它）
    SELECT count(*) INTO kept
    FROM sys_permission
    WHERE deleted = 0 AND permission_code = 'finance:other-income-doc:approve';
    IF kept <> 1 THEN
        RAISE EXCEPTION 'finance:other-income-doc:approve 被误删：它被部门管理员/系统管理员真实持有，需产品确认后才能处置';
    END IF;

    -- 3.4 不得出现同码多行
    SELECT count(*) INTO dup
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup > 0 THEN
        RAISE EXCEPTION '清理后出现 % 组同码多行，请人工核查', dup;
    END IF;
END $$;
