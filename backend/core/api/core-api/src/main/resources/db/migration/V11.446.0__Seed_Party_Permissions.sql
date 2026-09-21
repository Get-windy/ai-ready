-- 往来单位域权限码种子（E-04，2026-09-21）
--
-- 【为什么】party 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器
--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**
--   （本仓历史事故，铁律：先补种子后补注解）。
--
-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController
--   16 端点 → 8 码）。本批：92 端点 → 59 码。
--
-- 【id 号段】权限码 104000 起（本模块独占槽位 slot=4）、角色关联 9540000 起。
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
 (104000, '往来单位customer新增', 'md:customer:create', '/api/erp/md/customer', 'POST', 1200),
 (104001, '往来单位customer删除', 'md:customer:delete', '/api/erp/md/customer/{id}', 'DELETE', 1201),
 (104002, '往来单位customer详情', 'md:customer:detail', '/api/erp/md/customer/{id}', 'GET', 1202),
 (104003, '往来单位customer导出', 'md:customer:export', '/api/erp/md/customer/export', 'GET', 1203),
 (104004, '往来单位customer导入', 'md:customer:import', '/api/erp/md/customer/import', 'POST', 1204),
 (104005, '往来单位customer查询', 'md:customer:list', '/api/erp/md/customer/page', 'GET', 1205),
 (104006, '往来单位customer编辑', 'md:customer:update', '/api/erp/md/customer/{id}', 'PUT', 1206),
 (104007, '往来单位customer查看', 'md:customer:view', '/api/erp/md/customer/search', 'GET', 1207),
 (104008, '往来单位linked-account新增', 'md:linked-account:create', '/api/erp/md/linked-account', 'POST', 1208),
 (104009, '往来单位linked-account删除', 'md:linked-account:delete', '/api/erp/md/linked-account/{id}', 'DELETE', 1209),
 (104010, '往来单位linked-account详情', 'md:linked-account:detail', '/api/erp/md/linked-account/{id}', 'GET', 1210),
 (104011, '往来单位linked-account导出', 'md:linked-account:export', '/api/erp/md/linked-account/export', 'GET', 1211),
 (104012, '往来单位linked-account查询', 'md:linked-account:list', '/api/erp/md/linked-account/page', 'GET', 1212),
 (104013, '往来单位linked-account编辑', 'md:linked-account:update', '/api/erp/md/linked-account/{id}', 'PUT', 1213),
 (104014, '往来单位linked-account查看', 'md:linked-account:view', '/api/erp/md/linked-account/dict', 'GET', 1214),
 (104015, '往来单位attachments新增', 'partner:attachments:create', '/api/erp/partner/attachments', 'POST', 1215),
 (104016, '往来单位attachments删除', 'partner:attachments:delete', '/api/erp/partner/attachments/{id}', 'DELETE', 1216),
 (104017, '往来单位attachments详情', 'partner:attachments:detail', '/api/erp/partner/attachments/by-partner/{partyId}', 'GET', 1217),
 (104018, '往来单位categories新增', 'partner:categories:create', '/api/erp/partner/categories', 'POST', 1218),
 (104019, '往来单位categories删除', 'partner:categories:delete', '/api/erp/partner/categories/{id}', 'DELETE', 1219),
 (104020, '往来单位categories查询', 'partner:categories:list', '/api/erp/partner/categories/tree', 'GET', 1220),
 (104021, '往来单位categories编辑', 'partner:categories:update', '/api/erp/partner/categories/{id}', 'PUT', 1221),
 (104022, '往来单位categories查看', 'partner:categories:view', '/api/erp/partner/categories', 'GET', 1222),
 (104023, '往来单位contacts新增', 'partner:contacts:create', '/api/erp/partner/contacts', 'POST', 1223),
 (104024, '往来单位contacts删除', 'partner:contacts:delete', '/api/erp/partner/contacts/{id}', 'DELETE', 1224),
 (104025, '往来单位contacts详情', 'partner:contacts:detail', '/api/erp/partner/contacts/by-party/{partyId}', 'GET', 1225),
 (104026, '往来单位contacts查询', 'partner:contacts:list', '/api/erp/partner/contacts/page', 'GET', 1226),
 (104027, '往来单位contacts编辑', 'partner:contacts:update', '/api/erp/partner/contacts/{id}', 'PUT', 1227),
 (104028, '往来单位grades新增', 'partner:grades:create', '/api/erp/partner/grades', 'POST', 1228),
 (104029, '往来单位grades删除', 'partner:grades:delete', '/api/erp/partner/grades/{id}', 'DELETE', 1229),
 (104030, '往来单位grades详情', 'partner:grades:detail', '/api/erp/partner/grades/{id}', 'GET', 1230),
 (104031, '往来单位grades编辑', 'partner:grades:update', '/api/erp/partner/grades/{id}', 'PUT', 1231),
 (104032, '往来单位grades查看', 'partner:grades:view', '/api/erp/partner/grades', 'GET', 1232),
 (104033, '往来单位roles新增', 'partner:roles:create', '/api/erp/partner/roles', 'POST', 1233),
 (104034, '往来单位roles删除', 'partner:roles:delete', '/api/erp/partner/roles/{id}', 'DELETE', 1234),
 (104035, '往来单位roles详情', 'partner:roles:detail', '/api/erp/partner/roles/{id}', 'GET', 1235),
 (104036, '往来单位roles查询', 'partner:roles:list', '/api/erp/partner/roles/page', 'GET', 1236),
 (104037, '往来单位roles编辑', 'partner:roles:update', '/api/erp/partner/roles/{id}', 'PUT', 1237),
 (104038, '往来单位contact新增', 'party:contact:create', '/api/erp/contact', 'POST', 1238),
 (104039, '往来单位contact删除', 'party:contact:delete', '/api/erp/contact/{id}', 'DELETE', 1239),
 (104040, '往来单位contact详情', 'party:contact:detail', '/api/erp/contact/{id}', 'GET', 1240),
 (104041, '往来单位contact查询', 'party:contact:list', '/api/erp/contact/page', 'GET', 1241),
 (104042, '往来单位contact编辑', 'party:contact:update', '/api/erp/contact/{id}', 'PUT', 1242),
 (104043, '往来单位contact查看', 'party:contact:view', '/api/erp/contact/{id}/parties', 'GET', 1243),
 (104044, '往来单位新增', 'party:create', '/api/erp/party', 'POST', 1244),
 (104045, '往来单位customer-region新增', 'party:customer-region:create', '/api/erp/customer/region', 'POST', 1245),
 (104046, '往来单位customer-region删除', 'party:customer-region:delete', '/api/erp/customer/region/{id}', 'DELETE', 1246),
 (104047, '往来单位customer-region详情', 'party:customer-region:detail', '/api/erp/customer/region/{id}', 'GET', 1247),
 (104048, '往来单位customer-region查询', 'party:customer-region:list', '/api/erp/customer/region/page', 'GET', 1248),
 (104049, '往来单位customer-region编辑', 'party:customer-region:update', '/api/erp/customer/region/{id}', 'PUT', 1249),
 (104050, '往来单位删除', 'party:delete', '/api/erp/party/{id}', 'DELETE', 1250),
 (104051, '往来单位详情', 'party:detail', '/api/erp/party/{id}', 'GET', 1251),
 (104052, '往来单位查询', 'party:list', '/api/erp/party/page', 'GET', 1252),
 (104053, '往来单位member-level新增', 'party:member-level:create', '/api/erp/member-level', 'POST', 1253),
 (104054, '往来单位member-level删除', 'party:member-level:delete', '/api/erp/member-level/{id}', 'DELETE', 1254),
 (104055, '往来单位member-level查询', 'party:member-level:list', '/api/erp/member-level/list', 'GET', 1255),
 (104056, '往来单位member-level编辑', 'party:member-level:update', '/api/erp/member-level/{id}', 'PUT', 1256),
 (104057, '往来单位编辑', 'party:update', '/api/erp/party/{id}', 'PUT', 1257),
 (104058, '往来单位查看', 'party:view', '/api/erp/party/search', 'GET', 1258)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9540000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 104000 AND 104059
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
