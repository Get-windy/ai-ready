-- =============================================================================
-- 回填「应用中心」（设置 → 系统配置 → 应用中心，菜单 80625 / set:app-center）的租户模块开通记录
-- 2026-09-18
--
-- 背景（《应用中心开发文档》§7.1 / §9.2-P0① / §12-P0①）：
--   `sys_tenant_module` 实测 **0 行** → 页面核心区「已开通模块」在任何租户下都恒为空态
--   （前端 `a-empty description="暂无已开通模块"`，与「真的没开通任何模块」无法区分）。
--   表结构本身没问题（`V3.7.0` 建表时预置过 12 行，但 `tenant_id = 0`；`V5.0.0` 重建该表后
--   这些预置行未随迁，故为 0 行）。
--
-- 本次裁定（**只回填真实存在的模块，不发明任何模块**）：
--   数据源 = `sys_module`（系统模块注册表，本次实测 6 行：销售管理 sale / 采购管理 purchase /
--   仓储管理 warehouse / 财务管理 finance / 客户关系 crm / 营销管理 marketing）。
--   其中 `status = 1`（启用）的 5 个模块才回填；`营销管理`（id = 6）在注册表中 `status = 0`（停用），
--   **不回填开通记录**，如实保持「未开通」。
--   `module_code` / `module_name` 逐字取自 `sys_module` 的同名列，不做任何映射或改名。
--
--   为什么可以回填：这 5 个模块的菜单在本系统里**正在被各租户实际使用**（tenant-admin 域）；
--   即「模块已开通」是既成事实，只是授权记录从未落库。本次是把**缺失的记录补齐**，
--   而非给租户凭空发放权益。
--
-- 字段取值口径：
--   purchase_type = 'permanent' —— 本系统为本地部署买断制，注册表中无订阅/到期信息；
--                 该列有 CHECK 约束 `ck_module_purchase_type`，仅允许 permanent / auto_renew / manual。
--   expire_time   = NULL        —— permanent 类型无到期时间（表注释原文）。
--   status        = 0           —— 表注释原文「0-正常 1-停用」；`TenantModuleService#getValidModuleCodes`
--                 亦按 `status = 0` 取有效模块，口径一致。
--   tenant_id     = 各租户自身 id（本表**参与**租户隔离，不在 MyBatisPlusConfig.IGNORE_TENANT_TABLES 中）。
--
-- id 取 9000000000000000000 + 租户id * 100 + 模块id：
--   本表主键为雪花（MyBatis-Plus ASSIGN_ID，实测当前量级约 2.09e18），特意取 9.0e18 段避免撞号；
--   本次实测该表 0 行，区间必然空闲。
--
-- ⚠️ 已知遗留（不在本次改动范围，如实登记）：
--   前端 `stores/user.ts#loadValidModuleCodes` 用 `validModuleCodes` 做路由模块校验
--   （`hasValidModule` 取路由**首段**，如 `sales/order` → `sales`）。该处目前恒为空数组
--   （取 `res.data` 而 request 拦截器已拆包 → undefined），故路由校验**未生效**。
--   若将来启用该校验，须先把 `module_code`（注册表口径：sale / purchase / …）与
--   路由首段（sales / purchase / …）的口径统一，否则会误拦页面。
--
-- 幂等：WHERE NOT EXISTS + ON CONFLICT (id) DO NOTHING，可重复执行。
-- =============================================================================

INSERT INTO sys_tenant_module (id, tenant_id, deleted, create_time, update_time,
                               module_code, module_name, purchase_type, expire_time, status)
SELECT 9000000000000000000 + (t.id * 100) + m.id,
       t.id,
       0,
       now(),
       now(),
       m.module_code,
       m.module_name,
       'permanent',
       NULL,
       0
FROM sys_tenant t
CROSS JOIN sys_module m
WHERE t.deleted = 0
  AND t.id > 0
  AND m.deleted = 0
  AND m.status = 1
  AND NOT EXISTS (
      SELECT 1
      FROM sys_tenant_module x
      WHERE x.tenant_id = t.id
        AND x.module_code = m.module_code
        AND x.deleted = 0
  )
ON CONFLICT (id) DO NOTHING;
