-- 营销域权限码种子（E-04，2026-09-21）
--
-- 【为什么】marketing 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器
--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**
--   （本仓历史事故，铁律：先补种子后补注解）。
--
-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController
--   16 端点 → 8 码）。本批：142 端点 → 98 码。
--
-- 【id 号段】权限码 103000 起（本模块独占槽位 slot=3）、角色关联 9530000 起。
--   9xxxx 段已被 V11.42x 系列的短块占满，故另开 100000 段；
--   各模块用不同槽位避免多迁移撞主键（本仓 sys_permission.id 无序列默认值）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (103000, '营销addon-rule新增', 'marketing:addon-rule:create', '/api/erp/marketing/addon-rule', 'POST', 1200),
 (103001, '营销addon-rule删除', 'marketing:addon-rule:delete', '/api/erp/marketing/addon-rule/{id}', 'DELETE', 1201),
 (103002, '营销addon-rule详情', 'marketing:addon-rule:detail', '/api/erp/marketing/addon-rule/{id}', 'GET', 1202),
 (103003, '营销addon-rule查询', 'marketing:addon-rule:list', '/api/erp/marketing/addon-rule/page', 'GET', 1203),
 (103004, '营销addon-rule编辑', 'marketing:addon-rule:update', '/api/erp/marketing/addon-rule/{id}', 'PUT', 1204),
 (103005, '营销auto-campaign新增', 'marketing:auto-campaign:create', '/api/erp/marketing/auto-campaign', 'POST', 1205),
 (103006, '营销auto-campaign删除', 'marketing:auto-campaign:delete', '/api/erp/marketing/auto-campaign/{id}', 'DELETE', 1206),
 (103007, '营销auto-campaign详情', 'marketing:auto-campaign:detail', '/api/erp/marketing/auto-campaign/{id}', 'GET', 1207),
 (103008, '营销auto-campaign执行', 'marketing:auto-campaign:execute', '/api/erp/marketing/auto-campaign/{id}/run', 'POST', 1208),
 (103009, '营销auto-campaign查询', 'marketing:auto-campaign:list', '/api/erp/marketing/auto-campaign/page', 'GET', 1209),
 (103010, '营销auto-campaign编辑', 'marketing:auto-campaign:update', '/api/erp/marketing/auto-campaign/{id}', 'PUT', 1210),
 (103011, '营销auto-campaign查看', 'marketing:auto-campaign:view', '/api/erp/marketing/auto-campaign/{id}/candidates', 'GET', 1211),
 (103012, '营销card新增', 'marketing:card:create', '/api/erp/marketing/card', 'POST', 1212),
 (103013, '营销card删除', 'marketing:card:delete', '/api/erp/marketing/card/{id}', 'DELETE', 1213),
 (103014, '营销card详情', 'marketing:card:detail', '/api/erp/marketing/card/member/{memberId}', 'GET', 1214),
 (103015, '营销card查询', 'marketing:card:list', '/api/erp/marketing/card/page', 'GET', 1215),
 (103016, '营销card编辑', 'marketing:card:update', '/api/erp/marketing/card/{id}', 'PUT', 1216),
 (103017, '营销commission-record删除', 'marketing:commission-record:delete', '/api/erp/marketing/commission/record/{id}', 'DELETE', 1217),
 (103018, '营销commission-record详情', 'marketing:commission-record:detail', '/api/erp/marketing/commission/record/{id}', 'GET', 1218),
 (103019, '营销commission-record查询', 'marketing:commission-record:list', '/api/erp/marketing/commission/record/page', 'GET', 1219),
 (103020, '营销commission-rule新增', 'marketing:commission-rule:create', '/api/erp/marketing/commission/rule', 'POST', 1220),
 (103021, '营销commission-rule删除', 'marketing:commission-rule:delete', '/api/erp/marketing/commission/rule/{id}', 'DELETE', 1221),
 (103022, '营销commission-rule详情', 'marketing:commission-rule:detail', '/api/erp/marketing/commission/rule/{id}', 'GET', 1222),
 (103023, '营销commission-rule查询', 'marketing:commission-rule:list', '/api/erp/marketing/commission/rule/page', 'GET', 1223),
 (103024, '营销commission-rule编辑', 'marketing:commission-rule:update', '/api/erp/marketing/commission/rule/{id}', 'PUT', 1224),
 (103025, '营销commission查询', 'marketing:commission:list', '/api/erp/marketing/commission/staff-summary/page', 'GET', 1225),
 (103026, '营销coupon-template新增', 'marketing:coupon-template:create', '/api/erp/marketing/coupon-template', 'POST', 1226),
 (103027, '营销coupon-template删除', 'marketing:coupon-template:delete', '/api/erp/marketing/coupon-template/{id}', 'DELETE', 1227),
 (103028, '营销coupon-template详情', 'marketing:coupon-template:detail', '/api/erp/marketing/coupon-template/{id}', 'GET', 1228),
 (103029, '营销coupon-template查询', 'marketing:coupon-template:list', '/api/erp/marketing/coupon-template/page', 'GET', 1229),
 (103030, '营销coupon-template编辑', 'marketing:coupon-template:update', '/api/erp/marketing/coupon-template/{id}', 'PUT', 1230),
 (103031, '营销coupon-template查看', 'marketing:coupon-template:view', '/api/erp/marketing/coupon-template/{id}/stat', 'GET', 1231),
 (103032, '营销coupon新增', 'marketing:coupon:create', '/api/erp/marketing/coupon/{id}/use', 'POST', 1232),
 (103033, '营销coupon删除', 'marketing:coupon:delete', '/api/erp/marketing/coupon/{id}', 'DELETE', 1233),
 (103034, '营销coupon详情', 'marketing:coupon:detail', '/api/erp/marketing/coupon/available/{partnerId}', 'GET', 1234),
 (103035, '营销coupon查询', 'marketing:coupon:list', '/api/erp/marketing/coupon/page', 'GET', 1235),
 (103036, '营销flash-sale新增', 'marketing:flash-sale:create', '/api/erp/marketing/flash-sale', 'POST', 1236),
 (103037, '营销flash-sale删除', 'marketing:flash-sale:delete', '/api/erp/marketing/flash-sale/{id}', 'DELETE', 1237),
 (103038, '营销flash-sale详情', 'marketing:flash-sale:detail', '/api/erp/marketing/flash-sale/{id}', 'GET', 1238),
 (103039, '营销flash-sale查询', 'marketing:flash-sale:list', '/api/erp/marketing/flash-sale/page', 'GET', 1239),
 (103040, '营销flash-sale发布', 'marketing:flash-sale:publish', '/api/erp/marketing/flash-sale/{id}/publish', 'POST', 1240),
 (103041, '营销flash-sale编辑', 'marketing:flash-sale:update', '/api/erp/marketing/flash-sale/{id}', 'PUT', 1241),
 (103042, '营销flash-sale查看', 'marketing:flash-sale:view', '/api/erp/marketing/flash-sale/{id}/participants', 'GET', 1242),
 (103043, '营销member-config编辑', 'marketing:member-config:update', '/api/erp/marketing/member-config', 'PUT', 1243),
 (103044, '营销member-config查看', 'marketing:member-config:view', '/api/erp/marketing/member-config', 'GET', 1244),
 (103045, '营销member-level新增', 'marketing:member-level:create', '/api/erp/marketing/member-level/evaluate', 'POST', 1245),
 (103046, '营销member-level查看', 'marketing:member-level:view', '/api/erp/marketing/member-level/rules', 'GET', 1246),
 (103047, '营销points-exchange新增', 'marketing:points-exchange:create', '/api/erp/marketing/points-exchange', 'POST', 1247),
 (103048, '营销points-exchange删除', 'marketing:points-exchange:delete', '/api/erp/marketing/points-exchange/{id}', 'DELETE', 1248),
 (103049, '营销points-exchange详情', 'marketing:points-exchange:detail', '/api/erp/marketing/points-exchange/product/{productId}', 'GET', 1249),
 (103050, '营销points-exchange查询', 'marketing:points-exchange:list', '/api/erp/marketing/points-exchange/page', 'GET', 1250),
 (103051, '营销points-exchange编辑', 'marketing:points-exchange:update', '/api/erp/marketing/points-exchange/{id}', 'PUT', 1251),
 (103052, '营销points-ledger新增', 'marketing:points-ledger:create', '/api/erp/marketing/points-ledger/earn', 'POST', 1252),
 (103053, '营销points-ledger查询', 'marketing:points-ledger:list', '/api/erp/marketing/points-ledger/batch/list', 'GET', 1253),
 (103054, '营销points-ledger查看', 'marketing:points-ledger:view', '/api/erp/marketing/points-ledger/available', 'GET', 1254),
 (103055, '营销points查询', 'marketing:points:list', '/api/erp/marketing/points/page', 'GET', 1255),
 (103056, '营销presale新增', 'marketing:presale:create', '/api/erp/marketing/presale', 'POST', 1256),
 (103057, '营销presale删除', 'marketing:presale:delete', '/api/erp/marketing/presale/{id}', 'DELETE', 1257),
 (103058, '营销presale详情', 'marketing:presale:detail', '/api/erp/marketing/presale/{id}', 'GET', 1258),
 (103059, '营销presale查询', 'marketing:presale:list', '/api/erp/marketing/presale/page', 'GET', 1259),
 (103060, '营销presale发布', 'marketing:presale:publish', '/api/erp/marketing/presale/{id}/publish', 'POST', 1260),
 (103061, '营销presale编辑', 'marketing:presale:update', '/api/erp/marketing/presale/{id}', 'PUT', 1261),
 (103062, '营销product-points-rule新增', 'marketing:product-points-rule:create', '/api/erp/marketing/product-points-rule', 'POST', 1262),
 (103063, '营销product-points-rule删除', 'marketing:product-points-rule:delete', '/api/erp/marketing/product-points-rule/{id}', 'DELETE', 1263),
 (103064, '营销product-points-rule查询', 'marketing:product-points-rule:list', '/api/erp/marketing/product-points-rule/list', 'GET', 1264),
 (103065, '营销product-points-rule编辑', 'marketing:product-points-rule:update', '/api/erp/marketing/product-points-rule/{id}', 'PUT', 1265),
 (103066, '营销program新增', 'marketing:program:create', '/api/erp/marketing/program', 'POST', 1266),
 (103067, '营销program删除', 'marketing:program:delete', '/api/erp/marketing/program/{id}', 'DELETE', 1267),
 (103068, '营销program详情', 'marketing:program:detail', '/api/erp/marketing/program/{id}', 'GET', 1268),
 (103069, '营销program查询', 'marketing:program:list', '/api/erp/marketing/program/page', 'GET', 1269),
 (103070, '营销program编辑', 'marketing:program:update', '/api/erp/marketing/program/{id}', 'PUT', 1270),
 (103071, '营销program查看', 'marketing:program:view', '/api/erp/marketing/program/active', 'GET', 1271),
 (103072, '营销promote查询', 'marketing:promote:list', '/api/erp/marketing/promote/product/page', 'GET', 1272),
 (103073, '营销promotion-activity新增', 'marketing:promotion-activity:create', '/api/erp/marketing/promotion-activity', 'POST', 1273),
 (103074, '营销promotion-activity删除', 'marketing:promotion-activity:delete', '/api/erp/marketing/promotion-activity/{id}', 'DELETE', 1274),
 (103075, '营销promotion-activity详情', 'marketing:promotion-activity:detail', '/api/erp/marketing/promotion-activity/{id}', 'GET', 1275),
 (103076, '营销promotion-activity查询', 'marketing:promotion-activity:list', '/api/erp/marketing/promotion-activity/page', 'GET', 1276),
 (103077, '营销promotion-activity编辑', 'marketing:promotion-activity:update', '/api/erp/marketing/promotion-activity/{id}', 'PUT', 1277),
 (103078, '营销promotion-activity查看', 'marketing:promotion-activity:view', '/api/erp/marketing/promotion-activity/{id}/products', 'GET', 1278),
 (103079, '营销promotion新增', 'marketing:promotion:create', '/api/erp/marketing/promotion/calc', 'POST', 1279),
 (103080, '营销rules新增', 'marketing:rules:create', '/api/erp/marketing/rules', 'POST', 1280),
 (103081, '营销rules删除', 'marketing:rules:delete', '/api/erp/marketing/rules/{id}', 'DELETE', 1281),
 (103082, '营销rules详情', 'marketing:rules:detail', '/api/erp/marketing/rules/{id}', 'GET', 1282),
 (103083, '营销rules查询', 'marketing:rules:list', '/api/erp/marketing/rules/page', 'GET', 1283),
 (103084, '营销rules编辑', 'marketing:rules:update', '/api/erp/marketing/rules/{id}', 'PUT', 1284),
 (103085, '营销share新增', 'marketing:share:create', '/api/erp/marketing/share', 'POST', 1285),
 (103086, '营销share查询', 'marketing:share:list', '/api/erp/marketing/share/page', 'GET', 1286),
 (103087, '营销share查看', 'marketing:share:view', '/api/erp/marketing/share/summary', 'GET', 1287),
 (103088, '营销sms新增', 'marketing:sms:create', '/api/erp/marketing/sms/template', 'POST', 1288),
 (103089, '营销sms删除', 'marketing:sms:delete', '/api/erp/marketing/sms/template/{id}', 'DELETE', 1289),
 (103090, '营销sms查询', 'marketing:sms:list', '/api/erp/marketing/sms/template/page', 'GET', 1290),
 (103091, '营销sms编辑', 'marketing:sms:update', '/api/erp/marketing/sms/setting', 'PUT', 1291),
 (103092, '营销sms查看', 'marketing:sms:view', '/api/erp/marketing/sms/setting', 'GET', 1292),
 (103093, '营销stored-card占用', 'marketing:stored-card:consume', '/api/erp/marketing/stored-card/{id}/consume', 'POST', 1293),
 (103094, '营销stored-card新增', 'marketing:stored-card:create', '/api/erp/marketing/stored-card', 'POST', 1294),
 (103095, '营销stored-card详情', 'marketing:stored-card:detail', '/api/erp/marketing/stored-card/{id}', 'GET', 1295),
 (103096, '营销stored-card查询', 'marketing:stored-card:list', '/api/erp/marketing/stored-card/page', 'GET', 1296),
 (103097, '营销stored-card查看', 'marketing:stored-card:view', '/api/erp/marketing/stored-card/by-no', 'GET', 1297)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9530000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 103000 AND 103098
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
