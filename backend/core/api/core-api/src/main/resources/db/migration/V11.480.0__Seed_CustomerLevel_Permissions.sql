-- 客户等级（party:customer-level）权限码种子（E-01 partnerlevel 批次，2026-09-21）
--
-- 【本批范围】`erp-partner` 的 `cn.aiedge.erp.customer` 包 —— 它此前不在任何模块的 dirs 里
--   （`MODULES['party']` 的 dirs 是 `.../erp/party/controller`），故 `CustomerLevelController`
--   从未被任何批次覆盖。
--
-- 【交付判断】该控制器**每个方法第一行**都是 `StpUtil.getLoginIdAsLong()`（当 tenantId 用）
--   ⇒ 依赖 Sa-Token 会话，是后台功能，正常补码（不是设备/匿名接口）。
--
-- 【域口径：不新建域、不复用同义码】类级路径 `/api/erp/customer/level` 推导出的
--   「域:资源」正是 `party:customer-level` —— 与库中**既有**的 `party:customer-region:*`
--   （挂 `/api/erp/customer/region`）同族、同命名风格，故**不加任何 override**；
--   `party:` 已归属 master-data，**无需**新增前缀映射行。
--
-- 【只补不删】本文件 12 端点 → 7 码，**全部新增**。
--
-- 【口径要点】见 `MODULES['partnerlevel']`：
--   · `POST /calculate`（按交易金额/频次/回款率算等级）是**纯计算**、方法体不落库 ⇒ `view`，
--     不是 create（同"价格引擎算价 ⇒ `pricing:engine:view`"的先例）；
--   · `POST /assign/{customerId}` 才是真写（把等级分配给客户）⇒ 复用库中既有动作词 `assign`；
--   · `PUT /{id}/enable|disable` 是改这个等级 ⇒ `update`（通用 PUT 兜底即 update，不额外建码）。
--
-- 【id 号段】权限码 **128000 起**（slot=28）、角色关联 **9780000 起** —— 执行前实测两段 count(*) = 0。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (128000, '往来单位客户等级分配', 'party:customer-level:assign', '/api/erp/customer/level/assign/{customerId}', 'POST', 1200),
 (128001, '往来单位客户等级新增', 'party:customer-level:create', '/api/erp/customer/level', 'POST', 1201),
 (128002, '往来单位客户等级删除', 'party:customer-level:delete', '/api/erp/customer/level/{id}', 'DELETE', 1202),
 (128003, '往来单位客户等级详情', 'party:customer-level:detail', '/api/erp/customer/level/{id}', 'GET', 1203),
 (128004, '往来单位客户等级查询', 'party:customer-level:list', '/api/erp/customer/level/list', 'GET', 1204),
 (128005, '往来单位客户等级编辑', 'party:customer-level:update', '/api/erp/customer/level/{id}', 'PUT', 1205),
 (128006, '往来单位客户等级查看', 'party:customer-level:view', '/api/erp/customer/level/calculate', 'POST', 1206)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9780000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 128000 AND 128007
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
