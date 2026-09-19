-- =============================================================================
-- 补齐「代码引用但库中缺失」的 90 个权限码 + 关联超管角色
-- V11.423.0 · 2026-09-19（由 tools/gen-permission-seed.py 生成）
--
-- 【问题】鉴权取码 SQL（core/base/core-base/src/main/resources/mapper/SysUserMapper.xml:48-59）
--   走 `INNER JOIN sys_permission`，**权限码必须在 sys_permission 里有行**才能被任何人拿到。
--   实测（tools/audit-permission-codes.py）：代码引用 243 个码，库中 364 个，交叉命中仅 153 个
--   ⇒ 90 个码所对应的接口对**非超管用户一律 403**。超管走 UnifiedPermissionCacheService
--   的 ["*"] 通配，所以只有超管不受影响——这也掩盖了问题。
--
--   典型：erp:product:list 被引用 24 处、erp:product:update 12 处，
--   而库中 `erp:product%` 实测 0 行 ⇒ 商品管理整块对非超管不可用。
--   同理 dms:* 整族 23 个（配送模块）、platform:* 11 个（平台配置）。
--
-- 【字段口径】完全沿用 V11.419.0（同 V11.394.0 / V11.407.0 / V11.417.0）：
--   tenant_id=1 / parent_id=0 / deleted=0 / permission_type=3
--   api_path=NULL / method=NULL / visible=1 / status=0
--   sort 从 942 起递增（V11.419.0 用到 941）。
--
-- 【id 号段纪律】落地前实测（devdb，2026-09-19）：
--   SELECT count(*) FROM sys_permission      WHERE id BETWEEN 91603 AND 91692   → 0
--   SELECT count(*) FROM sys_role_permission WHERE id BETWEEN 9169100 AND 9169300 → 0
--   `sys_permission.id` / `sys_role_permission.id` 均无序列默认值，必须显式给值。
--   迁移版本号：实测该目录最大为 V11.422.0，故取 V11.423.0。
--
-- 【幂等】两张表均无 (业务列) 唯一约束 ⇒ 一律用 NOT EXISTS 守卫，重复执行安全。
--
-- 【本迁移不做的事】只登记权限码 + 关联超管。**不给普通角色授权**——
--   普通角色（SYSTEM_ADMIN/DEPT_ADMIN 等）该不该有这些权限属业务决策，
--   由租户在「权限配置」页自行勾选。这样本迁移不改变任何现有账号的实际权限，
--   只把「无法授予」变为「可以授予」。
--
-- 【回滚】
--   DELETE FROM sys_role_permission WHERE permission_id IN
--     (SELECT id FROM sys_permission WHERE id BETWEEN 91603 AND 91692);
--   DELETE FROM sys_permission WHERE id BETWEEN 91603 AND 91692;
-- =============================================================================

-- ── ① 90 个权限码（id 91603~91692，sort 942 起）──────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 91603, 1, 0, 0, now(), now(), '配送渠道新增', 'dms:channel:create', 3, NULL, NULL, 942, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:channel:create')
UNION ALL
SELECT 91604, 1, 0, 0, now(), now(), '配送渠道删除', 'dms:channel:delete', 3, NULL, NULL, 943, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:channel:delete')
UNION ALL
SELECT 91605, 1, 0, 0, now(), now(), '配送渠道编辑', 'dms:channel:update', 3, NULL, NULL, 944, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:channel:update')
UNION ALL
SELECT 91606, 1, 0, 0, now(), now(), '配送配置编辑', 'dms:config:update', 3, NULL, NULL, 945, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:config:update')
UNION ALL
SELECT 91607, 1, 0, 0, now(), now(), '配送派单指派', 'dms:dispatch:assign', 3, NULL, NULL, 946, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:assign')
UNION ALL
SELECT 91608, 1, 0, 0, now(), now(), '配送派单自动派单', 'dms:dispatch:auto', 3, NULL, NULL, 947, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:auto')
UNION ALL
SELECT 91609, 1, 0, 0, now(), now(), '配送派单候选', 'dms:dispatch:candidates', 3, NULL, NULL, 948, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:candidates')
UNION ALL
SELECT 91610, 1, 0, 0, now(), now(), '配送派单配置', 'dms:dispatch:config', 3, NULL, NULL, 949, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:config')
UNION ALL
SELECT 91611, 1, 0, 0, now(), now(), '配送派单围栏', 'dms:dispatch:fence', 3, NULL, NULL, 950, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:fence')
UNION ALL
SELECT 91612, 1, 0, 0, now(), now(), '配送派单改派', 'dms:dispatch:reassign', 3, NULL, NULL, 951, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:reassign')
UNION ALL
SELECT 91613, 1, 0, 0, now(), now(), '配送派单查看', 'dms:dispatch:view', 3, NULL, NULL, 952, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:dispatch:view')
UNION ALL
SELECT 91614, 1, 0, 0, now(), now(), '配送事件重试', 'dms:event:retry', 3, NULL, NULL, 953, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:event:retry')
UNION ALL
SELECT 91615, 1, 0, 0, now(), now(), '配送执行操作', 'dms:execution:operate', 3, NULL, NULL, 954, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:execution:operate')
UNION ALL
SELECT 91616, 1, 0, 0, now(), now(), '配送骑手审批', 'dms:rider:approve', 3, NULL, NULL, 955, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:rider:approve')
UNION ALL
SELECT 91617, 1, 0, 0, now(), now(), '配送骑手新增', 'dms:rider:create', 3, NULL, NULL, 956, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:rider:create')
UNION ALL
SELECT 91618, 1, 0, 0, now(), now(), '配送骑手删除', 'dms:rider:delete', 3, NULL, NULL, 957, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:rider:delete')
UNION ALL
SELECT 91619, 1, 0, 0, now(), now(), '配送骑手编辑', 'dms:rider:update', 3, NULL, NULL, 958, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:rider:update')
UNION ALL
SELECT 91620, 1, 0, 0, now(), now(), '配送签到审核', 'dms:sign:audit', 3, NULL, NULL, 959, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:sign:audit')
UNION ALL
SELECT 91621, 1, 0, 0, now(), now(), '配送签到提交', 'dms:sign:submit', 3, NULL, NULL, 960, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:sign:submit')
UNION ALL
SELECT 91622, 1, 0, 0, now(), now(), '配送轨迹上报', 'dms:tracking:report', 3, NULL, NULL, 961, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:tracking:report')
UNION ALL
SELECT 91623, 1, 0, 0, now(), now(), '配送车辆新增', 'dms:vehicle:create', 3, NULL, NULL, 962, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:vehicle:create')
UNION ALL
SELECT 91624, 1, 0, 0, now(), now(), '配送车辆删除', 'dms:vehicle:delete', 3, NULL, NULL, 963, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:vehicle:delete')
UNION ALL
SELECT 91625, 1, 0, 0, now(), now(), '配送车辆编辑', 'dms:vehicle:update', 3, NULL, NULL, 964, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'dms:vehicle:update')
UNION ALL
SELECT 91626, 1, 0, 0, now(), now(), 'ERP商品approval', 'erp:product:approval', 3, NULL, NULL, 965, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:approval')
UNION ALL
SELECT 91627, 1, 0, 0, now(), now(), 'ERP商品新增', 'erp:product:create', 3, NULL, NULL, 966, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:create')
UNION ALL
SELECT 91628, 1, 0, 0, now(), now(), 'ERP商品删除', 'erp:product:delete', 3, NULL, NULL, 967, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:delete')
UNION ALL
SELECT 91629, 1, 0, 0, now(), now(), 'ERP商品列表', 'erp:product:list', 3, NULL, NULL, 968, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:list')
UNION ALL
SELECT 91630, 1, 0, 0, now(), now(), 'ERP商品批量改价', 'erp:product:price-batch', 3, NULL, NULL, 969, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:price-batch')
UNION ALL
SELECT 91631, 1, 0, 0, now(), now(), 'ERP商品状态变更', 'erp:product:status', 3, NULL, NULL, 970, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:status')
UNION ALL
SELECT 91632, 1, 0, 0, now(), now(), 'ERP商品编辑', 'erp:product:update', 3, NULL, NULL, 971, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:update')
UNION ALL
SELECT 91633, 1, 0, 0, now(), now(), 'ERP商品查看', 'erp:product:view', 3, NULL, NULL, 972, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'erp:product:view')
UNION ALL
SELECT 91634, 1, 0, 0, now(), now(), '财务科目新增', 'finance:subject:create', 3, NULL, NULL, 973, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:subject:create')
UNION ALL
SELECT 91635, 1, 0, 0, now(), now(), '财务科目删除', 'finance:subject:delete', 3, NULL, NULL, 974, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:subject:delete')
UNION ALL
SELECT 91636, 1, 0, 0, now(), now(), '财务科目编辑', 'finance:subject:edit', 3, NULL, NULL, 975, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:subject:edit')
UNION ALL
SELECT 91637, 1, 0, 0, now(), now(), '日志操作统计', 'log:oper:stats', 3, NULL, NULL, 976, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'log:oper:stats')
UNION ALL
SELECT 91638, 1, 0, 0, now(), now(), '权限模板应用', 'permission-template:apply', 3, NULL, NULL, 977, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:apply')
UNION ALL
SELECT 91639, 1, 0, 0, now(), now(), '权限模板新增', 'permission-template:create', 3, NULL, NULL, 978, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:create')
UNION ALL
SELECT 91640, 1, 0, 0, now(), now(), '权限模板删除', 'permission-template:delete', 3, NULL, NULL, 979, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:delete')
UNION ALL
SELECT 91641, 1, 0, 0, now(), now(), '权限模板详情', 'permission-template:detail', 3, NULL, NULL, 980, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:detail')
UNION ALL
SELECT 91642, 1, 0, 0, now(), now(), '权限模板列表', 'permission-template:list', 3, NULL, NULL, 981, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:list')
UNION ALL
SELECT 91643, 1, 0, 0, now(), now(), '权限模板编辑', 'permission-template:update', 3, NULL, NULL, 982, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:update')
UNION ALL
SELECT 91644, 1, 0, 0, now(), now(), '权限模板状态更新', 'permission-template:update-status', 3, NULL, NULL, 983, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'permission-template:update-status')
UNION ALL
SELECT 91645, 1, 0, 0, now(), now(), '平台邮件配置', 'platform:mail:config', 3, NULL, NULL, 984, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:mail:config')
UNION ALL
SELECT 91646, 1, 0, 0, now(), now(), '平台邮件测试', 'platform:mail:test', 3, NULL, NULL, 985, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:mail:test')
UNION ALL
SELECT 91647, 1, 0, 0, now(), now(), '平台邮件编辑', 'platform:mail:update', 3, NULL, NULL, 986, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:mail:update')
UNION ALL
SELECT 91648, 1, 0, 0, now(), now(), '平台安全policy', 'platform:security:policy', 3, NULL, NULL, 987, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:security:policy')
UNION ALL
SELECT 91649, 1, 0, 0, now(), now(), '平台安全编辑', 'platform:security:update', 3, NULL, NULL, 988, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:security:update')
UNION ALL
SELECT 91650, 1, 0, 0, now(), now(), '平台短信配置', 'platform:sms:config', 3, NULL, NULL, 989, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:sms:config')
UNION ALL
SELECT 91651, 1, 0, 0, now(), now(), '平台短信测试', 'platform:sms:test', 3, NULL, NULL, 990, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:sms:test')
UNION ALL
SELECT 91652, 1, 0, 0, now(), now(), '平台短信编辑', 'platform:sms:update', 3, NULL, NULL, 991, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:sms:update')
UNION ALL
SELECT 91653, 1, 0, 0, now(), now(), '平台存储配置', 'platform:storage:config', 3, NULL, NULL, 992, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:storage:config')
UNION ALL
SELECT 91654, 1, 0, 0, now(), now(), '平台存储测试', 'platform:storage:test', 3, NULL, NULL, 993, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:storage:test')
UNION ALL
SELECT 91655, 1, 0, 0, now(), now(), '平台存储编辑', 'platform:storage:update', 3, NULL, NULL, 994, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'platform:storage:update')
UNION ALL
SELECT 91656, 1, 0, 0, now(), now(), '采购费用分摊取消', 'purchase:cost-sharing:cancel', 3, NULL, NULL, 995, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase:cost-sharing:cancel')
UNION ALL
SELECT 91657, 1, 0, 0, now(), now(), '采购费用分摊完成', 'purchase:cost-sharing:complete', 3, NULL, NULL, 996, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase:cost-sharing:complete')
UNION ALL
SELECT 91658, 1, 0, 0, now(), now(), '采购费用分摊新增', 'purchase:cost-sharing:create', 3, NULL, NULL, 997, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase:cost-sharing:create')
UNION ALL
SELECT 91659, 1, 0, 0, now(), now(), '采购费用分摊详情', 'purchase:cost-sharing:detail', 3, NULL, NULL, 998, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase:cost-sharing:detail')
UNION ALL
SELECT 91660, 1, 0, 0, now(), now(), '采购费用分摊列表', 'purchase:cost-sharing:list', 3, NULL, NULL, 999, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase:cost-sharing:list')
UNION ALL
SELECT 91661, 1, 0, 0, now(), now(), '采购费用分摊编辑', 'purchase:cost-sharing:update', 3, NULL, NULL, 1000, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase:cost-sharing:update')
UNION ALL
SELECT 91662, 1, 0, 0, now(), now(), '采购订单审批', 'purchase_order:approve', 3, NULL, NULL, 1001, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:approve')
UNION ALL
SELECT 91663, 1, 0, 0, now(), now(), '采购订单新增', 'purchase_order:create', 3, NULL, NULL, 1002, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:create')
UNION ALL
SELECT 91664, 1, 0, 0, now(), now(), '采购订单删除', 'purchase_order:delete', 3, NULL, NULL, 1003, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:delete')
UNION ALL
SELECT 91665, 1, 0, 0, now(), now(), '采购订单execute', 'purchase_order:execute', 3, NULL, NULL, 1004, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:execute')
UNION ALL
SELECT 91666, 1, 0, 0, now(), now(), '采购订单导出', 'purchase_order:export', 3, NULL, NULL, 1005, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:export')
UNION ALL
SELECT 91667, 1, 0, 0, now(), now(), '采购订单提交', 'purchase_order:submit', 3, NULL, NULL, 1006, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:submit')
UNION ALL
SELECT 91668, 1, 0, 0, now(), now(), '采购订单编辑', 'purchase_order:update', 3, NULL, NULL, 1007, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'purchase_order:update')
UNION ALL
SELECT 91669, 1, 0, 0, now(), now(), '角色继承manage', 'role-inheritance:manage', 3, NULL, NULL, 1008, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'role-inheritance:manage')
UNION ALL
SELECT 91670, 1, 0, 0, now(), now(), '角色继承查看', 'role-inheritance:view', 3, NULL, NULL, 1009, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'role-inheritance:view')
UNION ALL
SELECT 91671, 1, 0, 0, now(), now(), '销售订单列表', 'sale:order:list', 3, NULL, NULL, 1010, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sale:order:list')
UNION ALL
SELECT 91672, 1, 0, 0, now(), now(), '销售订单print', 'sale:order:print', 3, NULL, NULL, 1011, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sale:order:print')
UNION ALL
SELECT 91673, 1, 0, 0, now(), now(), '销售Promotion新增', 'sale:promotion:create', 3, NULL, NULL, 1012, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sale:promotion:create')
UNION ALL
SELECT 91674, 1, 0, 0, now(), now(), '销售Promotion删除', 'sale:promotion:delete', 3, NULL, NULL, 1013, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sale:promotion:delete')
UNION ALL
SELECT 91675, 1, 0, 0, now(), now(), '销售Promotionpublish', 'sale:promotion:publish', 3, NULL, NULL, 1014, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sale:promotion:publish')
UNION ALL
SELECT 91676, 1, 0, 0, now(), now(), '销售Promotion编辑', 'sale:promotion:update', 3, NULL, NULL, 1015, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sale:promotion:update')
UNION ALL
SELECT 91677, 1, 0, 0, now(), now(), '系统Agentactivate', 'system:agent:activate', 3, NULL, NULL, 1016, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:activate')
UNION ALL
SELECT 91678, 1, 0, 0, now(), now(), '系统Agentdeactivate', 'system:agent:deactivate', 3, NULL, NULL, 1017, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:deactivate')
UNION ALL
SELECT 91679, 1, 0, 0, now(), now(), '系统Agent删除', 'system:agent:delete', 3, NULL, NULL, 1018, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:delete')
UNION ALL
SELECT 91680, 1, 0, 0, now(), now(), '系统Agent详情', 'system:agent:detail', 3, NULL, NULL, 1019, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:detail')
UNION ALL
SELECT 91681, 1, 0, 0, now(), now(), '系统Agent列表', 'system:agent:list', 3, NULL, NULL, 1020, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:list')
UNION ALL
SELECT 91682, 1, 0, 0, now(), now(), '系统Agentregister', 'system:agent:register', 3, NULL, NULL, 1021, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:register')
UNION ALL
SELECT 91683, 1, 0, 0, now(), now(), '系统Agent编辑', 'system:agent:update', 3, NULL, NULL, 1022, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:agent:update')
UNION ALL
SELECT 91684, 1, 0, 0, now(), now(), '系统Datasource新增', 'system:datasource:create', 3, NULL, NULL, 1023, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:datasource:create')
UNION ALL
SELECT 91685, 1, 0, 0, now(), now(), '系统Datasource删除', 'system:datasource:delete', 3, NULL, NULL, 1024, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:datasource:delete')
UNION ALL
SELECT 91686, 1, 0, 0, now(), now(), '系统Datasource列表', 'system:datasource:list', 3, NULL, NULL, 1025, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:datasource:list')
UNION ALL
SELECT 91687, 1, 0, 0, now(), now(), '系统Datasource测试', 'system:datasource:test', 3, NULL, NULL, 1026, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:datasource:test')
UNION ALL
SELECT 91688, 1, 0, 0, now(), now(), '系统Datasource编辑', 'system:datasource:update', 3, NULL, NULL, 1027, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:datasource:update')
UNION ALL
SELECT 91689, 1, 0, 0, now(), now(), '系统Permission状态更新', 'system:permission:update-status', 3, NULL, NULL, 1028, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:permission:update-status')
UNION ALL
SELECT 91690, 1, 0, 0, now(), now(), '系统Sessiondisable', 'system:session:disable', 3, NULL, NULL, 1029, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:session:disable')
UNION ALL
SELECT 91691, 1, 0, 0, now(), now(), '系统Sessionkickout', 'system:session:kickout', 3, NULL, NULL, 1030, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:session:kickout')
UNION ALL
SELECT 91692, 1, 0, 0, now(), now(), '系统Session查看', 'system:session:view', 3, NULL, NULL, 1031, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:session:view');

-- ── ② 关联给超级管理员（role_id = 1），口径同 V11.394.0 / V11.407.0 / V11.417.0 / V11.419.0 ──
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9169100 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 91603 AND 91692
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
