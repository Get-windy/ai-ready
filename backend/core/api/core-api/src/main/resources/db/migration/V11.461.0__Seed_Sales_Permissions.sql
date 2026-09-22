-- 销售域权限码**补齐**（E-01 sales 批次，2026-09-21）
--
-- 【与其它 E-04 种子的区别】sales **不是零码模块** —— 库里已有 18 个历史码
--   （`sale:order:*` 11 / `sale:promotion:*` 4 / `sale:discount:edit` / `sale:price:edit` /
--   `sale:settle:force` / `sale:manage`），所以本迁移**只补历史词表确实没有的动作，
--   不重建、不改名、不删除**任何既有码。本文件 97 行里只有 2 行命中已有（会被 NOT EXISTS 跳过）。
--
-- 【为什么必须补】E-01 要给销售域裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【覆盖的零码资源】出库单 outbound / 零售 retail / 零售班次 retail-shift /
--   销售退货 return / 退货单 return-doc / 换货 exchange / 预售 pre-order /
--   物流 logistics / 账户交割 account-delivery(财务域) / 商品组合 product:kit /
--   明细查询 detail-query / 单据查询 doc-query / 分析 analysis。
--
-- 【口径要点】写在 `tools/gen-module-permission-seed.py` 的 `MODULES['sales']`：
--   · `/api/sales/**`（注意是 sales）会被推导成 `sales:` 域，而本域历史前缀是 `sale:`
--     ⇒ 用 `base_overrides` 归一到 sale，避免一个模块分裂出两个域前缀；
--   · 拣货/打包/发货、结算/挂单/作废、激活/停用等**状态推进**类 POST 不能落到 `create`
--     兜底（"确认出库"变成"新建订单"），逐条指定为 `update`；
--   · 已带注解的端点在扫描阶段就被排除 —— 本批 224 个端点里 27 个已注解，
--     **只有真正会被插注解的 197 个才参与建码**，避免造出永不使用的僵尸码。
--
-- 【id 号段】权限码 114000 起（工具槽位 slot=14）、角色关联 9640000 起。
--   实测 114000~114999 与 9640000~9649999 均空闲。
--
-- 【遗留（未处理）】`sale:discount:edit` / `sale:settle:force` / `sale:manage` 三个历史码
--   在本批扫描里没有对应端点，疑似孤儿，建议随僵尸码批次处理，本批不动。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (114000, '销售account-delivery编辑', 'finance:account-delivery:update', '/api/erp/finance/account-delivery/deliver', 'POST', 1200),
 (114001, '销售account-delivery查看', 'finance:account-delivery:view', '/api/erp/finance/account-delivery/doc-page', 'GET', 1201),
 (114002, '销售kit新增', 'product:kit:create', '/api/erp/product-kit创建套装', 'POST', 1202),
 (114003, '销售kit删除', 'product:kit:delete', '/api/erp/product-kit/{id}', 'DELETE', 1203),
 (114004, '销售kit详情', 'product:kit:detail', '/api/erp/product-kit/{id}', 'GET', 1204),
 (114005, '销售kit查询', 'product:kit:list', '/api/erp/product-kit/page', 'GET', 1205),
 (114006, '销售kit编辑', 'product:kit:update', '/api/erp/product-kit/{id}', 'PUT', 1206),
 (114007, '销售kit查看', 'product:kit:view', '/api/erp/product-kit/active', 'GET', 1207),
 (114008, '销售analysis-promotion-funnel查询', 'sale:analysis-promotion-funnel:list', '/api/erp/sale/analysis/promotion-funnel/page', 'GET', 1208),
 (114009, '销售analysis查询', 'sale:analysis:list', '/api/erp/sale/analysis/customer-active/page', 'GET', 1209),
 (114010, '销售detail-query导出', 'sale:detail-query:export', '/api/sales/detail-query/export', 'GET', 1210),
 (114011, '销售detail-query查询', 'sale:detail-query:list', '/api/sales/detail-query/page', 'GET', 1211),
 (114012, '销售doc-query查询', 'sale:doc-query:list', '/api/sales/doc-query/page', 'GET', 1212),
 (114013, '销售doc-query编辑', 'sale:doc-query:update', '/api/sales/doc-query/{docType}/{id}/remark', 'PUT', 1213),
 (114014, '销售exchange审批', 'sale:exchange:approve', '/api/erp/sale/exchange/{id}/approve', 'POST', 1214),
 (114015, '销售exchange取消', 'sale:exchange:cancel', '/api/erp/sale/exchange/{id}/cancel', 'POST', 1215),
 (114016, '销售exchange完成', 'sale:exchange:complete', '/api/erp/sale/exchange/{id}/complete', 'POST', 1216),
 (114017, '销售exchange新增', 'sale:exchange:create', '/api/erp/sale/exchange创建换货单', 'POST', 1217),
 (114018, '销售exchange删除', 'sale:exchange:delete', '/api/erp/sale/exchange/{id}', 'DELETE', 1218),
 (114019, '销售exchange详情', 'sale:exchange:detail', '/api/erp/sale/exchange/{id:\\d+}', 'GET', 1219),
 (114020, '销售exchange导出', 'sale:exchange:export', '/api/erp/sale/exchange/export', 'GET', 1220),
 (114021, '销售exchange查询', 'sale:exchange:list', '/api/erp/sale/exchange/page', 'GET', 1221),
 (114022, '销售exchange打印', 'sale:exchange:print', '/api/erp/sale/exchange/{id}/print', 'POST', 1222),
 (114023, '销售exchange提交', 'sale:exchange:submit', '/api/erp/sale/exchange/{id}/submit', 'POST', 1223),
 (114024, '销售exchange编辑', 'sale:exchange:update', '/api/erp/sale/exchange/{id}', 'PUT', 1224),
 (114025, '销售exchange查看', 'sale:exchange:view', '/api/erp/sale/exchange/{id}/approval-records', 'GET', 1225),
 (114026, '销售logistics新增', 'sale:logistics:create', '/api/erp/sale/logistics/{orderId}/packages', 'POST', 1226),
 (114027, '销售logistics删除', 'sale:logistics:delete', '/api/erp/sale/logistics/{orderId}/packages/{packageId}', 'DELETE', 1227),
 (114028, '销售logistics查询', 'sale:logistics:list', '/api/erp/sale/logistics/shipment-notify/page', 'GET', 1228),
 (114029, '销售logistics编辑', 'sale:logistics:update', '/api/erp/sale/logistics/packages/{packageId}/acquire-waybill', 'POST', 1229),
 (114030, '销售logistics查看', 'sale:logistics:view', '/api/erp/sale/logistics/{orderId}/packages', 'GET', 1230),
 (114031, '销售order详情', 'sale:order:detail', '/api/erp/sale/order/{id:\\d+}', 'GET', 1231),
 (114032, '销售order查询', 'sale:order:list', '/api/erp/sale/order/page', 'GET', 1232),
 (114033, '销售order编辑', 'sale:order:update', '/api/erp/sale/order/{id}/pick-complete', 'POST', 1233),
 (114034, '销售order查看', 'sale:order:view', '/api/erp/sale/order/stats', 'GET', 1234),
 (114035, '销售out确认', 'sale:out:confirm', '/api/erp/sale-out/confirm', 'POST', 1235),
 (114036, '销售outbound审批', 'sale:outbound:approve', '/api/erp/sale/outbound/{id}/approve', 'POST', 1236),
 (114037, '销售outbound取消', 'sale:outbound:cancel', '/api/erp/sale/outbound/{id}/cancel', 'POST', 1237),
 (114038, '销售outbound完成', 'sale:outbound:complete', '/api/erp/sale/outbound/{id}/complete', 'POST', 1238),
 (114039, '销售outbound新增', 'sale:outbound:create', '/api/erp/sale/outbound创建出库单', 'POST', 1239),
 (114040, '销售outbound删除', 'sale:outbound:delete', '/api/erp/sale/outbound/{id}/items/{itemId}', 'DELETE', 1240),
 (114041, '销售outbound详情', 'sale:outbound:detail', '/api/erp/sale/outbound/{id}', 'GET', 1241),
 (114042, '销售outbound导出', 'sale:outbound:export', '/api/erp/sale/outbound/export', 'GET', 1242),
 (114043, '销售outbound导入', 'sale:outbound:import', '/api/erp/sale/outbound/import', 'POST', 1243),
 (114044, '销售outbound查询', 'sale:outbound:list', '/api/erp/sale/outbound/page', 'GET', 1244),
 (114045, '销售outbound打印', 'sale:outbound:print', '/api/erp/sale/outbound/batch-print', 'POST', 1245),
 (114046, '销售outbound提交', 'sale:outbound:submit', '/api/erp/sale/outbound/{id}/submit', 'POST', 1246),
 (114047, '销售outbound编辑', 'sale:outbound:update', '/api/erp/sale/outbound/{id}', 'PUT', 1247),
 (114048, '销售outbound查看', 'sale:outbound:view', '/api/erp/sale/outbound/page-detail', 'GET', 1248),
 (114049, '销售pre-order-analysis查询', 'sale:pre-order-analysis:list', '/api/erp/sale/pre-order/analysis/page', 'GET', 1249),
 (114050, '销售pre-order审批', 'sale:pre-order:approve', '/api/erp/sale/pre-order/{id}/approve', 'POST', 1250),
 (114051, '销售pre-order新增', 'sale:pre-order:create', '/api/erp/sale/pre-order创建预订货单', 'POST', 1251),
 (114052, '销售pre-order删除', 'sale:pre-order:delete', '/api/erp/sale/pre-order/{id}', 'DELETE', 1252),
 (114053, '销售pre-order详情', 'sale:pre-order:detail', '/api/erp/sale/pre-order/{id:\\d+}', 'GET', 1253),
 (114054, '销售pre-order导出', 'sale:pre-order:export', '/api/erp/sale/pre-order/export', 'GET', 1254),
 (114055, '销售pre-order查询', 'sale:pre-order:list', '/api/erp/sale/pre-order/page', 'GET', 1255),
 (114056, '销售pre-order打印', 'sale:pre-order:print', '/api/erp/sale/pre-order/{id}/print', 'POST', 1256),
 (114057, '销售pre-order提交', 'sale:pre-order:submit', '/api/erp/sale/pre-order/{id}/submit', 'POST', 1257),
 (114058, '销售pre-order编辑', 'sale:pre-order:update', '/api/erp/sale/pre-order/{id}', 'PUT', 1258),
 (114059, '销售pre-order查看', 'sale:pre-order:view', '/api/erp/sale/pre-order/page-detail', 'GET', 1259),
 (114060, '销售promotion详情', 'sale:promotion:detail', '/api/sale/promotion/{id}', 'GET', 1260),
 (114061, '销售promotion查询', 'sale:promotion:list', '/api/sale/promotion/page', 'GET', 1261),
 (114062, '销售promotion查看', 'sale:promotion:view', '/api/sale/promotion/active', 'GET', 1262),
 (114063, '销售retail-shift关闭', 'sale:retail-shift:close', '/api/sales/retail/shift/close', 'POST', 1263),
 (114064, '销售retail-shift新增', 'sale:retail-shift:create', '/api/sales/retail/shift/open', 'POST', 1264),
 (114065, '销售retail-shift详情', 'sale:retail-shift:detail', '/api/sales/retail/shift/{id}', 'GET', 1265),
 (114066, '销售retail-shift查询', 'sale:retail-shift:list', '/api/sales/retail/shift/page', 'GET', 1266),
 (114067, '销售retail-shift查看', 'sale:retail-shift:view', '/api/sales/retail/shift/current', 'GET', 1267),
 (114068, '销售retail新增', 'sale:retail:create', '/api/sales/retail创建零售单（含明细行）', 'POST', 1268),
 (114069, '销售retail删除', 'sale:retail:delete', '/api/sales/retail/{id}', 'DELETE', 1269),
 (114070, '销售retail详情', 'sale:retail:detail', '/api/sales/retail/{id}', 'GET', 1270),
 (114071, '销售retail查询', 'sale:retail:list', '/api/sales/retail/page/doc', 'GET', 1271),
 (114072, '销售retail打印', 'sale:retail:print', '/api/sales/retail/{id}/print', 'POST', 1272),
 (114073, '销售retail编辑', 'sale:retail:update', '/api/sales/retail/{id}', 'PUT', 1273),
 (114074, '销售retail查看', 'sale:retail:view', '/api/sales/retail/hold-list', 'GET', 1274),
 (114075, '销售return-doc审批', 'sale:return-doc:approve', '/api/erp/sale/return-doc/{id}/approve', 'POST', 1275),
 (114076, '销售return-doc取消', 'sale:return-doc:cancel', '/api/erp/sale/return-doc/{id}/cancel', 'POST', 1276),
 (114077, '销售return-doc完成', 'sale:return-doc:complete', '/api/erp/sale/return-doc/{id}/complete', 'POST', 1277),
 (114078, '销售return-doc新增', 'sale:return-doc:create', '/api/erp/sale/return-doc创建退货单', 'POST', 1278),
 (114079, '销售return-doc删除', 'sale:return-doc:delete', '/api/erp/sale/return-doc/{id}', 'DELETE', 1279),
 (114080, '销售return-doc详情', 'sale:return-doc:detail', '/api/erp/sale/return-doc/{id}', 'GET', 1280),
 (114081, '销售return-doc导出', 'sale:return-doc:export', '/api/erp/sale/return-doc/export', 'GET', 1281),
 (114082, '销售return-doc查询', 'sale:return-doc:list', '/api/erp/sale/return-doc/page', 'GET', 1282),
 (114083, '销售return-doc提交', 'sale:return-doc:submit', '/api/erp/sale/return-doc/{id}/submit', 'POST', 1283),
 (114084, '销售return-doc编辑', 'sale:return-doc:update', '/api/erp/sale/return-doc/{id}', 'PUT', 1284),
 (114085, '销售return-doc查看', 'sale:return-doc:view', '/api/erp/sale/return-doc/page-detail', 'GET', 1285),
 (114086, '销售return审批', 'sale:return:approve', '/api/erp/sale/return/{id}/approve', 'POST', 1286),
 (114087, '销售return取消', 'sale:return:cancel', '/api/erp/sale/return/{id}/cancel', 'POST', 1287),
 (114088, '销售return完成', 'sale:return:complete', '/api/erp/sale/return/{id}/complete', 'POST', 1288),
 (114089, '销售return新增', 'sale:return:create', '/api/erp/sale/return创建退货申请单', 'POST', 1289),
 (114090, '销售return删除', 'sale:return:delete', '/api/erp/sale/return/{id}', 'DELETE', 1290),
 (114091, '销售return详情', 'sale:return:detail', '/api/erp/sale/return/{id}', 'GET', 1291),
 (114092, '销售return导出', 'sale:return:export', '/api/erp/sale/return/export', 'GET', 1292),
 (114093, '销售return查询', 'sale:return:list', '/api/erp/sale/return/page', 'GET', 1293),
 (114094, '销售return提交', 'sale:return:submit', '/api/erp/sale/return/{id}/submit', 'POST', 1294),
 (114095, '销售return编辑', 'sale:return:update', '/api/erp/sale/return/{id}', 'PUT', 1295),
 (114096, '销售return查看', 'sale:return:view', '/api/erp/sale/return/page-detail', 'GET', 1296)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9640000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 114000 AND 114097
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
