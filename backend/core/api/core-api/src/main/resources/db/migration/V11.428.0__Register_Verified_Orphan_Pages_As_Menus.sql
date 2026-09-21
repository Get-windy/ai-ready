-- =============================================================================
-- 孤儿页面转正 · 补挂 28 个「有页面、无菜单」的功能入口
-- V11.428.0 · 2026-09-19
--
-- 【背景】收尾盘点（CLEANUP_SCOPE_20260919.md）交叉比对 views 磁盘文件 / componentMap /
--   sys_menu，发现 87 个「前端有页面、菜单表无记录」的真孤儿。经逐页裁决后分四类处理；
--   本迁移负责其中**第 2 类：功能真实且唯一、只是菜单漏登记**的页面。
--   裁决依据与业界对照见 CLEANUP_DECISIONS_20260919.md §1。
--
-- 【挂载前逐项验证】（2026-09-19 实测，全部通过）
--   ① 28 个目标 .vue 文件全部存在于磁盘；
--   ② 28 个 component 值经 `getComponent` 三级兜底链均能在 componentMap 中解析；
--   ③ 拟用的 28 个 id（80140-80151、81000-81015）与 28 个 menu_code 均无占用；
--   ④ 每个父菜单 id 均存在且为 tenant-admin 域的目录节点。
--
-- 【两个空目录因此被填上】
--   · 60607「资产管理」此前 0 个子菜单 —— 固定资产业务完全没有入口（本项目已知菜单洞）；
--   · 61205「打印管理」此前仅 1 个子菜单（打印设置）—— 打印模板/链路/客户端/任务四处无入口。
--
-- 【字段口径】逐列对照同域已挂菜单实测值（devdb，2026-09-19）：
--   tenant_id=0 / client_type='tenant-admin' / menu_type=1（叶子）/ visible=1
--   ⚠️ status=1 —— `sys_menu.status` 是 **1=启用**（与 sys_role/sys_permission 的 0=启用相反，
--      实体注释「0-正常 1-禁用」是错的）。写成 0 会让菜单不显示。
--   display_mode=0（都是单入口页面，无"列表+表单"双入口）
--   menu_level=0 —— 实测本项目所有父目录（60006/60601/61101/61205…）的 menu_level 都是 0，
--      故子项同样取 0，不要按"层级深度"去算。
--   is_external=0 / is_cache=1 / deleted=0
--
-- 【幂等】ON CONFLICT (id) DO NOTHING
-- 【回滚】DELETE FROM sys_menu WHERE id BETWEEN 80140 AND 80151 OR id BETWEEN 81000 AND 81015;
-- =============================================================================

-- ─────────────────────── 财务域（60006）───────────────────────
-- 财务总览：60006 下的 9 个子节点都是目录，这个叶子用 sort=0 排在最前，作为财务首页
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80140, 0, 60006, 0, now(), now(),
        '财务总览', 'finance:index', 1, 'finance/index',
        'views/finance/index.vue', '', '',
        'DollarOutlined', 0, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 应收账款管理：手工立账 / 账龄分析 / 核销 / 坏账标记（后端 ReceivableController 全套端点已就绪）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80141, 0, 60601, 0, now(), now(),
        '应收账款管理', 'finance:receivable', 1, 'finance/receivable',
        'views/finance/receivable/index.vue', '', '',
        '', 7, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 应付账款管理：与应收对称（含核销；后端 PayableController 无 bad-debt 端点，故页面亦无坏账动作）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80142, 0, 60602, 0, now(), now(),
        '应付账款管理', 'finance:payable', 1, 'finance/payable',
        'views/finance/payable/index.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 收付款核销：只读台账（WriteOffController 仅 4 个 GET）；核销动作本身在应收/应付页内
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80143, 0, 60604, 0, now(), now(),
        '收付款核销', 'finance:write-off', 1, 'finance/write-off',
        'views/finance/write-off/index.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '核销记录查询（发起核销请到应收/应付账款管理）', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 往来对冲：OffsetController 真实存在，无已挂等价页
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80144, 0, 60603, 0, now(), now(),
        '往来对冲', 'finance:offset', 1, 'finance/offset',
        'views/finance/offset/index.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 定金押金：双向（DepositConditionController 真实存在）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80145, 0, 60603, 0, now(), now(),
        '定金押金', 'finance:deposit', 1, 'finance/deposit',
        'views/finance/deposit/index.vue', '', '',
        '', 5, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 辅助核算（类型/项目配置）：FinanceAuxiliaryController 真实存在，无其它入口
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80146, 0, 61204, 0, now(), now(),
        '辅助核算', 'finance:auxiliary', 1, 'finance/auxiliary',
        'views/finance/auxiliary/index.vue', '', '',
        '', 2, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────── 资产管理（60607，此前 0 子菜单）───────────────────────
-- 容器页：内部以 Tab 承载资产/分类/折旧/购置/调拨/处置/盘点/报表 8 个子视图，
-- 因此**只挂这一个菜单**，8 个 Tab 不单独挂（见 CLEANUP_DECISIONS §2.4 第 3 类）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80147, 0, 60607, 0, now(), now(),
        '资产总览', 'finance:fixed-asset', 1, 'fixed-asset/index',
        'views/fixed-asset/index.vue', '', '',
        'GoldOutlined', 1, 0, 1, 1, 1, 'tenant-admin',
        '资产/分类/折旧/购置/调拨/处置/盘点/报表', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────── 预算管理（60608）───────────────────────
-- 预算总览：仪表盘，卡片可跳转至模板/调整/报表
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80148, 0, 60608, 0, now(), now(),
        '预算总览', 'finance:budget', 1, 'budget/index',
        'views/budget/index.vue', '', '',
        '', 0, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 预算模板：BudgetTemplateController 真实存在
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80149, 0, 60608, 0, now(), now(),
        '预算模板', 'finance:budget-template', 1, 'budget/template/index',
        'views/budget/template/index.vue', '', '',
        '', 3, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 预算调整：BudgetAdjustmentController 真实存在
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80150, 0, 60608, 0, now(), now(),
        '预算调整', 'finance:budget-adjustment', 1, 'budget/adjustment/index',
        'views/budget/adjustment/index.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 预算报表：BudgetReportController 真实存在
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80151, 0, 60608, 0, now(), now(),
        '预算报表', 'finance:budget-report', 1, 'budget/report/index',
        'views/budget/report/index.vue', '', '',
        '', 5, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- 说明：原 `budget/annual/index.vue`（年度预算列表，含 create/update/delete）
--   已随本轮清理删除，其能力由已挂菜单 80130「预算编制」承接 ——
--   后者用 `annualBudgetApi.save`（无 id 新建 / 有 id 更新，即 upsert）覆盖 create+update，
--   用 `batchDelete` 覆盖 delete（批量是单条的超集）。两者同打 `/erp/budget/annual`。

-- ─────────────────────── 打印管理（61205，此前仅 1 子菜单）───────────────────────
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81000, 0, 61205, 0, now(), now(),
        '打印模板', 'set:print-template', 1, 'printing/template',
        'views/printing/template/index.vue', '', '',
        'FileTextOutlined', 2, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81001, 0, 61205, 0, now(), now(),
        '打印链路', 'set:print-chain', 1, 'printing/chain',
        'views/printing/chain/index.vue', '', '',
        'LinkOutlined', 3, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81002, 0, 61205, 0, now(), now(),
        '打印客户端', 'set:print-client', 1, 'printing/client',
        'views/printing/client/index.vue', '', '',
        'LaptopOutlined', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81003, 0, 61205, 0, now(), now(),
        '打印任务', 'set:print-task', 1, 'printing/task',
        'views/printing/task/index.vue', '', '',
        'AuditOutlined', 5, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────── 会员中心 / 质量 / 采购 / 商品 / 分析 ───────────────────────
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81004, 0, 60801, 0, now(), now(),
        '积分流水', 'member:points-history', 1, 'member/points-history',
        'views/member/points-history/index.vue', '', '',
        '', 6, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81005, 0, 60311, 0, now(), now(),
        '质量证书', 'quality:certificate', 1, 'quality/certificate/list',
        'views/quality/certificate/list.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81006, 0, 60203, 0, now(), now(),
        '供应商询价', 'purchase:supplier-inquiry', 1, 'supplier/inquiry/index',
        'views/supplier/inquiry/index.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81007, 0, 60203, 0, now(), now(),
        '供应商绩效评估', 'purchase:supplier-performance', 1, 'supplier/performance/index',
        'views/supplier/performance/index.vue', '', '',
        '', 5, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81008, 0, 61101, 0, now(), now(),
        '价格等级', 'md:product-grade', 1, 'erp/product/grade',
        'views/erp/product/grade.vue', '', '',
        '', 7, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81009, 0, 61101, 0, now(), now(),
        '库存管理模式', 'md:inventory-mode', 1, 'erp/product/inventory-mode',
        'views/erp/product/inventory-mode.vue', '', '',
        '', 8, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81010, 0, 60202, 0, now(), now(),
        '采购合同', 'purchase:contract', 1, 'erp/purchase-contract/index',
        'views/erp/purchase-contract/index.vue', '', '',
        '', 6, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81011, 0, 61002, 0, now(), now(),
        '销售报表', 'analytics:sales-report', 1, 'erp/sales-report/index',
        'views/erp/sales-report/index.vue', '', '',
        '', 14, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────── 仓储 / 工作台 ───────────────────────
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81012, 0, 60312, 0, now(), now(),
        '波次管理', 'wms:wave', 1, 'wms/wave/index',
        'views/wms/wave/index.vue', '', '',
        '', 2, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81013, 0, 60312, 0, now(), now(),
        '事件监控', 'wms:event', 1, 'wms/event/index',
        'views/wms/event/index.vue', '', '',
        'AlertOutlined', 3, 0, 1, 1, 1, 'tenant-admin',
        '事件 outbox 的待发/重试/处理状态', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81014, 0, 61103, 0, now(), now(),
        '批次管理', 'md:batch', 1, 'erp/batch/index',
        'views/erp/batch/index.vue', '', '',
        '', 3, 0, 1, 1, 1, 'tenant-admin',
        '批次 / 序列号', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (81015, 0, 50010, 0, now(), now(),
        'ERP 仪表盘', 'dashboard:erp', 1, 'erp/dashboard/index',
        'views/erp/dashboard/index.vue', '', '',
        'DashboardOutlined', 1, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 0)
ON CONFLICT (id) DO NOTHING;
