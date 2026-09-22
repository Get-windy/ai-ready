-- 平台扩展域（core-platform）权限码种子（E-01 coreplatform 批次，2026-09-21）
--
-- 【与"零码模块"种子的区别】core-platform **是零码模块**：`automation:` / `custom-field:` /
--   `kanban:` 三个前缀在 sys_permission 里**一条都没有**（同目录的 RecordRuleController 用的是
--   既有的 `tenant-admin:record-rule:*`，不在本次补注解范围内）。本迁移 28 个码全部是新建。
--   VALUES 占满 122000~122027 共 28 个槽位（按码名排序位置化分配），实际落库 28 个。
--
-- 【为什么必须补】E-01 要给这三个裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['coreplatform']`：
--   · 三个控制器的**类级路径都没有资源段**（`/api/automation`、`/api/custom-field`、
--     `/api/custom-field-value`、`/api/kanban`）⇒ 用 base_overrides 显式给出「域 + 资源」，
--     否则生成器会拿方法路径首段当资源名，产出 `custom-field:model:*`、`kanban:data:*`、
--     `kanban:sync:*` 这类型碎片码。
--   · **看板的 列/卡片 不并进 board**：三者各自有完整增删改查，合并后"删卡片"与"删看板列"
--     会共用一个开关（粒度错位）⇒ 拆成 `kanban:board|column|card:*`。
--   · 字段分组同理独立成 `custom-field:group:*`（组本身有增/删/查）。
--   · 动作词修正：启用/停用→update（不是 create）、预览/校验类端点归 view/check、
--     看板"从模型同步"归 execute。
--   · ⚠️ 本模块四个 base **全配了 base_overrides** ⇒ code_rules 一律写**完整码**：
--     `@动作词` 简写只替换动作词、域与资源仍走路径推导，会**绕过 overrides** 产出碎片码
--     （erppricing / erpobserv 批次已踩过，记此备注）。
--
-- 【本批有意排除的端点 —— 共 4 个，不加任何权限码】
--   · `/api/automation/trigger/{create,write,delete,scheduled}`（4）：
--     方法体是 `ruleService.executeOnCreate(modelName, recordId, values)` 这类 **ORM 事件钩子**
--     （Odoo 风格），设计上由业务层/调度器触发，不是"用户点出来的功能"；全仓 grep
--     `trigger/create` / `executeOnCreate` 除本控制器外**没有任何调用方**。
--     给它挂"某个角色的权限码"没有语义 —— 谁该拥有"记录被创建"的权限？
--     （正确修法是内部服务调用鉴权，不是权限码。）
--
-- 【id 号段】权限码 122000 起（槽位 22）、角色关联 9720000 起，执行前实测两段均为空。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (122000, '平台扩展自动化规则新增', 'automation:rule:create', '/api/automation', 'POST', 1200),
 (122001, '平台扩展自动化规则删除', 'automation:rule:delete', '/api/automation/{id}', 'DELETE', 1201),
 (122002, '平台扩展自动化规则详情', 'automation:rule:detail', '/api/automation/{id}', 'GET', 1202),
 (122003, '平台扩展自动化规则查询', 'automation:rule:list', '/api/automation/model/{modelName}', 'GET', 1203),
 (122004, '平台扩展自动化规则编辑', 'automation:rule:update', '/api/automation/{id}', 'PUT', 1204),
 (122005, '平台扩展自定义字段校验', 'custom-field:field:check', '/api/custom-field/validate', 'GET', 1205),
 (122006, '平台扩展自定义字段新增', 'custom-field:field:create', '/api/custom-field', 'POST', 1206),
 (122007, '平台扩展自定义字段删除', 'custom-field:field:delete', '/api/custom-field/{id}', 'DELETE', 1207),
 (122008, '平台扩展自定义字段详情', 'custom-field:field:detail', '/api/custom-field/{id}', 'GET', 1208),
 (122009, '平台扩展自定义字段查询', 'custom-field:field:list', '/api/custom-field/model/{modelName}', 'GET', 1209),
 (122010, '平台扩展自定义字段编辑', 'custom-field:field:update', '/api/custom-field/{id}', 'PUT', 1210),
 (122011, '平台扩展字段分组新增', 'custom-field:group:create', '/api/custom-field/group', 'POST', 1211),
 (122012, '平台扩展字段分组删除', 'custom-field:group:delete', '/api/custom-field/group/{id}', 'DELETE', 1212),
 (122013, '平台扩展字段分组查询', 'custom-field:group:list', '/api/custom-field/group/model/{modelName}', 'GET', 1213),
 (122014, '平台扩展字段值新增', 'custom-field:value:create', '/api/custom-field-value/save', 'POST', 1214),
 (122015, '平台扩展字段值删除', 'custom-field:value:delete', '/api/custom-field-value/delete', 'DELETE', 1215),
 (122016, '平台扩展字段值查看', 'custom-field:value:view', '/api/custom-field-value/get', 'GET', 1216),
 (122017, '平台扩展看板执行', 'kanban:board:execute', '/api/kanban/{modelName}/sync', 'POST', 1217),
 (122018, '平台扩展看板查询', 'kanban:board:list', '/api/kanban/{modelName}/data', 'GET', 1218),
 (122019, '平台扩展看板卡片新增', 'kanban:card:create', '/api/kanban/card', 'POST', 1219),
 (122020, '平台扩展看板卡片删除', 'kanban:card:delete', '/api/kanban/card/{id}', 'DELETE', 1220),
 (122021, '平台扩展看板卡片详情', 'kanban:card:detail', '/api/kanban/{modelName}/record/{recordId}/card', 'GET', 1221),
 (122022, '平台扩展看板卡片查询', 'kanban:card:list', '/api/kanban/column/{columnId}/cards', 'GET', 1222),
 (122023, '平台扩展看板卡片编辑', 'kanban:card:update', '/api/kanban/card/{id}', 'PUT', 1223),
 (122024, '平台扩展看板列新增', 'kanban:column:create', '/api/kanban/column', 'POST', 1224),
 (122025, '平台扩展看板列删除', 'kanban:column:delete', '/api/kanban/column/{id}', 'DELETE', 1225),
 (122026, '平台扩展看板列查询', 'kanban:column:list', '/api/kanban/{modelName}/columns', 'GET', 1226),
 (122027, '平台扩展看板列编辑', 'kanban:column:update', '/api/kanban/column/{id}', 'PUT', 1227)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9720000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 122000 AND 122028
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
