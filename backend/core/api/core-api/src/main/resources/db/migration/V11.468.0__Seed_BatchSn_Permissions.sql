-- 批次 / 序列号域权限码种子（E-01 erpbatchsn 批次，2026-09-21）
--
-- 【零码模块】库中**没有 `batch:` / `serial:` 码**。按 E-04 口径现场建码：
--   28 个待补端点 → 22 个码，**全部新增、无命中**，VALUES 占 119000~119021。
--
-- 【为什么必须补】E-01 要给 `cn.aiedge.erp.batchsn` 的裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【域口径 —— 为什么既不用 stock 也不用 batch-sn】
--   目录虽在 `erp-stock` 模块里，但业务是**批次号/序列号主数据**（表 batch_number /
--   serial_number / batch_rule / batch_flow_record / serial_flow_record…），是跨模块的
--   批号/序列号台账（库存、质检、售后、追溯都用），不是库存账本身：
--     · 不用 `stock:` —— 该前缀已被 erp-stock 的库存控制器占用（库中 105 条 `stock:*` 码的
--       api_path 全是 `/api/erp/stock/*` 与 `/api/erp/product*`），并进来会让"批次"这个对象
--       在权限矩阵里挂到「库存」模块下，语义错位；
--     · 不用新造一级域 `batch-sn:` —— 前端 pc-admin 的批次/序列号页**早已写着**
--       `erp:batch:*` / `erp:serial:*`（views/erp/batch/index.vue:160,177,394,402,410,418,426、
--       views/erp/serial/index.vue:51,287），而 `erp:` 域在库中已存在（`erp:expense:*` 四段式
--       历史码族）⇒ 取 **域 `erp` + 资源 `batch` / `serial`**，前端零改动即可对上。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['erpbatchsn']`：
--   · 入库/出库/质检是**状态推进**（RULES 的 POST 兜底会判成"新建批次"），且三者是三个
--     独立职责 ⇒ 取前端既有字符串的写法 `erp:batch:{inbound,outbound,inspect}`；
--   · 改状态复用库中既有动作词 `update-status`（system:menu:update-status 等 6 条）；
--     批次转移复用既有动作词 `transfer`；清缓存复用既有动作词 `clear`
--     （DELETE 兜底会把它判成"删除批次"）；校验批号/序列号归 `check`；
--   · 批次盘点（调整实物数量）与 `POST /search`（POST 传查询体）分别归 update / list。
--
-- 【id 号段】权限码 119000 起（工具槽位 slot=19）、角色关联 9690000 起。
--   实测 119000~119999 = 0 行、9690000~9699999 = 0 行，均空闲。
--
-- 【有意排除的端点】无 —— 两个控制器的 28 个端点全部补码。
--
-- 【遗留 / 拿不准 —— 前端有两个按钮后端没有端点】
--   · `views/erp/serial/index.vue:287` 的 `v-permission="'erp:serial:delete'"` 指向"删除序列号"，
--     但 `SerialNumberController` **没有 DELETE 端点**（只有 create/update/status/inbound/
--     outbound 与若干查询）⇒ 本批建不出 `erp:serial:delete`（生成器只给真实端点建码），
--     该按钮属"前端有、后端无"的悬空功能。只报告，不在本批修。
--   · `views/erp/batch/index.vue:426` 的 `v-permission="'erp:batch:delete'"` 同理 ——
--     批次控制器只有 `DELETE /clear-cache`（清缓存），**没有删除批次的端点**。
--     故本批把清缓存建成 `erp:batch:clear` 而**不是** `erp:batch:delete`，避免制造僵尸码；
--     那个删除按钮同样悬空。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (119000, 'ERP批次校验', 'erp:batch:check', '/api/erp/batch-sn/batches/validate-batch-no', 'GET', 1200),
 (119001, 'ERP批次清理', 'erp:batch:clear', '/api/erp/batch-sn/batches/clear-cache', 'DELETE', 1201),
 (119002, 'ERP批次新增', 'erp:batch:create', '/api/erp/batch-sn/batches', 'POST', 1202),
 (119003, 'ERP批次详情', 'erp:batch:detail', '/api/erp/batch-sn/batches/{id}', 'GET', 1203),
 (119004, 'ERP批次导出', 'erp:batch:export', '/api/erp/batch-sn/batches/export', 'GET', 1204),
 (119005, 'ERP批次入库', 'erp:batch:inbound', '/api/erp/batch-sn/batches/inbound', 'POST', 1205),
 (119006, 'ERP批次质检', 'erp:batch:inspect', '/api/erp/batch-sn/batches/{id}/quality-inspection', 'POST', 1206),
 (119007, 'ERP批次查询', 'erp:batch:list', '/api/erp/batch-sn/batches/page', 'GET', 1207),
 (119008, 'ERP批次出库', 'erp:batch:outbound', '/api/erp/batch-sn/batches/outbound', 'POST', 1208),
 (119009, 'ERP批次调拨', 'erp:batch:transfer', '/api/erp/batch-sn/batches/transfer', 'POST', 1209),
 (119010, 'ERP批次编辑', 'erp:batch:update', '/api/erp/batch-sn/batches/{id}', 'PUT', 1210),
 (119011, 'ERP批次更新状态', 'erp:batch:update-status', '/api/erp/batch-sn/batches/status', 'PATCH', 1211),
 (119012, 'ERP批次查看', 'erp:batch:view', '/api/erp/batch-sn/batches/expiring-warning', 'GET', 1212),
 (119013, 'ERP序列号校验', 'erp:serial:check', '/api/erp/batch-sn/serials/validate-serial-no', 'GET', 1213),
 (119014, 'ERP序列号新增', 'erp:serial:create', '/api/erp/batch-sn/serials', 'POST', 1214),
 (119015, 'ERP序列号详情', 'erp:serial:detail', '/api/erp/batch-sn/serials/{id}', 'GET', 1215),
 (119016, 'ERP序列号入库', 'erp:serial:inbound', '/api/erp/batch-sn/serials/inbound', 'POST', 1216),
 (119017, 'ERP序列号查询', 'erp:serial:list', '/api/erp/batch-sn/serials', 'GET', 1217),
 (119018, 'ERP序列号出库', 'erp:serial:outbound', '/api/erp/batch-sn/serials/outbound', 'POST', 1218),
 (119019, 'ERP序列号编辑', 'erp:serial:update', '/api/erp/batch-sn/serials/{id}', 'PUT', 1219),
 (119020, 'ERP序列号更新状态', 'erp:serial:update-status', '/api/erp/batch-sn/serials/{id}/status', 'PATCH', 1220),
 (119021, 'ERP序列号查看', 'erp:serial:view', '/api/erp/batch-sn/serials/warranty-warning', 'GET', 1221)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9690000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 119000 AND 119022
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
