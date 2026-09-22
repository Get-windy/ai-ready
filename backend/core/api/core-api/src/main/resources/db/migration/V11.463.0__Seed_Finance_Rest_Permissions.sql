-- 财务域其余（费用 / 账户 / 银行账户 / 资金划转 / 往来调整 / 分析 / 主数据）权限码补齐
-- （E-01 erpfinance 批次，2026-09-21）
--
-- 【与其它 E-04 种子的区别】本域**不是零码模块**：库里已有 `erp:expense:*` 26 条与
--   若干 `finance:*` / `md:*` 码，所以本迁移**只补确实没有的动作**，
--   不重建、不改名、不删除任何既有码。
--
-- 【最关键的一处：费用子域必须映射到历史码】
--   `erp:expense:*` 是**四段式**（`erp:expense:<资源>:<动作>`），而按类级路径推导
--   只会得到 `expense:application:create` 这种**三段式** —— 两者对不上。
--   若放任推导，26 条历史码会永远是没有消费方的僵尸码，同时多出一整套同义新码。
--   故在 `MODULES['erpfinance']` 里把域固定成 `erp`、资源前缀固定成 `expense:`，
--   并沿用历史动作词（`edit`/`query` 而非 `update`/`detail`）。
--   实测：本批 85 个码里 **13 个命中历史码**（费用申请/报销/统计/审批处理），
--   其余 72 个为新增。
--
-- 【为什么必须补】E-01 要给本域裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【覆盖】账户 account / 银行账户 bank-account / 资金划转 cash-transfer /
--   往来调整 ar-ap-adjust / 账龄分析 analytics-* / 集成 integration /
--   费用申请与统计 expense / 费用审批 expense-approval / 费用单据 expense-doc /
--   费用类型 expense-type / 主数据（费用类型、支付渠道、支付方式、其他收入）md:*
--
-- 【id 号段】权限码 112000 起（工具槽位 slot=12）、角色关联 9620000 起。
--   实测 112000~112999 与 9620000~9629999 均空闲。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (112000, '财务域其余expense-application新增', 'erp:expense:application:create', '/api/erp/expense/application', 'POST', 1200),
 (112001, '财务域其余expense-application删除', 'erp:expense:application:delete', '/api/erp/expense/application/{id}', 'DELETE', 1201),
 (112002, '财务域其余expense-application编辑', 'erp:expense:application:edit', '/api/erp/expense/application/{id}', 'PUT', 1202),
 (112003, '财务域其余expense-application查询', 'erp:expense:application:list', '/api/erp/expense/list', 'GET', 1203),
 (112004, '财务域其余expense-applicationquery', 'erp:expense:application:query', '/api/erp/expense/{id}', 'GET', 1204),
 (112005, '财务域其余expense-application提交', 'erp:expense:application:submit', '/api/erp/expense/application/{id}/submit', 'POST', 1205),
 (112006, '财务域其余expense-approvalprocess', 'erp:expense:approval:process', '/api/erp/expense/{id}/approve', 'POST', 1206),
 (112007, '财务域其余expense-reimbursement新增', 'erp:expense:reimbursement:create', '/api/erp/expense/reimbursement', 'POST', 1207),
 (112008, '财务域其余expense-reimbursement删除', 'erp:expense:reimbursement:delete', '/api/erp/expense/reimbursement/{id}', 'DELETE', 1208),
 (112009, '财务域其余expense-reimbursement编辑', 'erp:expense:reimbursement:edit', '/api/erp/expense/reimbursement/{id}', 'PUT', 1209),
 (112010, '财务域其余expense-reimbursement查询', 'erp:expense:reimbursement:list', '/api/erp/expense/reimbursement/page', 'GET', 1210),
 (112011, '财务域其余expense-reimbursement提交', 'erp:expense:reimbursement:submit', '/api/erp/expense/reimbursement/{id}/submit', 'POST', 1211),
 (112012, '财务域其余expense-statistics查询', 'erp:expense:statistics:list', '/api/erp/expense/statistics/summary', 'GET', 1212),
 (112013, '财务域其余expense-type查询', 'erp:expense:type:list', '/api/erp/expense/type/list', 'GET', 1213),
 (112014, '财务域其余expense-typequery', 'erp:expense:type:query', '/api/erp/expense/type/{code}', 'GET', 1214),
 (112015, '财务域其余expense-type查看', 'erp:expense:type:view', '/api/erp/expense/type/descriptions', 'GET', 1215),
 (112016, '财务域其余account新增', 'finance:account:create', '/api/erp/finance/account', 'POST', 1216),
 (112017, '财务域其余account删除', 'finance:account:delete', '/api/erp/finance/account/{id}', 'DELETE', 1217),
 (112018, '财务域其余account详情', 'finance:account:detail', '/api/erp/finance/account/{id}', 'GET', 1218),
 (112019, '财务域其余account导出', 'finance:account:export', '/api/erp/finance/account/export', 'GET', 1219),
 (112020, '财务域其余account查询', 'finance:account:list', '/api/erp/finance/account/page', 'GET', 1220),
 (112021, '财务域其余account编辑', 'finance:account:update', '/api/erp/finance/account/{id}', 'PUT', 1221),
 (112022, '财务域其余account查看', 'finance:account:view', '/api/erp/finance/account/next-code', 'GET', 1222),
 (112023, '财务域其余analytics-collection-stats查询', 'finance:analytics-collection-stats:list', '/api/erp/finance/analytics/collection-stats/page', 'GET', 1223),
 (112024, '财务域其余analytics-collection-stats查看', 'finance:analytics-collection-stats:view', '/api/erp/finance/analytics/collection-stats/detail', 'GET', 1224),
 (112025, '财务域其余analytics-invoice-stats查询', 'finance:analytics-invoice-stats:list', '/api/erp/finance/analytics/invoice-stats/page', 'GET', 1225),
 (112026, '财务域其余analytics-partner-balance对账', 'finance:analytics-partner-balance:reconcile', '/api/erp/finance/analytics/partner-balance/reconcile', 'POST', 1226),
 (112027, '财务域其余ar-ap-adjust取消', 'finance:ar-ap-adjust:cancel', '/api/erp/finance/ar-ap-adjust/cancel', 'POST', 1227),
 (112028, '财务域其余ar-ap-adjust确认', 'finance:ar-ap-adjust:confirm', '/api/erp/finance/ar-ap-adjust/confirm', 'POST', 1228),
 (112029, '财务域其余ar-ap-adjust新增', 'finance:ar-ap-adjust:create', '/api/erp/finance/ar-ap-adjust/create', 'POST', 1229),
 (112030, '财务域其余ar-ap-adjust删除', 'finance:ar-ap-adjust:delete', '/api/erp/finance/ar-ap-adjust/{id}', 'DELETE', 1230),
 (112031, '财务域其余ar-ap-adjust详情', 'finance:ar-ap-adjust:detail', '/api/erp/finance/ar-ap-adjust/{id}', 'GET', 1231),
 (112032, '财务域其余ar-ap-adjust查询', 'finance:ar-ap-adjust:list', '/api/erp/finance/ar-ap-adjust/page', 'GET', 1232),
 (112033, '财务域其余bank-account新增', 'finance:bank-account:create', '/api/erp/finance/bank-account', 'POST', 1233),
 (112034, '财务域其余bank-account删除', 'finance:bank-account:delete', '/api/erp/finance/bank-account/{id}', 'DELETE', 1234),
 (112035, '财务域其余bank-account详情', 'finance:bank-account:detail', '/api/erp/finance/bank-account/{id}', 'GET', 1235),
 (112036, '财务域其余bank-account导出', 'finance:bank-account:export', '/api/erp/finance/bank-account/export', 'GET', 1236),
 (112037, '财务域其余bank-account查询', 'finance:bank-account:list', '/api/erp/finance/bank-account/page', 'GET', 1237),
 (112038, '财务域其余bank-account状态', 'finance:bank-account:status', '/api/erp/finance/bank-account/{id}/status', 'PUT', 1238),
 (112039, '财务域其余bank-account编辑', 'finance:bank-account:update', '/api/erp/finance/bank-account/{id}', 'PUT', 1239),
 (112040, '财务域其余bank-account查看', 'finance:bank-account:view', '/api/erp/finance/bank-account/next-code', 'GET', 1240),
 (112041, '财务域其余cash-transfer取消', 'finance:cash-transfer:cancel', '/api/erp/finance/cash-transfer/cancel', 'POST', 1241),
 (112042, '财务域其余cash-transfer确认', 'finance:cash-transfer:confirm', '/api/erp/finance/cash-transfer/confirm', 'POST', 1242),
 (112043, '财务域其余cash-transfer新增', 'finance:cash-transfer:create', '/api/erp/finance/cash-transfer/create', 'POST', 1243),
 (112044, '财务域其余cash-transfer删除', 'finance:cash-transfer:delete', '/api/erp/finance/cash-transfer/{id}', 'DELETE', 1244),
 (112045, '财务域其余cash-transfer详情', 'finance:cash-transfer:detail', '/api/erp/finance/cash-transfer/{id}', 'GET', 1245),
 (112046, '财务域其余cash-transfer查询', 'finance:cash-transfer:list', '/api/erp/finance/cash-transfer/page', 'GET', 1246),
 (112047, '财务域其余cash-transfer查看', 'finance:cash-transfer:view', '/api/erp/finance/cash-transfer/page-detail', 'GET', 1247),
 (112048, '财务域其余expense-approval新增', 'finance:expense-approval:create', '/api/erp/finance/expense-approval/process', 'POST', 1248),
 (112049, '财务域其余expense-approval详情', 'finance:expense-approval:detail', '/api/erp/finance/expense-approval/detail/{docId}', 'GET', 1249),
 (112050, '财务域其余expense-approval提交', 'finance:expense-approval:submit', '/api/erp/finance/expense-approval/submit', 'POST', 1250),
 (112051, '财务域其余expense-approval查看', 'finance:expense-approval:view', '/api/erp/finance/expense-approval/pending', 'GET', 1251),
 (112052, '财务域其余expense-doc取消', 'finance:expense-doc:cancel', '/api/erp/finance/expense-doc/cancel', 'POST', 1252),
 (112053, '财务域其余expense-doc确认', 'finance:expense-doc:confirm', '/api/erp/finance/expense-doc/confirm', 'POST', 1253),
 (112054, '财务域其余expense-doc新增', 'finance:expense-doc:create', '/api/erp/finance/expense-doc/create', 'POST', 1254),
 (112055, '财务域其余expense-doc删除', 'finance:expense-doc:delete', '/api/erp/finance/expense-doc/{id}', 'DELETE', 1255),
 (112056, '财务域其余expense-doc详情', 'finance:expense-doc:detail', '/api/erp/finance/expense-doc/{id}', 'GET', 1256),
 (112057, '财务域其余expense-doc查询', 'finance:expense-doc:list', '/api/erp/finance/expense-doc/page', 'GET', 1257),
 (112058, '财务域其余expense-doc查看', 'finance:expense-doc:view', '/api/erp/finance/expense-doc/page-detail', 'GET', 1258),
 (112059, '财务域其余expense-stats查看', 'finance:expense-stats:view', '/api/erp/finance/expense-stats/summary', 'GET', 1259),
 (112060, '财务域其余integration新增', 'finance:integration:create', '/api/erp/finance/integration/voucher', 'POST', 1260),
 (112061, '财务域其余expense-type新增', 'md:expense-type:create', '/api/erp/md/expense-type', 'POST', 1261),
 (112062, '财务域其余expense-type删除', 'md:expense-type:delete', '/api/erp/md/expense-type/{id}', 'DELETE', 1262),
 (112063, '财务域其余expense-type详情', 'md:expense-type:detail', '/api/erp/md/expense-type/{id}', 'GET', 1263),
 (112064, '财务域其余expense-type导出', 'md:expense-type:export', '/api/erp/md/expense-type/export', 'GET', 1264),
 (112065, '财务域其余expense-type查询', 'md:expense-type:list', '/api/erp/md/expense-type/tree', 'GET', 1265),
 (112066, '财务域其余expense-type编辑', 'md:expense-type:update', '/api/erp/md/expense-type/{id}', 'PUT', 1266),
 (112067, '财务域其余expense-type查看', 'md:expense-type:view', '/api/erp/md/expense-type/aux-types', 'GET', 1267),
 (112068, '财务域其余other-income新增', 'md:other-income:create', '/api/erp/md/other-income', 'POST', 1268),
 (112069, '财务域其余other-income删除', 'md:other-income:delete', '/api/erp/md/other-income/{id}', 'DELETE', 1269),
 (112070, '财务域其余other-income详情', 'md:other-income:detail', '/api/erp/md/other-income/{id}', 'GET', 1270),
 (112071, '财务域其余other-income导出', 'md:other-income:export', '/api/erp/md/other-income/export', 'GET', 1271),
 (112072, '财务域其余other-income查询', 'md:other-income:list', '/api/erp/md/other-income/page', 'GET', 1272),
 (112073, '财务域其余other-income编辑', 'md:other-income:update', '/api/erp/md/other-income/{id}', 'PUT', 1273),
 (112074, '财务域其余other-income查看', 'md:other-income:view', '/api/erp/md/other-income/aux-types', 'GET', 1274),
 (112075, '财务域其余payment-channel导出', 'md:payment-channel:export', '/api/erp/md/payment-channel/export', 'GET', 1275),
 (112076, '财务域其余payment-channel查询', 'md:payment-channel:list', '/api/erp/md/payment-channel/list', 'GET', 1276),
 (112077, '财务域其余payment-channel查看', 'md:payment-channel:view', '/api/erp/md/payment-channel/import-template', 'GET', 1277),
 (112078, '财务域其余payment-method新增', 'md:payment-method:create', '/api/erp/md/payment-method', 'POST', 1278),
 (112079, '财务域其余payment-method删除', 'md:payment-method:delete', '/api/erp/md/payment-method/{id}', 'DELETE', 1279),
 (112080, '财务域其余payment-method详情', 'md:payment-method:detail', '/api/erp/md/payment-method/{id}', 'GET', 1280),
 (112081, '财务域其余payment-method导出', 'md:payment-method:export', '/api/erp/md/payment-method/export', 'GET', 1281),
 (112082, '财务域其余payment-method查询', 'md:payment-method:list', '/api/erp/md/payment-method/page', 'GET', 1282),
 (112083, '财务域其余payment-method状态', 'md:payment-method:status', '/api/erp/md/payment-method/{id}/status', 'PUT', 1283),
 (112084, '财务域其余payment-method编辑', 'md:payment-method:update', '/api/erp/md/payment-method/{id}', 'PUT', 1284)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9620000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 112000 AND 112085
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
