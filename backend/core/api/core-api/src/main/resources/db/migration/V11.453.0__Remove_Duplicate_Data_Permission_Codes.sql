-- ══════════════════════════════════════════════════════════════════════════════
-- E-02 批次 5：删除 9 条「重影权限码」（数据权限域 + 租户配置）
--
-- 本批是 E-02 的最后一类：**同一件事起了两个名字**。与批次 3/4 的"多余动作码"不同，
-- 这里是**整族重影** —— 一个码族真实在用，另一个码族只是个名字。
--
--   ① data-permission:*  ×8  ←→  system:data-scope:*（真实在用）
--      · 真实实现：core-base `SysDataScopeController`（@ /api/data-scope）
--        + 前端 `views/system/role/components/RoleDataScopeTab.vue`，挂的是
--        system:data-scope:{list,assign,delete}；
--      · data-permission:* 由 V11.407.0 随「系统模块权限」一起种下，但**从来没有控制器、
--        没有页面、没有任何消费方**，api_path 也是空的 —— 同名异写，留着只会让人在矩阵里
--        勾到一个永远不会生效的"数据权限"。
--
--   ② tenant:config  ←→  system:tenant:query（真实在用）
--      · `GET /api/tenant/{id}/config` 真实注解是 system:tenant:query
--        （见 TenantController#getConfig）；tenant:config 登记的 api_path
--        `/api/tenant/*/config` 是个通配写法，没有对应的注解消费方。
--
-- ⚠️ 与批次 4 同样的克制：**"已定义但功能未建"的码不在这批删** ——
--    例如 product:cost:view、product:retail-price:view（字段级可见性）、
--    doc:reverse/doc:void（单据反冲/作废）、sale:settle:force（强制结账）等 20 条，
--    它们各自是一个**尚未实现的独立能力**，不是别人的重影。删掉等于销毁路线图，
--    保留则在前端矩阵里显示为「未生效（标灰）」—— 那正是它们此刻的真实状态。
--    逐个裁定记入 MASTER_TODO。
--
-- 删前已核对（真库）：9 条均为叶子节点；仅「超级管理员」持有；
-- 全仓引用只落在种子 SQL / 审计 JSON（登记处本身），无一处活跃代码。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 软删 9 条重影码 ──
UPDATE sys_permission
SET deleted = 1
WHERE deleted = 0
  AND permission_code IN (
    -- ① 数据权限：system:data-scope:* 才是正主
    'data-permission:assign', 'data-permission:check', 'data-permission:create',
    'data-permission:delete', 'data-permission:list', 'data-permission:update',
    'data-permission:update-status', 'data-permission:view',
    -- ② 租户配置：system:tenant:query 才是正主
    'tenant:config'
  );

-- ── 2. 清掉这些码残留的角色授权行 ──
DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission
    WHERE deleted = 1
      AND permission_code IN (
        'data-permission:assign', 'data-permission:check', 'data-permission:create',
        'data-permission:delete', 'data-permission:list', 'data-permission:update',
        'data-permission:update-status', 'data-permission:view',
        'tenant:config'
      )
);

-- ── 3. 自检 ──
DO $$
DECLARE
    left_zombies int;
    live_guards  int;
    kept         int;
    dup          int;
BEGIN
    -- 3.1 9 条必须都已下架
    SELECT count(*) INTO left_zombies
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN (
        'data-permission:assign', 'data-permission:check', 'data-permission:create',
        'data-permission:delete', 'data-permission:list', 'data-permission:update',
        'data-permission:update-status', 'data-permission:view',
        'tenant:config'
      );
    IF left_zombies > 0 THEN
        RAISE EXCEPTION '批次5清理后仍有 % 条重影码未下架', left_zombies;
    END IF;

    -- 3.2 正主族必须完整（数据权限三道门，少一道就有页面进不去）
    SELECT count(*) INTO live_guards
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN ('system:data-scope:list', 'system:data-scope:assign',
                              'system:data-scope:delete', 'system:tenant:query');
    IF live_guards <> 4 THEN
        RAISE EXCEPTION '数据权限域正主码异常：期望 4 条，实际 % 条', live_guards;
    END IF;

    -- 3.3 批次 5 接线依赖的 6 个数据导入码必须还在（刚挂到 SyncConfigController 九个端点）
    SELECT count(*) INTO kept
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN ('system:dataimport:list', 'system:dataimport:create',
                              'system:dataimport:update', 'system:dataimport:delete',
                              'system:dataimport:test', 'system:dataimport:sync');
    IF kept <> 6 THEN
        RAISE EXCEPTION '数据导入域码缺失：期望 6 条，实际 % 条', kept;
    END IF;

    -- 3.4 不得出现同码多行
    SELECT count(*) INTO dup
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup > 0 THEN
        RAISE EXCEPTION '清理后出现 % 组同码多行，请人工核查', dup;
    END IF;
END $$;
