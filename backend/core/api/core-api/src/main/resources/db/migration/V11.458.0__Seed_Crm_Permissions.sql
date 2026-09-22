-- crm 域权限码**补齐**（E-01 crm 批次，2026-09-21）
--
-- 【与其它 E-04 种子的关键区别】crm **不是零码模块** —— 库里已有 57 个历史码。
--   所以本迁移只补「历史词表里确实没有」的动作，**不重建、不改名、不删除**任何既有码：
--   既有码继续被它原来的端点使用，授权矩阵不受影响。
--
-- 【为什么必须补】E-01 要给 crm 的 144 个裸端点补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--   逐条比对后发现历史码缺三类动作：
--     ① 5 个资源在库里**一个码都没有**：跟进 / 公海池 / 营销活动 / 报价模板 / 拜访；
--     ② 线索、商机、报价单**没有 delete/export**（报价单还缺 submit/approve）；
--     ③ 合同没有 delete。
--   合计 36 个码（本文件 VALUES 里 65 行的其余 29 行因已存在会被 NOT EXISTS 跳过）。
--
-- 【粒度对照表】185 个端点 → 65 个码，映射规则写在
--   `tools/gen-module-permission-seed.py` 的 `MODULES['crm']`（resource_overrides + code_rules）：
--   有历史码的资源逐条对齐（list/detail→view、update→edit、export→download/downloadpdf…），
--   零码资源现场按 `<域>:<资源>:<动作>` 建码。
--
-- 【id 号段】权限码 110000 起（工具槽位 slot=10）、角色关联 9600000 起。
--   实测 110000~110999 与 9600000~9609999 均空闲。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (110000, 'CRM合同审批', 'crm:contract:approve', '/api/crm/contract/{id}/approve', 'POST', 1200),
 (110001, 'CRM合同新增', 'crm:contract:create', '/api/crm/contract', 'POST', 1201),
 (110002, 'CRM合同删除', 'crm:contract:delete', '/api/crm/contract/batch', 'DELETE', 1202),
 (110003, 'CRM合同下载', 'crm:contract:download', '/api/crm/contract/export', 'GET', 1203),
 (110004, 'CRM合同编辑', 'crm:contract:edit', '/api/crm/contract/{id}', 'PUT', 1204),
 (110005, 'CRM合同刷新', 'crm:contract:refresh', '/api/crm/contract/mark-expired', 'POST', 1205),
 (110006, 'CRM合同续签申请', 'crm:contract:renewapply', '/api/crm/contract/{id}/renew', 'POST', 1206),
 (110007, 'CRM合同签署', 'crm:contract:sign', '/api/crm/contract/{id}/sign', 'POST', 1207),
 (110008, 'CRM合同查看', 'crm:contract:view', '/api/crm/contract/page', 'GET', 1208),
 (110009, 'CRM公海池查询', 'crm:customer-pool:list', '/api/crm/customer-pool/page', 'GET', 1209),
 (110010, 'CRM公海池编辑', 'crm:customer-pool:update', '/api/crm/customer-pool/put/{customerId}', 'POST', 1210),
 (110011, 'CRM公海池查看', 'crm:customer-pool:view', '/api/crm/customer-pool/available', 'GET', 1211),
 (110012, 'CRM客户新增', 'crm:customer:create', '/api/customer', 'POST', 1212),
 (110013, 'CRM客户删除', 'crm:customer:delete', '/api/customer/{id}', 'DELETE', 1213),
 (110014, 'CRM客户跟进', 'crm:customer:follow', '/api/customer/{customerId:\\d+}/follow', 'POST', 1214),
 (110015, 'CRM客户导入', 'crm:customer:import', '/api/customer/import', 'POST', 1215),
 (110016, 'CRM客户查询', 'crm:customer:list', '/api/customer/page', 'GET', 1216),
 (110017, 'CRM客户编辑', 'crm:customer:update', '/api/customer/{id}', 'PUT', 1217),
 (110018, 'CRM客户查看', 'crm:customer:view', '/api/customer/{id}', 'GET', 1218),
 (110019, 'CRM跟进新增', 'crm:follow-up:create', '/api/crm/followUp', 'POST', 1219),
 (110020, 'CRM跟进删除', 'crm:follow-up:delete', '/api/crm/followUp/{id}', 'DELETE', 1220),
 (110021, 'CRM跟进详情', 'crm:follow-up:detail', '/api/crm/followUp/{id}', 'GET', 1221),
 (110022, 'CRM跟进查询', 'crm:follow-up:list', '/api/crm/followUp/page', 'GET', 1222),
 (110023, 'CRM跟进编辑', 'crm:follow-up:update', '/api/crm/followUp/{id}', 'PUT', 1223),
 (110024, 'CRM线索批量转换', 'crm:lead:batchconvert', '/api/crm/lead/batch-convert', 'POST', 1224),
 (110025, 'CRM线索转换', 'crm:lead:convert', '/api/crm/lead/{id}/convert', 'POST', 1225),
 (110026, 'CRM线索新增', 'crm:lead:create', '/api/crm/lead', 'POST', 1226),
 (110027, 'CRM线索删除', 'crm:lead:delete', '/api/crm/lead/{id}', 'DELETE', 1227),
 (110028, 'CRM线索编辑', 'crm:lead:edit', '/api/crm/lead/{id}', 'PUT', 1228),
 (110029, 'CRM线索导出', 'crm:lead:export', '/api/crm/lead/export', 'GET', 1229),
 (110030, 'CRM线索查看', 'crm:lead:view', '/api/crm/lead/page', 'GET', 1230),
 (110031, 'CRM营销活动审批', 'crm:marketing:approve', '/api/crm/marketing/{id}/approve', 'POST', 1231),
 (110032, 'CRM营销活动新增', 'crm:marketing:create', '/api/crm/marketing', 'POST', 1232),
 (110033, 'CRM营销活动删除', 'crm:marketing:delete', '/api/crm/marketing/{id}/targets/{targetId}', 'DELETE', 1233),
 (110034, 'CRM营销活动详情', 'crm:marketing:detail', '/api/crm/marketing/{id}', 'GET', 1234),
 (110035, 'CRM营销活动查询', 'crm:marketing:list', '/api/crm/marketing/page', 'GET', 1235),
 (110036, 'CRM营销活动提交', 'crm:marketing:submit', '/api/crm/marketing/{id}/submit', 'POST', 1236),
 (110037, 'CRM营销活动编辑', 'crm:marketing:update', '/api/crm/marketing/{id}', 'PUT', 1237),
 (110038, 'CRM营销活动查看', 'crm:marketing:view', '/api/crm/marketing/running', 'GET', 1238),
 (110039, 'CRM商机新增', 'crm:opportunity:create', '/api/crm/opportunity', 'POST', 1239),
 (110040, 'CRM商机删除', 'crm:opportunity:delete', '/api/crm/opportunity/{id}', 'DELETE', 1240),
 (110041, 'CRM商机编辑', 'crm:opportunity:edit', '/api/crm/opportunity/{id}', 'PUT', 1241),
 (110042, 'CRM商机导出', 'crm:opportunity:export', '/api/crm/opportunity/export', 'GET', 1242),
 (110043, 'CRM商机查看', 'crm:opportunity:view', '/api/crm/opportunity/page', 'GET', 1243),
 (110044, 'CRM报价模板新增', 'crm:quotation-template:create', '/api/crm/quotation-template', 'POST', 1244),
 (110045, 'CRM报价模板删除', 'crm:quotation-template:delete', '/api/crm/quotation-template/{id}', 'DELETE', 1245),
 (110046, 'CRM报价模板详情', 'crm:quotation-template:detail', '/api/crm/quotation-template/{id}', 'GET', 1246),
 (110047, 'CRM报价模板查询', 'crm:quotation-template:list', '/api/crm/quotation-template/page', 'GET', 1247),
 (110048, 'CRM报价模板编辑', 'crm:quotation-template:update', '/api/crm/quotation-template/{id}', 'PUT', 1248),
 (110049, 'CRM报价模板查看', 'crm:quotation-template:view', '/api/crm/quotation-template/active', 'GET', 1249),
 (110050, 'CRM报价单审批', 'crm:quotation:approve', '/api/crm/quotation/{id}/approve', 'POST', 1250),
 (110051, 'CRM报价单转换', 'crm:quotation:convert', '/api/crm/quotation/{id}/convert', 'POST', 1251),
 (110052, 'CRM报价单新增', 'crm:quotation:create', '/api/crm/quotation', 'POST', 1252),
 (110053, 'CRM报价单删除', 'crm:quotation:delete', '/api/crm/quotation/{id}', 'DELETE', 1253),
 (110054, 'CRM报价单下载PDF', 'crm:quotation:downloadpdf', '/api/crm/quotation/export', 'GET', 1254),
 (110055, 'CRM报价单编辑', 'crm:quotation:edit', '/api/crm/quotation/{id}', 'PUT', 1255),
 (110056, 'CRM报价单发送', 'crm:quotation:send', '/api/crm/quotation/{id}/send', 'POST', 1256),
 (110057, 'CRM报价单提交', 'crm:quotation:submit', '/api/crm/quotation/{id}/submit', 'POST', 1257),
 (110058, 'CRM报价单查看', 'crm:quotation:view', '/api/crm/quotation/page', 'GET', 1258),
 (110059, 'CRM拜访取消', 'crm:visit:cancel', '/api/crm/visit/plan/{id}/cancel', 'PUT', 1259),
 (110060, 'CRM拜访新增', 'crm:visit:create', '/api/crm/visit/plan', 'POST', 1260),
 (110061, 'CRM拜访删除', 'crm:visit:delete', '/api/crm/visit/plan/{id}', 'DELETE', 1261),
 (110062, 'CRM拜访查询', 'crm:visit:list', '/api/crm/visit/plan/page', 'GET', 1262),
 (110063, 'CRM拜访编辑', 'crm:visit:update', '/api/crm/visit/plan/{id}', 'PUT', 1263),
 (110064, 'CRM拜访查看', 'crm:visit:view', '/api/crm/visit/stats/summary', 'GET', 1264)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9600000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 110000 AND 110065
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
