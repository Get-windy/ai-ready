-- 采购域权限码**补齐**（E-01 purchase 批次，2026-09-21）
--
-- 【与其它 E-04 种子的区别】purchase **不是零码模块** —— 库里已有 23 个历史码
--   （`purchase:order:*` 8 / `purchase:contract:*` 7 / `purchase:cost-sharing:*` 7 / `purchase:manage`），
--   所以本迁移**只补历史词表确实没有的动作，不重建、不改名、不删除**任何既有码：
--   既有码继续被它原来的端点使用，授权矩阵不受影响。
--   （本文件 VALUES 里 90 行，其中 21 行因 code 已存在会被 NOT EXISTS 跳过。）
--
-- 【为什么必须补】E-01 要给采购域裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--   缺的主要是四个**零码资源**：入库单 inbound / 退货单 return / 换货单 exchange /
--   询价单 inquiry / 报价 quote / 销售驱动 sales-driven / 价格跟踪 price-track / 采购分析 analytics。
--
-- 【映射口径】写在 `tools/gen-module-permission-seed.py` 的 `MODULES['purchase']`
--   （`resource_overrides` + `code_rules`），要点：
--   · 采购订单的两个子查询控制器（`/order/detail-query`、`/order/doc-query`）资源名归并到 `order`，
--     免得"同一张单三种资源名"；
--   · 生效/终止/归档、收货/质检/入库确认、报价评审/接受/撤回等**状态推进**类 POST
--     不能落到 `create` 兜底（那会把"确认收货"算成"新建入库单"），逐条指定；
--   · `batch-print` 此前不匹配 `.*/print$` 规则 ⇒ 也会落到 create，已修规则。
--
-- 【id 号段】权限码 115000 起（工具槽位 slot=15）、角色关联 9650000 起。
--   实测 115000~115999 与 9650000~9659999 均空闲。
--
-- 【遗留（未处理，供后续清理）】`purchase:price:edit`（"采购单价修改"）**无任何消费方、
--   api_path 为空**；价格跟踪控制器的码本批按 URL 建为 `purchase:price-track:*`，
--   故这条历史码是孤儿，建议随僵尸码批次处理，本批不动它。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (115000, '采购analytics查询', 'purchase:analytics:list', '/api/erp/purchase/analytics/page', 'GET', 1200),
 (115001, '采购合同审批', 'purchase:contract:approve', '/api/erp/purchase/contract/{id}/approve', 'POST', 1201),
 (115002, '采购合同新增', 'purchase:contract:create', '/api/erp/purchase/contract', 'POST', 1202),
 (115003, '采购合同删除', 'purchase:contract:delete', '/api/erp/purchase/contract/{id}', 'DELETE', 1203),
 (115004, '采购合同详情', 'purchase:contract:detail', '/api/erp/purchase/contract/{id}', 'GET', 1204),
 (115005, '采购合同导出', 'purchase:contract:export', '/api/erp/purchase/contract/export', 'GET', 1205),
 (115006, '采购合同查询', 'purchase:contract:list', '/api/erp/purchase/contract/page', 'GET', 1206),
 (115007, '采购合同提交', 'purchase:contract:submit', '/api/erp/purchase/contract/{id}/submit', 'POST', 1207),
 (115008, '采购合同编辑', 'purchase:contract:update', '/api/erp/purchase/contract/{id}', 'PUT', 1208),
 (115009, '采购合同查看', 'purchase:contract:view', '/api/erp/purchase/contract/statistics', 'GET', 1209),
 (115010, '采购cost-sharing取消', 'purchase:cost-sharing:cancel', '/api/erp/purchase/cost-sharing/{id}/cancel', 'POST', 1210),
 (115011, '采购cost-sharing完成', 'purchase:cost-sharing:complete', '/api/erp/purchase/cost-sharing/{id}/complete', 'POST', 1211),
 (115012, '采购cost-sharing新增', 'purchase:cost-sharing:create', '/api/erp/purchase/cost-sharing', 'POST', 1212),
 (115013, '采购cost-sharing详情', 'purchase:cost-sharing:detail', '/api/erp/purchase/cost-sharing/{id}', 'GET', 1213),
 (115014, '采购cost-sharing查询', 'purchase:cost-sharing:list', '/api/erp/purchase/cost-sharing/page', 'GET', 1214),
 (115015, '采购cost-sharing编辑', 'purchase:cost-sharing:update', '/api/erp/purchase/cost-sharing/{id}', 'PUT', 1215),
 (115016, '采购doc-query查询', 'purchase:doc-query:list', '/api/purchase/doc-query/page', 'GET', 1216),
 (115017, '采购doc-query编辑', 'purchase:doc-query:update', '/api/purchase/doc-query/{docType}/{id}/remark', 'PUT', 1217),
 (115018, '采购exchange审批', 'purchase:exchange:approve', '/api/erp/purchase/exchange/{id}/approve', 'POST', 1218),
 (115019, '采购exchange取消', 'purchase:exchange:cancel', '/api/erp/purchase/exchange/{id}/cancel', 'POST', 1219),
 (115020, '采购exchange完成', 'purchase:exchange:complete', '/api/erp/purchase/exchange/{id}/complete', 'POST', 1220),
 (115021, '采购exchange新增', 'purchase:exchange:create', '/api/erp/purchase/exchange', 'POST', 1221),
 (115022, '采购exchange删除', 'purchase:exchange:delete', '/api/erp/purchase/exchange/{id}', 'DELETE', 1222),
 (115023, '采购exchange详情', 'purchase:exchange:detail', '/api/erp/purchase/exchange/{id}', 'GET', 1223),
 (115024, '采购exchange导出', 'purchase:exchange:export', '/api/erp/purchase/exchange/export', 'GET', 1224),
 (115025, '采购exchange查询', 'purchase:exchange:list', '/api/erp/purchase/exchange/page', 'GET', 1225),
 (115026, '采购exchange打印', 'purchase:exchange:print', '/api/erp/purchase/exchange/{id}/print', 'POST', 1226),
 (115027, '采购exchange提交', 'purchase:exchange:submit', '/api/erp/purchase/exchange/{id}/submit', 'POST', 1227),
 (115028, '采购exchange编辑', 'purchase:exchange:update', '/api/erp/purchase/exchange/{id}', 'PUT', 1228),
 (115029, '采购exchange查看', 'purchase:exchange:view', '/api/erp/purchase/exchange/{id}/approval-records', 'GET', 1229),
 (115030, '采购inbound审批', 'purchase:inbound:approve', '/api/erp/purchase/inbound/{id}/approve', 'POST', 1230),
 (115031, '采购inbound取消', 'purchase:inbound:cancel', '/api/erp/purchase/inbound/{id}/cancel', 'POST', 1231),
 (115032, '采购inbound完成', 'purchase:inbound:complete', '/api/erp/purchase/inbound/{id}/complete', 'POST', 1232),
 (115033, '采购inbound确认', 'purchase:inbound:confirm', '/api/erp/purchase/inbound/{id}/warehouse-confirm', 'POST', 1233),
 (115034, '采购inbound新增', 'purchase:inbound:create', '/api/erp/purchase/inbound', 'POST', 1234),
 (115035, '采购inbound删除', 'purchase:inbound:delete', '/api/erp/purchase/inbound/batch', 'DELETE', 1235),
 (115036, '采购inbound详情', 'purchase:inbound:detail', '/api/erp/purchase/inbound/{id}', 'GET', 1236),
 (115037, '采购inbound导出', 'purchase:inbound:export', '/api/erp/purchase/inbound/export', 'GET', 1237),
 (115038, '采购inbound导入', 'purchase:inbound:import', '/api/erp/purchase/inbound/import', 'POST', 1238),
 (115039, '采购inbound查询', 'purchase:inbound:list', '/api/erp/purchase/inbound/page', 'GET', 1239),
 (115040, '采购inbound打印', 'purchase:inbound:print', '/api/erp/purchase/inbound/batch-print', 'POST', 1240),
 (115041, '采购inbound提交', 'purchase:inbound:submit', '/api/erp/purchase/inbound/{id}/submit', 'POST', 1241),
 (115042, '采购inbound编辑', 'purchase:inbound:update', '/api/erp/purchase/inbound/{id}', 'PUT', 1242),
 (115043, '采购inbound查看', 'purchase:inbound:view', '/api/erp/purchase/inbound/statistics', 'GET', 1243),
 (115044, '采购inquiry取消', 'purchase:inquiry:cancel', '/api/erp/purchase/inquiry/{id}/cancel', 'POST', 1244),
 (115045, '采购inquiry关闭', 'purchase:inquiry:close', '/api/erp/purchase/inquiry/{id}/close', 'POST', 1245),
 (115046, '采购inquiry新增', 'purchase:inquiry:create', '/api/erp/purchase/inquiry', 'POST', 1246),
 (115047, '采购inquiry删除', 'purchase:inquiry:delete', '/api/erp/purchase/inquiry/{id}', 'DELETE', 1247),
 (115048, '采购inquiry详情', 'purchase:inquiry:detail', '/api/erp/purchase/inquiry/status/{status}', 'GET', 1248),
 (115049, '采购inquiry查询', 'purchase:inquiry:list', '/api/erp/purchase/inquiry/page', 'GET', 1249),
 (115050, '采购inquiry发布', 'purchase:inquiry:publish', '/api/erp/purchase/inquiry/{id}/publish', 'POST', 1250),
 (115051, '采购inquiry发送', 'purchase:inquiry:send', '/api/erp/purchase/inquiry/{id}/send', 'POST', 1251),
 (115052, '采购inquiry编辑', 'purchase:inquiry:update', '/api/erp/purchase/inquiry/{id}', 'PUT', 1252),
 (115053, '采购inquiry查看', 'purchase:inquiry:view', '/api/erp/purchase/inquiry', 'GET', 1253),
 (115054, '采购order审批', 'purchase:order:approve', '/api/erp/purchase/order/{id}/approve', 'POST', 1254),
 (115055, '采购order取消', 'purchase:order:cancel', '/api/erp/purchase/order/{id}/cancel', 'POST', 1255),
 (115056, '采购order新增', 'purchase:order:create', '/api/erp/purchase/order', 'POST', 1256),
 (115057, '采购order删除', 'purchase:order:delete', '/api/erp/purchase/order/{id}', 'DELETE', 1257),
 (115058, '采购order详情', 'purchase:order:detail', '/api/erp/purchase/order/{id}', 'GET', 1258),
 (115059, '采购order导出', 'purchase:order:export', '/api/erp/purchase/order/export', 'GET', 1259),
 (115060, '采购order导入', 'purchase:order:import', '/api/erp/purchase/order/import', 'POST', 1260),
 (115061, '采购order查询', 'purchase:order:list', '/api/erp/purchase/order/detail-query/page', 'GET', 1261),
 (115062, '采购order打印', 'purchase:order:print', '/api/erp/purchase/order/batch-print', 'POST', 1262),
 (115063, '采购order提交', 'purchase:order:submit', '/api/erp/purchase/order/{id}/submit', 'POST', 1263),
 (115064, '采购order编辑', 'purchase:order:update', '/api/erp/purchase/order/{id}', 'PUT', 1264),
 (115065, '采购order查看', 'purchase:order:view', '/api/erp/purchase/order/statistics', 'GET', 1265),
 (115066, '采购price-track新增', 'purchase:price-track:create', '/api/purchase/price-track', 'POST', 1266),
 (115067, '采购price-track删除', 'purchase:price-track:delete', '/api/purchase/price-track/{id}', 'DELETE', 1267),
 (115068, '采购price-track查询', 'purchase:price-track:list', '/api/purchase/price-track/page', 'GET', 1268),
 (115069, '采购price-track编辑', 'purchase:price-track:update', '/api/purchase/price-track/{id}', 'PUT', 1269),
 (115070, '采购price-track查看', 'purchase:price-track:view', '/api/purchase/price-track/trend', 'GET', 1270),
 (115071, '采购quote审批', 'purchase:quote:approve', '/api/erp/purchase/quote/{id}/review', 'POST', 1271),
 (115072, '采购quote新增', 'purchase:quote:create', '/api/erp/purchase/quote', 'POST', 1272),
 (115073, '采购quote详情', 'purchase:quote:detail', '/api/erp/purchase/quote/{id}', 'GET', 1273),
 (115074, '采购quote编辑', 'purchase:quote:update', '/api/erp/purchase/quote/{id}', 'PUT', 1274),
 (115075, '采购quote查看', 'purchase:quote:view', '/api/erp/purchase/quote/inquiry/{inquiryId}/winner', 'GET', 1275),
 (115076, '采购return审批', 'purchase:return:approve', '/api/erp/purchase/return/{id}/approve', 'POST', 1276),
 (115077, '采购return取消', 'purchase:return:cancel', '/api/erp/purchase/return/{id}/cancel', 'POST', 1277),
 (115078, '采购return完成', 'purchase:return:complete', '/api/erp/purchase/return/{id}/complete', 'POST', 1278),
 (115079, '采购return新增', 'purchase:return:create', '/api/erp/purchase/return', 'POST', 1279),
 (115080, '采购return删除', 'purchase:return:delete', '/api/erp/purchase/return/batch', 'DELETE', 1280),
 (115081, '采购return详情', 'purchase:return:detail', '/api/erp/purchase/return/{id}', 'GET', 1281),
 (115082, '采购return导出', 'purchase:return:export', '/api/erp/purchase/return/export', 'GET', 1282),
 (115083, '采购return查询', 'purchase:return:list', '/api/erp/purchase/return/page', 'GET', 1283),
 (115084, '采购return打印', 'purchase:return:print', '/api/erp/purchase/return/batch-print', 'POST', 1284),
 (115085, '采购return提交', 'purchase:return:submit', '/api/erp/purchase/return/{id}/submit', 'POST', 1285),
 (115086, '采购return编辑', 'purchase:return:update', '/api/erp/purchase/return/{id}', 'PUT', 1286),
 (115087, '采购return查看', 'purchase:return:view', '/api/erp/purchase/return/statistics', 'GET', 1287),
 (115088, '采购sales-driven查询', 'purchase:sales-driven:list', '/api/erp/purchase/sales-driven/page', 'GET', 1288),
 (115089, '采购sales-driven编辑', 'purchase:sales-driven:update', '/api/erp/purchase/sales-driven/{id}/purchase-finished', 'POST', 1289)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9650000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 115000 AND 115090
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
